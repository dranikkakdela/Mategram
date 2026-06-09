package com.xxcactussell.presentation.messages.screen

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Ease
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.xxcactussell.domain.ChatActionChoosingContact
import com.xxcactussell.domain.ChatActionChoosingLocation
import com.xxcactussell.domain.ChatActionChoosingSticker
import com.xxcactussell.domain.ChatActionRecordingVideo
import com.xxcactussell.domain.ChatActionRecordingVideoNote
import com.xxcactussell.domain.ChatActionRecordingVoiceNote
import com.xxcactussell.domain.ChatActionStartPlayingGame
import com.xxcactussell.domain.ChatActionTyping
import com.xxcactussell.domain.ChatActionUploadingDocument
import com.xxcactussell.domain.ChatActionUploadingPhoto
import com.xxcactussell.domain.ChatActionUploadingVideo
import com.xxcactussell.domain.ChatActionUploadingVideoNote
import com.xxcactussell.domain.ChatActionUploadingVoiceNote
import com.xxcactussell.domain.ChatActionWatchingAnimations
import com.xxcactussell.domain.ChatTypeBasicGroup
import com.xxcactussell.domain.ChatTypeSupergroup
import com.xxcactussell.domain.UserTypeBot
import com.xxcactussell.mategram.presentation.R
import com.xxcactussell.player.PlayerState
import com.xxcactussell.presentation.chats.model.AvatarUiState
import com.xxcactussell.presentation.chats.screen.ChatAvatar
import com.xxcactussell.presentation.localization.localizedString
import com.xxcactussell.presentation.messages.model.InputEvent
import com.xxcactussell.presentation.messages.model.MessageUiItem
import com.xxcactussell.presentation.messages.model.MessagesEvent
import com.xxcactussell.presentation.messages.model.MessagesUiState
import com.xxcactussell.presentation.messages.model.getItem
import com.xxcactussell.presentation.messages.model.getMessageDate
import com.xxcactussell.presentation.messages.model.getMessageId
import com.xxcactussell.presentation.messages.model.getMessageSenderId
import com.xxcactussell.presentation.messages.model.getReplyTo
import com.xxcactussell.presentation.messages.model.isOutgoing
import com.xxcactussell.presentation.messages.model.isServiceMessage
import com.xxcactussell.presentation.tools.InputMessageField
import com.xxcactussell.presentation.tools.formatTimestampToDate
import com.xxcactussell.presentation.tools.screenStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class, ExperimentalLayoutApi::class
)
@Composable
fun MessagesContent(
    state: MessagesUiState,
    onProfileClicked: (Long) -> Unit,
    onEvent: (Any) -> Unit,
    onBackHandle: () -> Unit,
    onCameraClicked: () -> Unit,
    onLinkClicked: (Long, Long?) -> Unit,
    onMediaClicked: (Long) -> Unit,
    playerState: PlayerState
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val sizeNewMessageButton = ButtonDefaults.MediumContainerHeight
    val lazyListState = rememberLazyListState(cacheWindow = LazyLayoutCacheWindow(0.5f, 0.5f))

    var highlightedMessageId by remember { mutableStateOf<Long?>(null) }
    var pendingScrollToMessageId by remember { mutableStateOf<Long?>(null) }
    var isInitialScrollPerformed by remember { mutableStateOf(false) }

    LaunchedEffect(state.messages.firstOrNull()) {
        if (state.messages.firstOrNull() != null && isInitialScrollPerformed) {
            if (lazyListState.firstVisibleItemIndex > 1) {
                onEvent(MessagesEvent.ShowScrollToBottomButton)
            } else {
                scope.launch {
                    lazyListState.animateScrollToItem(0)
                }
            }
        }
    }

    LaunchedEffect(state.messages, state.initialScrollTargetId) {
        if (!isInitialScrollPerformed && state.messages.isNotEmpty() && state.initialScrollTargetId != null) {
            val targetId = state.initialScrollTargetId

            val index = state.messages.indexOfFirst { item ->
                val itemId = item.getMessageId()
                if (itemId == targetId) return@indexOfFirst true
                if (item is MessageUiItem.AlbumItem) {
                    return@indexOfFirst item.messages.any { it.message.id == targetId }
                }
                false
            }

            if (index != -1) {
                lazyListState.scrollToItem(index)
                isInitialScrollPerformed = true
            }
        } else if (!isInitialScrollPerformed && state.messages.isNotEmpty()) {
            isInitialScrollPerformed = true
        }
    }

    LaunchedEffect(state.messages, pendingScrollToMessageId) {
        val targetId = pendingScrollToMessageId
        if (targetId != null) {
            val index = state.messages.indexOfFirst { item ->
                val itemId = item.getMessageId()
                if (itemId == targetId) return@indexOfFirst true
                if (item is MessageUiItem.AlbumItem) {
                    return@indexOfFirst item.messages.any { it.message.id == targetId }
                }
                false
            }

            if (index != -1) {
                lazyListState.animateScrollToItem(index, -1)
                highlightedMessageId = targetId
                pendingScrollToMessageId = null
            } else {
                if (state.messages.isNotEmpty()) {
                    lazyListState.animateScrollToItem(state.messages.lastIndex)
                }
            }
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstIndex ->
                onEvent(MessagesEvent.UpdateFirstVisibleItemIndex(firstIndex))
                if (firstIndex == 0) {
                    onEvent(MessagesEvent.HideScrollToBottomButton)
                } else if (firstIndex >= 5) {
                    onEvent(MessagesEvent.ShowScrollToBottomButton)
                }
            }
    }
    Scaffold(
        modifier = Modifier.screenStyle(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            Modifier.clickable {
                                onProfileClicked(state.chat?.id ?: 0L)
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ChatAvatar(
                                modifier = Modifier.size(48.dp),
                                state = AvatarUiState(
                                    chatId = state.chat?.id ?: 0L,
                                    photo = state.chat?.photo,
                                    title = state.chat?.title ?: ""
                                ),
                                isPinned = false,
                                isOnline = false
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = state.chat?.title ?: "",
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleMediumEmphasized,
                                    maxLines = 1
                                )
                                if (state.chat != null) {
                                    val key = state.chatStatusStringKey
                                    val supportingText = if (state.chat.type is ChatTypeBasicGroup || state.chat.type is ChatTypeSupergroup) {
                                        localizedString(
                                            key ?: "",
                                            state.chat.memberCount?.toLong() ?: 0L,
                                            state.chat.memberCount ?: 0
                                        )
                                    } else if (state.user?.type is UserTypeBot) {
                                        localizedString(
                                            if ((state.user.type as UserTypeBot).activeUserCount == 0) "Bot" else "BotUsers",
                                            (state.user.type as UserTypeBot).activeUserCount,
                                            (state.user.type as UserTypeBot).activeUserCount
                                        )
                                    } else {
                                        val keyArgs : Pair<String, Any> = when (state.chatAction) {
                                            ChatActionChoosingContact -> Pair("SelectingContact", "")
                                            ChatActionChoosingLocation -> Pair("SelectingLocation", "")
                                            ChatActionChoosingSticker -> Pair("ChoosingSticker", "")
                                            ChatActionRecordingVideo -> Pair("RecordingRound", "")
                                            ChatActionRecordingVideoNote -> Pair("RecordingRound", "")
                                            ChatActionRecordingVoiceNote -> Pair("RecordingAudio", "")
                                            ChatActionStartPlayingGame -> Pair("SendingGame", "")
                                            ChatActionTyping -> Pair("Typing", "")
                                            is ChatActionUploadingDocument -> Pair("SendingFile", "")
                                            is ChatActionUploadingPhoto -> Pair("SendingPhoto", "")
                                            is ChatActionUploadingVideo -> Pair("SendingVideoStatus", "")
                                            is ChatActionUploadingVideoNote -> Pair("RecordingRound", "")
                                            is ChatActionUploadingVoiceNote -> Pair("RecordingAudio", "")
                                            is ChatActionWatchingAnimations -> Pair("EnjoyngAnimations", state.chatAction.emoji)
                                            else -> Pair(key ?: "", if (key == "LastSeenFormatted") formatTimestampToDate(state.wasOnline) else "")
                                        }
                                        localizedString(
                                            keyArgs.first,
                                            1,
                                            keyArgs.second
                                        )
                                    }
                                    Text(
                                        text = supportingText,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onBackHandle,
                        ) {
                            Icon(painterResource(R.drawable.arrow_back_24px), "Back")
                        }
                    },
                    actions = {
    IconButton(
        onClick = { /* TODO: открыть меню */ }
    ) {
        Icon(painterResource(R.drawable.more_vert_24px), "Menu")
    }
},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        scrolledContainerColor  = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            }
        },
        bottomBar = { InputMessageField(
            state = state,
            onEvent = onEvent,
            onCameraClicked = onCameraClicked,
            onReplyClicked = { replyToMessageId ->
                val index = state.messages.indexOfFirst { item ->
                    val itemId = item.getMessageId()
                    if (itemId == replyToMessageId) return@indexOfFirst true
                    if (item is MessageUiItem.AlbumItem) {
                        return@indexOfFirst item.messages.any { it.message.id == replyToMessageId }
                    }
                    false
                }

                if (index != -1) {
                    scope.launch {
                        lazyListState.animateScrollToItem(index)
                        highlightedMessageId = replyToMessageId
                    }
                } else {
                    scope.launch {
                        pendingScrollToMessageId = replyToMessageId
                        if (state.messages.isNotEmpty()) {
                            lazyListState.animateScrollToItem(state.messages.lastIndex, -2)
                        }
                    }
                }
            },
        ) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .clip(RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            LazyColumn(
                state = lazyListState,
                reverseLayout = true,
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = paddingValues.calculateBottomPadding() + 8.dp
                ),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom)
            ) {
                itemsIndexed(
                    items = state.messages,
                    key = { _, message ->
                        message.key
                    }
                ) { index, message ->
                    val nextPersonId = state.messages.getOrNull(index + 1)?.getMessageSenderId()
                    val prevPersonId = state.messages.getOrNull(index - 1)?.getMessageSenderId()

                    val currentReplyTo = message.getReplyTo()
                    val prevReplyTo = state.messages.getOrNull(index - 1)?.getReplyTo()

                    val topCorner = message.getMessageDate()?.let { currentTimestamp ->
                        val prevTimestamp = state.messages.getOrNull(index + 1)
                            ?.getMessageDate()
                            ?: 0
                        val timeDifferenceSeconds = currentTimestamp - prevTimestamp

                        val FIVE_MINUTES_SECONDS = 300

                        if (message.getMessageSenderId() == nextPersonId) {
                            if (timeDifferenceSeconds > FIVE_MINUTES_SECONDS
                                || currentReplyTo != null
                            ) {
                                18.dp
                            } else {
                                4.dp
                            }
                        } else {
                            18.dp
                        }
                    } ?: 18.dp


                    val bottomCorner = message.getMessageDate()?.let { currentTimestamp ->
                        val nextTimestamp = state.messages.getOrNull(index - 1)
                            ?.getMessageDate()
                            ?: 0
                        val timeDifferenceSeconds = nextTimestamp - currentTimestamp

                        val FIVE_MINUTES_SECONDS = 300

                        if (message.getMessageSenderId() == prevPersonId) {
                            if (timeDifferenceSeconds > FIVE_MINUTES_SECONDS
                                || (prevReplyTo != null && message.getMessageSenderId() == prevPersonId)
                            ) {
                                18.dp
                            } else {
                                4.dp
                            }
                        } else {
                            18.dp
                        }
                    } ?: 18.dp

                    val needAvatar = message.getMessageSenderId() != prevPersonId && message.isOutgoing() == false && (state.chat?.type is ChatTypeBasicGroup || state.chat?.type is ChatTypeSupergroup && !(state.chat.type as ChatTypeSupergroup).isChannel)
                    val needSenderName = message.getMessageSenderId() != nextPersonId && message.isOutgoing() == false && (state.chat?.type is ChatTypeBasicGroup || state.chat?.type is ChatTypeSupergroup && !(state.chat.type as ChatTypeSupergroup).isChannel)

                    val isHighlighted = remember(message, highlightedMessageId) {
                        val msgId = message.getMessageId()
                        if (msgId == highlightedMessageId && msgId != null) true
                        else if (message is MessageUiItem.AlbumItem) {
                            message.messages.any { it.message.id == highlightedMessageId }
                        } else false
                    }

                    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    val animatedColor = remember { Animatable(Color.Transparent) }

                    LaunchedEffect(isHighlighted) {
                        if (isHighlighted) {
                            animatedColor.animateTo(highlightColor, tween(300, easing = Ease))
                            animatedColor.animateTo(Color.Transparent, tween(300, easing = Ease))
                            animatedColor.animateTo(highlightColor, tween(300, easing = Ease))
                            animatedColor.animateTo(Color.Transparent, tween(300, easing = Ease))
                            animatedColor.animateTo(highlightColor, tween(300, easing = Ease))
                            animatedColor.animateTo(Color.Transparent, tween(1500, easing = LinearOutSlowInEasing))

                            if (highlightedMessageId == message.getMessageId() ||
                                (message is MessageUiItem.AlbumItem && message.messages.any { it.message.id == highlightedMessageId })) {
                                highlightedMessageId = null
                            }
                        } else {
                            animatedColor.snapTo(Color.Transparent)
                        }
                    }

                    val isUnread = when(message) {
                        is MessageUiItem.AlbumItem -> {
                            message.messages.any {
                                when (it.message.isOutgoing) {
                                    true -> {
                                        it.message.id > (state.chat?.lastReadOutboxMessageId ?: 0L)
                                    }
                                    false -> {
                                        it.message.id > (state.chat?.lastReadInboxMessageId ?: 0L)
                                    }
                                }
                            }
                        }
                        is MessageUiItem.MessageItem -> {
                            when (message.message.isOutgoing) {
                                true -> {
                                    message.message.id > (state.chat?.lastReadOutboxMessageId ?: 0L)
                                }
                                false -> {
                                    message.message.id > (state.chat?.lastReadInboxMessageId ?: 0L)
                                }
                            }
                        }
                        else -> false
                    }

                    val max = 0.dp
                    val min = (-140).dp
                    val thresholdDp = (-90).dp

                    val density = LocalDensity.current
                    val (minPx, maxPx) = with(density) { min.toPx() to max.toPx() }
                    val thresholdPx = with(density) { thresholdDp.toPx() }

                    val viscosityFactor = 0.3f
                    var rawDragInput by remember { mutableFloatStateOf(0f) }
                    val offsetPosition = remember { mutableFloatStateOf(0f) }

                    val offsetPositionAnimated by animateFloatAsState(
                        offsetPosition.floatValue,
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )


                    val messageItemModifier = when {
                        !(state.chat?.type is ChatTypeSupergroup && (state.chat.type as ChatTypeSupergroup).isChannel) && !message.isServiceMessage() -> Modifier
                            .draggable(
                                state = rememberDraggableState { delta ->
                                    val oldRaw = rawDragInput
                                    val newRaw = (oldRaw + delta).coerceIn(minPx, maxPx)
                                    rawDragInput = newRaw

                                    val newVisualValue = if (newRaw > thresholdPx) {
                                        newRaw * viscosityFactor
                                    } else {
                                        val visualThreshold = thresholdPx * viscosityFactor
                                        val fraction =
                                            (newRaw - thresholdPx) / (minPx - thresholdPx)
                                        visualThreshold + (minPx - visualThreshold) * fraction
                                    }

                                    offsetPosition.floatValue = newVisualValue

                                    val crossedThreshold =
                                        (oldRaw > thresholdPx && newRaw <= thresholdPx) ||
                                                (oldRaw < thresholdPx && newRaw >= thresholdPx)

                                    if (crossedThreshold) {
                                        haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                    }
                                },
                                orientation = Orientation.Horizontal,
                                onDragStopped = {
                                    if (rawDragInput < thresholdPx) {
                                        onEvent(MessagesEvent.ReplyToSelected(message.getItem()))
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    } else {
                                        haptic.performHapticFeedback(HapticFeedbackType.Reject)
                                    }
                                    offsetPosition.floatValue = 0f
                                    rawDragInput = 0f
                                }
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onEvent(MessagesEvent.MessageClicked(message.getMessageId()))
                            }
                        state.chat?.type is ChatTypeSupergroup && (state.chat.type as ChatTypeSupergroup).isChannel && !message.isServiceMessage() -> Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onEvent(MessagesEvent.MessageClicked(message.getMessageId()))
                        }
                        else -> Modifier
                    }

                    Box(
                        modifier = messageItemModifier
                            .fillMaxWidth()
                            .background(animatedColor.value)
                            .onVisibilityChanged {
                                if (index + 5 > state.messages.size) {
                                    onEvent(MessagesEvent.LoadMoreHistory)
                                }
                                if (index < 10) {
                                    onEvent(MessagesEvent.LoadMoreNewer)
                                }
                                if ((message.getMessageId()
                                        ?: 0L) > (state.chat?.lastReadInboxMessageId ?: 0L)
                                ) {
                                    when (message) {
                                        is MessageUiItem.MessageItem -> {
                                            onEvent(MessagesEvent.MessageRead(message.message.id))
                                        }

                                        is MessageUiItem.AlbumItem -> {
                                            message.messages.forEach {
                                                onEvent(MessagesEvent.MessageRead(it.message.id))
                                            }
                                        }

                                        else -> {

                                        }
                                    }
                                }
                            }
                            .padding(
                                start = paddingValues.calculateStartPadding(LocalLayoutDirection.current) + 8.dp,
                                end = paddingValues.calculateEndPadding(LocalLayoutDirection.current) + 8.dp
                            )
                    ) {
                        MessageItemContent(
                            modifier = Modifier
                                .offset {
                                    IntOffset(offsetPositionAnimated.roundToInt(), 0)
                                },
                            message = message,
                            playerState = playerState,
                            isDateShown = state.messageIdWithDateShown == message.getMessageId(),
                            topCorner = topCorner,
                            bottomCorner = bottomCorner,
                            needSenderName = needSenderName,
                            needAvatar = needAvatar,
                            isGroup = state.chat?.type is ChatTypeBasicGroup || state.chat?.type is ChatTypeSupergroup && !(state.chat.type as ChatTypeSupergroup).isChannel,
                            isUnread = isUnread,
                            onMediaClicked = onMediaClicked,
                            onLinkClicked = onLinkClicked,
                            onReplyClicked = { replyToMessageId ->
                                val index = state.messages.indexOfFirst { item ->
                                    val itemId = item.getMessageId()
                                    if (itemId == replyToMessageId) return@indexOfFirst true
                                    if (item is MessageUiItem.AlbumItem) {
                                        return@indexOfFirst item.messages.any { it.message.id == replyToMessageId }
                                    }
                                    false
                                }

                                if (index != -1) {
                                    scope.launch {
                                        lazyListState.animateScrollToItem(index)
                                        highlightedMessageId = replyToMessageId
                                    }
                                } else {
                                    scope.launch {
                                        pendingScrollToMessageId = replyToMessageId
                                        if (state.messages.isNotEmpty()) {
                                            lazyListState.animateScrollToItem(state.messages.lastIndex, -2)
                                        }
                                    }
                                }
                            },
                            onEvent = onEvent
                        ) {
                            AnimatedVisibility(
                                visible = (offsetPosition.floatValue < maxPx)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularWavyProgressIndicator(
                                        progress = {
                                            offsetPositionAnimated.coerceIn(thresholdPx * viscosityFactor, maxPx) / (thresholdPx * viscosityFactor)
                                        }
                                    )
                                    Icon(
                                        painterResource(R.drawable.reply_24px),
                                        "",
                                        Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    if (topCorner == 18.dp) Spacer(modifier = Modifier.height(8.dp))
                }
                if (state.isLoadingHistory && state.messages.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator()
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = state.showScrollToBottomButton,
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                enter = scaleIn(
                    transformOrigin = TransformOrigin(0.5f, 1f),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
                exit = scaleOut(
                    transformOrigin = TransformOrigin(0.5f, 1f),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            ) {
                FilledTonalButton (
                    onClick = {
                        scope.launch {
                            lazyListState.animateScrollToItem(0)
                        }
                    },
                    modifier = Modifier
                        .heightIn(sizeNewMessageButton)
                        .padding(paddingValues),
                    contentPadding = ButtonDefaults.contentPaddingFor(sizeNewMessageButton)
                )
                {
                    Icon(
                        painterResource(R.drawable.arrow_downward_24px),
                        contentDescription = "Scroll down",
                        modifier = Modifier.size(ButtonDefaults.iconSizeFor(sizeNewMessageButton)),
                    )
                    if (state.unreadBelowCount > 0) {
                        Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(sizeNewMessageButton)))
                        Text(
                            text = localizedString(
                                "NewMessages",
                                state.unreadBelowCount,
                                state.unreadBelowCount
                            ),
                            style = ButtonDefaults.textStyleFor(sizeNewMessageButton)
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = state.selectedMediaUris.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                val carouselState = rememberCarouselState { state.selectedMediaUris.size }
                ElevatedCard(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp, 32.dp, 0.dp, 0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 6.dp
                    ),
                ) {
                    Column(
                        Modifier.padding(
                            start = paddingValues.calculateStartPadding(LocalLayoutDirection.current) + 24.dp,
                            end = paddingValues.calculateEndPadding(LocalLayoutDirection.current) + 24.dp,
                            top = 24.dp,
                            bottom = paddingValues.calculateBottomPadding(),
                        )
                    ) {
                        HorizontalMultiBrowseCarousel(
                            state = carouselState,
                            modifier = Modifier
                                .height(100.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            itemSpacing = 2.dp,
                            preferredItemWidth = 100.dp,
                            maxSmallItemWidth = 100.dp,
                            minSmallItemWidth = 80.dp
                        ) { i ->
                            Box(modifier = Modifier.fillMaxSize()) {
                                MediaPreviewFromUri(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .maskClip(MaterialTheme.shapes.small),
                                    model = state.selectedMediaUris[i]
                                )
                                IconButton(
                                    onClick = {
                                        onEvent(InputEvent.ClearAttachments(state.selectedMediaUris[i]))
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(24.dp)
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        painterResource(R.drawable.close_24px),
                                        contentDescription = "Remove media",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MediaPreviewFromUri(modifier: Modifier = Modifier, model: Uri) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isVideo by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val context = LocalContext.current

    LaunchedEffect(model) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val mimeType = context.contentResolver.getType(model)
            isVideo = mimeType?.startsWith("video/") == true

            bitmap = if (isVideo) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, model)
                    retriever.frameAtTime
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                } finally {
                    retriever.release()
                }
            } else {
                try {
                    val source = ImageDecoder.createSource(context.contentResolver, model)
                    ImageDecoder.decodeBitmap(source)
                } catch (e: IOException) {
                    e.printStackTrace()
                    null
                }
            }
        }
        isLoading = false
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            LoadingIndicator()
        } else if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = if (isVideo) "Превью видео" else "Изображение",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            if (isVideo) {
                Icon(
                    painter = painterResource(R.drawable.video_file_24px),
                    contentDescription = "Иконка видео",
                    modifier = Modifier.size(32.dp),
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
        } else {
            Icon(
                painter = painterResource(R.drawable.broken_image_24px),
                contentDescription = "Ошибка загрузки",
                modifier = Modifier.size(32.dp),
                tint = Color.Gray
            )
        }
    }
}
