package com.xxcactussell.data.impl

import android.content.Context
import android.util.Log
import com.xxcactussell.data.TdClientManager
import com.xxcactussell.data.TelegramCredentials
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class TdClientManagerImpl @Inject constructor() : TdClientManager {
    private val isInitialized = AtomicBoolean(false)

    @Volatile
    private var _client: Client? = null
    override val client: Client
        get() = _client ?: throw IllegalStateException("Client is not initialized yet. Wait for AuthorizationState.")

    private val _authUpdatesFlow = MutableStateFlow<TdApi.AuthorizationState>(TdApi.AuthorizationStateWaitTdlibParameters())
    override val authUpdatesFlow: StateFlow<TdApi.AuthorizationState> = _authUpdatesFlow.asStateFlow()

    private val _newMessagesFlow = MutableSharedFlow<TdApi.Update>(replay = 25, extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val newMessagesFlow: SharedFlow<TdApi.Update> = _newMessagesFlow

    private val _chatsUpdatesFlow = MutableSharedFlow<TdApi.Update>(replay = 25, extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val chatsUpdatesFlow: SharedFlow<TdApi.Update> = _chatsUpdatesFlow.asSharedFlow()

    private val _chatFoldersUpdatesFlow = MutableSharedFlow<TdApi.Update>(replay = 25, extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val chatFoldersUpdatesFlow: SharedFlow<TdApi.Update> = _chatFoldersUpdatesFlow.asSharedFlow()

    private val _filesUpdatesFlow = MutableSharedFlow<TdApi.UpdateFile>(replay = 25, extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val filesUpdatesFlow: SharedFlow<TdApi.UpdateFile> = _filesUpdatesFlow.asSharedFlow()

    private val _updatesFlow = MutableSharedFlow<TdApi.Object>(replay = 25, extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val updatesFlow: SharedFlow<TdApi.Object> = _updatesFlow.asSharedFlow()

    private val tdClientManagerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun initCred(context: Context) {
        TelegramCredentials.initialize(context)
    }

    override fun initialize(context: Context) {
        if (isInitialized.getAndSet(true)) return
        tdClientManagerScope.launch {
            try {
                try {
                    System.loadLibrary("tdjni")
                } catch (e: UnsatisfiedLinkError) {
                    Log.e("TdClientManager", "Failed to load tdjni", e)
                }

                try {
                    Client.execute(TdApi.SetLogVerbosityLevel(0))
                } catch (e: Exception) { }

                _client = Client.create({ update ->
                    handleUpdate(update)
                }, null, null)

                val languagePackDbPath = File(context.filesDir, "language_pack.sqlite").absolutePath

                _client?.send(TdApi.SetOption("language_pack_database_path", TdApi.OptionValueString(languagePackDbPath))) { result ->
                    if (result is TdApi.Error) {
                        Log.e("TdLibRepo", "Error setting lang pack: ${result.message}")
                    }
                }

            } catch (e: Exception) {
                Log.e("TdClientManager", "Initialization failed", e)
            }
        }
    }

    private fun handleUpdate(update: TdApi.Object) {
        if (update is TdApi.Update) {
            when (update) {
                is TdApi.UpdateAuthorizationState -> _authUpdatesFlow.value = update.authorizationState
                is TdApi.UpdateChatFolders, is TdApi.UpdateUnreadChatCount -> _chatFoldersUpdatesFlow.tryEmit(update)
                is TdApi.UpdateNewChat, is TdApi.UpdateChatPosition, is TdApi.UpdateChatLastMessage,
                is TdApi.UpdateChatReadInbox, is TdApi.UpdateChatTitle, is TdApi.UpdateChatPhoto,
                is TdApi.UpdateUser, is TdApi.UpdateUserStatus, is TdApi.UpdateChatReadOutbox -> _chatsUpdatesFlow.tryEmit(update)
                is TdApi.UpdateFile -> _filesUpdatesFlow.tryEmit(update)
                is TdApi.UpdateNewMessage, is TdApi.UpdateMessageContent, is TdApi.UpdateMessageEdited,
                is TdApi.UpdateDeleteMessages, is TdApi.UpdateChatAction, is TdApi.UpdateMessageSendSucceeded,
                is TdApi.UpdateMessageInteractionInfo, is TdApi.UpdateMessageSendFailed -> _newMessagesFlow.tryEmit(update)
            }
        }
        _updatesFlow.tryEmit(update)
    }

    override fun send(query: TdApi.Function<*>, handler: Client.ResultHandler?) {
        val currentClient = _client
        if (currentClient != null) {
            currentClient.send(query, handler)
        } else {
            Log.w("TdClientManager", "Attempting to send query ${query.javaClass.simpleName} before Client is initialized")
        }
    }

    override suspend fun <T : TdApi.Object> sendSuspend(query: TdApi.Function<*>): T =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { cont ->
                val currentClient = _client
                if (currentClient == null) {
                    cont.resumeWithException(IllegalStateException("TDLib client is not initialized"))
                    return@suspendCancellableCoroutine
                }

                currentClient.send(query) { result ->
                    if (result.constructor == TdApi.Error.CONSTRUCTOR) {
                        val error = result as TdApi.Error
                        cont.resumeWithException(RuntimeException("TDLib Error: ${error.code} - ${error.message}"))
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        cont.resume(result as T)
                    }
                }
            }
        }
}