package com.xxcactussell.data.impl

import com.xxcactussell.customdomain.ForwardFullInfo
import com.xxcactussell.customdomain.ForwardInfoLink
import com.xxcactussell.data.TdClientManager
import com.xxcactussell.data.utils.ChatHistoryProcessor
import com.xxcactussell.data.utils.mappers.bots.toDomain
import com.xxcactussell.data.utils.mappers.chat.toDomain
import com.xxcactussell.data.utils.mappers.input.toData
import com.xxcactussell.data.utils.mappers.message.toData
import com.xxcactussell.data.utils.mappers.message.toDomain
import com.xxcactussell.data.utils.mappers.reaction.toData
import com.xxcactussell.data.utils.mappers.user.toDomain
import com.xxcactussell.domain.Chat
import com.xxcactussell.domain.ChatAction
import com.xxcactussell.domain.InputMessageContent
import com.xxcactussell.domain.Message
import com.xxcactussell.domain.MessageOriginChannel
import com.xxcactussell.domain.MessageOriginChat
import com.xxcactussell.domain.MessageOriginHiddenUser
import com.xxcactussell.domain.MessageOriginUser
import com.xxcactussell.domain.MessageReplyToMessage
import com.xxcactussell.domain.MessageSender
import com.xxcactussell.domain.MessageSenderChat
import com.xxcactussell.domain.MessageSenderUser
import com.xxcactussell.domain.MessageSource
import com.xxcactussell.domain.ReactionType
import com.xxcactussell.domain.User
import com.xxcactussell.repositories.messages.model.MessageListItem
import com.xxcactussell.repositories.messages.repository.MessagesRepository
import com.xxcactussell.repositories.messages.utils.getId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class MessagesRepositoryImpl @Inject constructor(
    private val clientManager: TdClientManager
) : MessagesRepository {
    private val chatProcessors = ConcurrentHashMap<Long, ChatHistoryProcessor>()
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _succeededMessages = MutableSharedFlow<Pair<Long, Message>>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val _lastReadOutboxIdFlow = MutableSharedFlow<Pair<Long, Long>>(
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        clientManager.newMessagesFlow
            .buffer(Channel.UNLIMITED)
            .onEach { updateNewMessage ->
                withContext(Dispatchers.Default) {
                    when (updateNewMessage) {
                        is TdApi.UpdateMessageContent -> {
                            val domainContent = updateNewMessage.newContent.toDomain()
                            getProcessor(updateNewMessage.chatId).processUpdateContent(
                                updateNewMessage.messageId,
                                domainContent
                            )
                        }
                        is TdApi.UpdateMessageEdited -> {
                            getProcessor(updateNewMessage.chatId).processEditMessage(
                                updateNewMessage.messageId,
                                updateNewMessage.editDate,
                                updateNewMessage.replyMarkup?.toDomain()
                            )
                        }
                        is TdApi.UpdateNewMessage -> {
                            val domainMsg = updateNewMessage.message.toDomain()
                            getProcessor(domainMsg.chatId).processNewMessage(domainMsg)
                        }
                        is TdApi.UpdateMessageSendSucceeded -> {
                            val domainMsg = updateNewMessage.message.toDomain()
                            getProcessor(domainMsg.chatId).processUpdatedMessage(
                                updateNewMessage.oldMessageId,
                                domainMsg
                            )
                        }
                        is TdApi.UpdateMessageSendFailed -> {
                            val domainMsg = updateNewMessage.message.toDomain()
                            getProcessor(domainMsg.chatId).processUpdatedMessage(
                                updateNewMessage.oldMessageId,
                                domainMsg
                            )
                        }
                        is TdApi.UpdateMessageInteractionInfo -> {
                            getProcessor(updateNewMessage.chatId).processUpdateInteractionInfo(
                                updateNewMessage.messageId,
                                updateNewMessage.interactionInfo?.toDomain()
                            )
                        }
                        is TdApi.UpdateDeleteMessages -> {
                            getProcessor(updateNewMessage.chatId).processDeleteMessages(updateNewMessage.messageIds)
                        }
                        is TdApi.UpdateChatAction -> {
                            getProcessor(updateNewMessage.chatId).processChatAction(updateNewMessage.action.toDomain())
                        }
                    }
                }
            }.launchIn(repositoryScope)

        clientManager.chatsUpdatesFlow
            .buffer(Channel.UNLIMITED)
            .onEach { update ->
                if (update is TdApi.UpdateChatReadOutbox) {
                    _lastReadOutboxIdFlow.tryEmit(update.chatId to update.lastReadOutboxMessageId)
                }
            }
            .launchIn(repositoryScope)
    }

    fun getProcessor(chatId: Long): ChatHistoryProcessor {
        return chatProcessors.computeIfAbsent(chatId) {
            ChatHistoryProcessor(chatId, clientManager, this, repositoryScope)
        }
    }

    override fun getChatFlow(chatId: Long): Flow<List<MessageListItem>> {
        return getProcessor(chatId).items
    }

    override fun getChatActionFlow(chatId: Long): Flow<ChatAction> {
        return getProcessor(chatId).action
    }

    override fun cancelSendMessageAndUploadFile(messageId: Long, chatId: Long) {
        clientManager.send(TdApi.DeleteMessages(chatId, longArrayOf(messageId), true))
    }

    override fun loadMoreHistory(chatId: Long, messageId: Long?) {
        if (messageId == null) {
            getProcessor(chatId).loadMoreHistory()
        } else {
            getProcessor(chatId).loadHistoryAround(messageId)
        }
    }

    override fun loadMoreNewer(chatId: Long) {
        getProcessor(chatId).loadMoreNewer()
    }

    override fun clearChatSession(chatId: Long) {
        chatProcessors.remove(chatId)?.clear()
    }

    override fun addReactionToMessage(
        chatId: Long,
        messageId: Long,
        reactionType: ReactionType,
    ) {
        clientManager.send(
            TdApi.AddMessageReaction(
                chatId,
                messageId,
                reactionType.toData(),
                false,
                false
            ), null
        )
    }

    override fun removeReactionFromMessage(
        chatId: Long,
        messageId: Long,
        reactionType: ReactionType,
    ) {
        clientManager.send(
            TdApi.RemoveMessageReaction(
                chatId,
                messageId,
                reactionType.toData()
            ), null
        )
    }

    suspend fun resolveSenders(senderIds: Set<MessageSender>): Map<Long, Any> {
        return withContext(Dispatchers.Default) {
            val deferredSenders = senderIds.associateWith { senderId ->
                async {
                    when (senderId) {
                        is MessageSenderChat -> getChat(senderId.chatId)
                        is MessageSenderUser -> getUser(senderId.userId)
                    }
                }
            }

            val resultMap = mutableMapOf<Long, Any>()

            deferredSenders.forEach { (senderId, deferredChat) ->
                val chat = deferredChat.await()
                if (chat != null) {
                    resultMap[senderId.getId() ?: 0L] = chat
                }
            }
            resultMap
        }
    }

    suspend fun getMessagesWithAlbumBoundary(
        chatId: Long,
        fromMessageId: Long,
        limit: Int
    ): List<Message> {
        var messages = getChatHistory(chatId, fromMessageId, 0, limit)
        if (messages.isNotEmpty()) {
            val oldestMessage = messages.last()
            if (oldestMessage.mediaAlbumId != 0L) {
                val lastAlbumIndex =
                    messages.indexOfFirst { it.mediaAlbumId == oldestMessage.mediaAlbumId }
                if (lastAlbumIndex > 0) {
                    messages = messages.subList(0, lastAlbumIndex)
                }
            }
        }
        return messages
    }

    override suspend fun getChatHistory(
        chatId: Long,
        fromMessageId: Long,
        offset: Int,
        limit: Int
    ): List<Message> {
        return withContext(Dispatchers.Default) {
            val initialMessages = suspendCancellableCoroutine { continuation ->
                clientManager.send(
                    TdApi.GetChatHistory(
                        chatId,
                        fromMessageId,
                        offset,
                        limit,
                        false
                    )
                ) { result ->
                    if (continuation.isActive) {
                        when (result) {
                            is TdApi.Messages -> {
                                val messages = result.messages.map { it.toDomain() }
                                continuation.resume(messages)
                            }
                            else -> continuation.resume(emptyList())
                        }
                    }
                }
            }

            initialMessages.map { message ->
                async {
                    var newMessage =
                        if (message.replyTo != null && message.replyTo is MessageReplyToMessage) {
                            val replyChatId = (message.replyTo as MessageReplyToMessage).chatId
                            val replyMessageId = (message.replyTo as MessageReplyToMessage).messageId
                            val loadedReply = getReplyMessage(replyChatId, replyMessageId)
                            message.copy(
                                replyTo = (message.replyTo as MessageReplyToMessage).copy(
                                    message = loadedReply
                                )
                            )
                        } else {
                            message
                        }

                    newMessage = if (message.forwardInfo != null) {
                        when (val origin = message.forwardInfo!!.origin) {
                            is MessageOriginUser -> {
                                val originChat = getUser(origin.senderUserId)
                                newMessage.copy(
                                    forwardFullInfo = ForwardFullInfo(
                                        isHidden = false,
                                        chat = originChat?.firstName + " " + originChat?.lastName,
                                        link = ForwardInfoLink(
                                            chatId = origin.senderUserId
                                        ),
                                        date = message.forwardInfo!!.date
                                    )
                                )
                            }
                            is MessageOriginChat -> {
                                val originChat = getChat(origin.senderChatId)
                                newMessage.copy(
                                    forwardFullInfo = ForwardFullInfo(
                                        isHidden = false,
                                        chat = originChat?.title,
                                        signature = origin.authorSignature,
                                        link = ForwardInfoLink(
                                            chatId = origin.senderChatId
                                        ),
                                        date = message.forwardInfo!!.date
                                    )
                                )
                            }
                            is MessageOriginChannel -> {
                                val originChat = getChat(origin.chatId)
                                newMessage.copy(
                                    forwardFullInfo = ForwardFullInfo(
                                        isHidden = false,
                                        chat = originChat?.title,
                                        signature = origin.authorSignature,
                                        link = ForwardInfoLink(
                                            chatId = origin.chatId,
                                            messageId = origin.messageId
                                        ),
                                        date = message.forwardInfo!!.date
                                    )
                                )
                            }
                            is MessageOriginHiddenUser -> newMessage.copy(
                                forwardFullInfo = ForwardFullInfo(
                                    isHidden = true,
                                    date = message.forwardInfo!!.date
                                )
                            )
                        }
                    } else {
                        newMessage
                    }

                    newMessage
                }
            }.awaitAll()
        }
    }

    override fun observeSucceededMessages(): Flow<Pair<Long, Message>> =
        _succeededMessages.asSharedFlow()

    override fun observeLastReadOutboxMessage(): Flow<Pair<Long, Long>> =
        _lastReadOutboxIdFlow.asSharedFlow()

    override suspend fun openChat(chatId: Long): Chat? = suspendCancellableCoroutine { continuation ->
        clientManager.send(TdApi.GetChat(chatId)) { result ->
            if (!continuation.isActive) return@send

            when (result.constructor) {
                TdApi.Chat.CONSTRUCTOR -> {
                    val tdChat = result as TdApi.Chat
                    CoroutineScope(Dispatchers.Default).launch {
                        var mgChat = tdChat.toDomain()
                        val type = tdChat.type
                        when (type) {
                            is TdApi.ChatTypeBasicGroup -> {
                                clientManager.send(TdApi.GetBasicGroup(type.basicGroupId)) { groupResult ->
                                    if (groupResult.constructor == TdApi.BasicGroup.CONSTRUCTOR) {
                                        val bg = groupResult as TdApi.BasicGroup
                                        mgChat = mgChat.copy(
                                            myMemberStatus = bg.status.toDomain(),
                                            memberCount = bg.memberCount
                                        )
                                    }
                                    clientManager.send(TdApi.OpenChat(chatId), null)
                                    if (continuation.isActive) continuation.resume(mgChat)
                                }
                            }
                            is TdApi.ChatTypeSupergroup -> {
                                clientManager.send(TdApi.GetSupergroup(type.supergroupId)) { superResult ->
                                    if (superResult.constructor == TdApi.Supergroup.CONSTRUCTOR) {
                                        val sg = superResult as TdApi.Supergroup
                                        mgChat = mgChat.copy(
                                            myMemberStatus = sg.status.toDomain(),
                                            memberCount = sg.memberCount
                                        )
                                    }
                                    clientManager.send(TdApi.OpenChat(chatId), null)
                                    if (continuation.isActive) continuation.resume(mgChat)
                                }
                            }
                            else -> {
                                clientManager.send(TdApi.OpenChat(chatId), null)
                                if (continuation.isActive) continuation.resume(mgChat)
                            }
                        }
                    }
                }
                else -> {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }
    }

    override fun closeChat(chatId: Long) {
        clientManager.send(TdApi.CloseChat(chatId), null)
    }

    override suspend fun getChat(chatId: Long): Chat? = suspendCancellableCoroutine { continuation ->
        clientManager.send(TdApi.GetChat(chatId)) { result ->
            if (continuation.isActive) {
                when (result.constructor) {
                    TdApi.Chat.CONSTRUCTOR -> {
                        CoroutineScope(Dispatchers.Default).launch {
                            val tdChat = result as TdApi.Chat
                            continuation.resume(tdChat.toDomain())
                        }
                    }
                    else -> continuation.resume(null)
                }
            }
        }
    }

    override suspend fun getUser(userId: Long): User? = suspendCancellableCoroutine { continuation ->
        clientManager.send(TdApi.GetUser(userId)) { result ->
            if (continuation.isActive) {
                when (result.constructor) {
                    TdApi.User.CONSTRUCTOR -> {
                        CoroutineScope(Dispatchers.Default).launch {
                            val tdUser = result as TdApi.User
                            continuation.resume(tdUser.toDomain())
                        }
                    }
                    else -> continuation.resume(null)
                }
            }
        }
    }

    override suspend fun sendMessage(
        chatId: Long,
        inputMessageContents: List<InputMessageContent>,
        replyToMessageId: Long,
        messageThreadId: Long
    ): List<Message> = suspendCancellableCoroutine { continuation ->
        if (inputMessageContents.isEmpty()) {
            continuation.resumeWithException(IllegalArgumentException("Input message contents cannot be empty"))
            return@suspendCancellableCoroutine
        }

        val replyTo = if (replyToMessageId != 0L) TdApi.InputMessageReplyToMessage(
            replyToMessageId,
            null,
            0
        ) else null

        if (inputMessageContents.size == 1) {
            clientManager.send(
                TdApi.SendMessage(
                    chatId,
                    messageThreadId,
                    replyTo,
                    null,
                    null,
                    inputMessageContents.first().toData()
                )
            ) { result ->
                if (continuation.isActive) {
                    when (result) {
                        is TdApi.Message -> {
                            CoroutineScope(Dispatchers.Default).launch {
                                val message = result.toDomain()
                                continuation.resume(listOf(message))
                            }
                        }
                        is TdApi.Error -> continuation.resumeWithException(Exception("TDLib error: ${result.code} ${result.message}"))
                        else -> continuation.resumeWithException(Exception("Unexpected result from sendMessage: $result"))
                    }
                }
            }
        } else {
            clientManager.send(
                TdApi.SendMessageAlbum(
                    chatId,
                    messageThreadId,
                    replyTo,
                    null,
                    inputMessageContents.map { it.toData() }.toTypedArray()
                )
            ) { result ->
                if (continuation.isActive) {
                    when (result) {
                        is TdApi.Messages -> {
                            if (result.messages.isNotEmpty()) {
                                CoroutineScope(Dispatchers.Default).launch {
                                    val messages = result.messages.map { it.toDomain() }
                                    continuation.resume(messages)
                                }
                            } else {
                                continuation.resumeWithException(Exception("TDLib returned empty Messages object for album"))
                            }
                        }
                        is TdApi.Error -> continuation.resumeWithException(Exception("TDLib error: ${result.code} ${result.message}"))
                        else -> continuation.resumeWithException(Exception("Unexpected result from sendMessageAlbum: $result"))
                    }
                }
            }
        }
    }

    override fun markMessageAsRead(
        chatId: Long,
        messageId: Long,
        source: MessageSource,
        forceRead: Boolean
    ) {
        clientManager.send(
            TdApi.ViewMessages(
                chatId,
                longArrayOf(messageId),
                source.toData(),
                forceRead
            ), null
        )
    }

    override suspend fun searchMediaMessages(
        chatId: Long,
        fromMessageId: Long,
        offset: Int,
        limit: Int,
    ): List<Message> = suspendCancellableCoroutine { continuation ->
        clientManager.send(
            TdApi.SearchChatMessages(
                chatId,
                null,
                "",
                null,
                fromMessageId,
                offset,
                limit,
                TdApi.SearchMessagesFilterPhotoAndVideo()
            )
        ) { result ->
            if (continuation.isActive) {
                when (result) {
                    is TdApi.FoundChatMessages -> {
                        CoroutineScope(Dispatchers.Default).launch {
                            val messages = result.messages.map { it.toDomain() }
                            continuation.resume(messages)
                        }
                    }
                    is TdApi.Error -> continuation.resumeWithException(Exception("TDLib error: ${result.code} ${result.message}"))
                    else -> continuation.resumeWithException(Exception("Unexpected result from searchMediaMessages: $result"))
                }
            }
        }
    }

    override suspend fun getReplyMessage(chatId: Long, messageId: Long): Message? =
        suspendCancellableCoroutine { continuation ->
            clientManager.send(TdApi.GetMessage(chatId, messageId)) { result ->
                if (continuation.isActive) {
                    when (result.constructor) {
                        TdApi.Message.CONSTRUCTOR -> {
                            CoroutineScope(Dispatchers.Default).launch {
                                val tdMsg = result as TdApi.Message
                                continuation.resume(tdMsg.toDomain())
                            }
                        }
                        else -> continuation.resume(null)
                    }
                }
            }
        }
}