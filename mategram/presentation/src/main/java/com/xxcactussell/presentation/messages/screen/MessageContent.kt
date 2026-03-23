package com.xxcactussell.presentation.messages.screen

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.xxcactussell.customdomain.ForwardFullInfo
import com.xxcactussell.domain.MessageAnimatedEmoji
import com.xxcactussell.domain.MessageAnimation
import com.xxcactussell.domain.MessageDocument
import com.xxcactussell.domain.MessagePhoto
import com.xxcactussell.domain.MessageSendingStateFailed
import com.xxcactussell.domain.MessageSendingStatePending
import com.xxcactussell.domain.MessageSticker
import com.xxcactussell.domain.MessageText
import com.xxcactussell.domain.MessageVideo
import com.xxcactussell.domain.MessageVideoNote
import com.xxcactussell.domain.MessageVoiceNote
import com.xxcactussell.domain.Sticker
import com.xxcactussell.player.PlayerState
import com.xxcactussell.presentation.LocalRootViewModel
import com.xxcactussell.presentation.LocalSharedTransitionScope
import com.xxcactussell.presentation.messages.model.MessageUiItem
import com.xxcactussell.presentation.messages.model.getChatId
import com.xxcactussell.presentation.tools.ColumnWidthOf
import com.xxcactussell.presentation.tools.FormattedTextView
import com.xxcactussell.presentation.tools.Sticker
import com.xxcactussell.presentation.tools.messageContentAspectRatio

@Composable
fun MessageContent(message: MessageUiItem.MessageItem, playerState: PlayerState, onMediaClicked: (Long) -> Unit, onEvent: (Any) -> Unit) {
    val isOutgoing = message.message.isOutgoing

    val messageTextColor = if (message.message.forwardFullInfo != null) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else if (isOutgoing) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    val isSending = message.message.sendingState is MessageSendingStatePending
    val isFailed = message.message.sendingState is MessageSendingStateFailed

    when (val content = message.message.content) {
        is MessageText -> MessageTextContent(content, messageTextColor)
        is MessagePhoto -> MessagePhotoContent(message, messageTextColor, isSending, isFailed, onEvent, onMediaClicked)
        is MessageVideo -> MessageVideo(message, messageTextColor, isSending, isFailed, onEvent, onMediaClicked)
        is MessageSticker -> MessageStickerContent(content.sticker, 172.dp)
        is MessageAnimatedEmoji -> MessageStickerContent(content.animatedEmoji.sticker, 92.dp)
        is MessageDocument -> DocumentMessage(message, messageTextColor, isSending, isFailed, onEvent)
        is MessageAnimation -> AnimationMessageContent(message, messageTextColor, isSending, isFailed, onMediaClicked)
        is MessageVoiceNote -> VoiceMessage(message, playerState, onEvent)
        is MessageVideoNote -> MessageVideoNoteContent(message, playerState, onEvent)
        else -> {
            Text(
                modifier = Modifier.padding(8.dp),
                text = "Unsupported message type: ${content.javaClass.simpleName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MessageStickerContent(sticker: Sticker?, size: Dp) {
    if (sticker!= null) {
        val localSticker = sticker.sticker

        val rootViewModel = LocalRootViewModel.current
        val fileUpdates = rootViewModel.files.collectAsState()

        val file = fileUpdates.value[sticker.sticker.id] ?: localSticker
        val path = file.local.path

        if (file.local.isDownloadingCompleted && path.isNotEmpty()) {
            Sticker(Modifier, path, size)
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            )
            if (!file.local.isDownloadingActive) {
                LaunchedEffect(key1 = file.id) {
                    rootViewModel.downloadFile(file.id)
                }
            }
        }
    }
}

@Composable
fun MessageVideo(
    message: MessageUiItem.MessageItem,
    textColor: Color,
    isSending: Boolean,
    isFailed: Boolean,
    onEvent: (Any) -> Unit,
    onMediaClicked: (Long) -> Unit
) {
    val content = message.message.content as MessageVideo

    ColumnWidthOf(
        modifier =
            if(content.caption.text.isEmpty())
                Modifier
            else if (content.showCaptionAboveMedia)
                Modifier.padding(top = 8.dp)
            else
                Modifier.padding(bottom = 8.dp),
        rulerId = "photo",
        horizontalSpacers = 8.dp
    ) {
        if (content.showCaptionAboveMedia) {
            Caption(content.caption, textColor)
        }

        val imageHeight = content.video.thumbnail?.height
        val imageWidth = content.video.thumbnail?.width



        val sharedTransitionScope = LocalSharedTransitionScope.current
            ?: throw IllegalStateException("No SharedElementScope found")
        val animatedVisibilityScope = LocalNavAnimatedContentScope.current

        with(sharedTransitionScope) {
            MessageVideoContent(
                modifier = Modifier
                    .layoutId("photo")
                    .messageContentAspectRatio(imageWidth?.toFloat(), imageHeight?.toFloat())
                    .clip(RoundedCornerShape(14.dp))
                    .sharedBounds(
                        rememberSharedContentState(key = "bounds_${message.message.id}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                    ),
                messageId = message.message.id,
                chatId = message.getChatId(),
                video = content.video,
                isSending = isSending,
                isFailed = isFailed,
                videoCover = content.cover,
                onEvent = onEvent,
                onMediaClicked = onMediaClicked
            )
        }
        if (!content.showCaptionAboveMedia) {
            Caption(content.caption, textColor)
        }
    }
}

@Composable
fun MessagePhotoContent(
    message: MessageUiItem.MessageItem,
    textColor: Color,
    isSending: Boolean,
    isFailed: Boolean,
    onEvent: (Any) -> Unit,
    onMediaClicked: (Long) -> Unit,
) {
    val content = message.message.content as MessagePhoto

    val w = content.photo.sizes.maxByOrNull { it.width }?.width?.toFloat()
    val h = content.photo.sizes.maxByOrNull { it.width }?.height?.toFloat()

    ColumnWidthOf(
        modifier =
            if(content.caption.text.isEmpty())
                Modifier
            else if (content.showCaptionAboveMedia)
                Modifier.padding(top = 8.dp)
            else
                Modifier.padding(bottom = 8.dp),
        rulerId = "photo",
        horizontalSpacers = 8.dp
    ) {
        if (content.showCaptionAboveMedia) {
            Caption(content.caption, textColor)
        }



        val sharedTransitionScope = LocalSharedTransitionScope.current
            ?: throw IllegalStateException("No SharedElementScope found")
        val animatedVisibilityScope = LocalNavAnimatedContentScope.current

        with(sharedTransitionScope) {
            PhotoMessage(
                modifier = Modifier
                    .layoutId("photo")
                    .messageContentAspectRatio(w, h)
                    .clip(RoundedCornerShape(14.dp))
                    .sharedBounds(
                        rememberSharedContentState(key = "bounds_${message.message.id}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                    ),
                messageId = message.message.id,
                photo = content.photo,
                isSending = isSending,
                isFailed = isFailed,
                onEvent = onEvent,
                chatId = message.getChatId(),
                onMediaClicked = onMediaClicked
            )
        }
        if (!content.showCaptionAboveMedia) {
            Caption(content.caption, textColor)
        }
    }
}

@Composable
fun MessageTextContent(content: MessageText, textColor: Color = MaterialTheme.colorScheme.onSurface) {
    val style = MaterialTheme.typography.bodyMedium
    val density = LocalDensity.current

    val singleLineThreshold = remember(style, density) {
        with(density) {
            val fontSize = if (style.fontSize.isSpecified) style.fontSize else 16.sp
            val lineHeight = if (style.lineHeight.isSpecified) style.lineHeight else fontSize
            (lineHeight.toDp() + 2.dp).toPx() * 1.5f
        }
    }

    FormattedTextView(
        text = content.text,
        style = style,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val isSingleLine = placeable.height <= singleLineThreshold
                val verticalPadding = if (isSingleLine) 8.dp.roundToPx() else 16.dp.roundToPx()

                layout(placeable.width, placeable.height + verticalPadding * 2) {
                    placeable.place(0, verticalPadding)
                }
            },
        color = textColor,
        lineOffset = 2.dp
    )
}