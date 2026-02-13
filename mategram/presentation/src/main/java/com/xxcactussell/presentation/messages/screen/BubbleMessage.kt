package com.xxcactussell.presentation.messages.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xxcactussell.customdomain.ForwardFullInfo
import com.xxcactussell.domain.Message
import com.xxcactussell.domain.MessageSendingStateFailed
import com.xxcactussell.domain.MessageSendingStatePending
import com.xxcactussell.mategram.presentation.R
import com.xxcactussell.presentation.chats.screen.ChatAvatar
import com.xxcactussell.presentation.messages.model.MessageUiItem
import com.xxcactussell.presentation.messages.model.MessagesEvent
import com.xxcactussell.presentation.messages.model.getAvatar
import com.xxcactussell.presentation.messages.model.getChatId
import com.xxcactussell.presentation.messages.model.getEditTime
import com.xxcactussell.presentation.messages.model.getMessage
import com.xxcactussell.presentation.messages.model.getMessageId
import com.xxcactussell.presentation.messages.model.getReactions
import com.xxcactussell.presentation.tools.formatTimestampToDateTime
import kotlin.math.abs

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BubbleMessage(
    modifier: Modifier = Modifier,
    message: MessageUiItem,
    isDateShown: Boolean = false,
    isUnread: Boolean,
    isOutgoing: Boolean,
    hasBubble: Boolean,
    topCorner: Dp,
    bottomCorner: Dp,
    needSenderName: Boolean,
    needAvatar: Boolean,
    isGroup: Boolean,
    onEvent: (Any) -> Unit,
    replyProgress: @Composable (() -> Unit),
    onReplyClicked: (Long) -> Unit,
    onLinkClicked: (Long, Long?) -> Unit,
    content: @Composable (() -> Unit)
) {
    val alignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val haptic = LocalHapticFeedback.current

    val forwardInfo : ForwardFullInfo? = when (message) {
        is MessageUiItem.MessageItem -> if (message.message.forwardFullInfo != null) message.message.forwardFullInfo else null
        is MessageUiItem.AlbumItem -> {
            message.messages.firstOrNull { it.message.forwardFullInfo != null }?.message?.forwardFullInfo
        }
        else -> null
    }

    val backgroundColor = if (!hasBubble) {
        Color.Transparent
    } else if (forwardInfo != null) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        if (isOutgoing) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        }
    }

    val shape = if (isOutgoing) {
        RoundedCornerShape(18.dp, topCorner, bottomCorner, 18.dp)
    } else {
        RoundedCornerShape(topCorner, 18.dp, 18.dp, bottomCorner)
    }

    val startPadding = if(isOutgoing) 64.dp
    else {
        if (isGroup) 42.dp
        else 0.dp
    }

    val sendingState = when (message) {
        is MessageUiItem.MessageItem -> message.message.sendingState
        is MessageUiItem.AlbumItem -> {
            if (message.messages.any { it.message.sendingState is MessageSendingStateFailed }) {
                message.messages.firstOrNull { it.message.sendingState is MessageSendingStateFailed }?.message?.sendingState
            } else if (message.messages.any { it.message.sendingState is MessageSendingStatePending }) {
                message.messages.firstOrNull { it.message.sendingState is MessageSendingStatePending }?.message?.sendingState
            } else {
                null
            }
        }
        else -> null
    }

    val replyingMessage : Message? = when (message) {
        is MessageUiItem.MessageItem -> if (message.message.replyTo != null) message.message else null
        is MessageUiItem.AlbumItem -> {
            message.messages.firstOrNull { it.message.replyTo != null }?.message
        }
        else -> null
    }

    val statusBackgroundColor = when(sendingState) {
        is MessageSendingStateFailed -> {
            MaterialTheme.colorScheme.error
        }
        else -> {
            Color.Transparent
        }
    }

    val statusIcon = when(sendingState) {
        is MessageSendingStateFailed -> {
            R.drawable.error_24px
        }
        is MessageSendingStatePending -> {
            R.drawable.timer_24px
        }
        else -> {
            R.drawable.check_24px
        }
    }

    val statusIconColor = when(sendingState) {
        is MessageSendingStateFailed -> MaterialTheme.colorScheme.onError
        else -> MaterialTheme.colorScheme.primary
    }

    val statusIconSize = when(sendingState) {
        is MessageSendingStateFailed -> 12.dp
        else -> 16.dp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = startPadding,
                end = if (isOutgoing) 0.dp else 64.dp
            )
    ) {
        Column(
            modifier = Modifier.align(alignment),
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
        ) {
            if (needSenderName) {
                Text(
                    modifier = Modifier.padding(start = 16.dp),
                    text = message.getAvatar()?.title ?: "@",
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.tertiary
                    )[abs(message.getAvatar()?.chatId?.rem(3) ?: 0).toInt()]
                )
            }

            if (replyingMessage != null) {
                ReplyMarkup(message = replyingMessage, isOutgoing, onReplyClicked)
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (forwardInfo != null) {
                ForwardMarkup(forwardInfo = forwardInfo, isDateShown = isDateShown, isOutgoing = isOutgoing, onLinkClicked = onLinkClicked)
                Spacer(modifier = Modifier.height(4.dp))
            }

            Box {
                Surface(
                    color = backgroundColor,
                    shape = shape
                ) {
                    content()
                }
                if (isUnread && isOutgoing) {
                    Surface(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomStart)
                            .offset((-20).dp),
                        color = statusBackgroundColor,
                        shape = CircleShape
                    ) {
                        Icon(
                            modifier = Modifier.requiredSize(statusIconSize),
                            painter = painterResource(statusIcon),
                            tint = statusIconColor,
                            contentDescription = ""
                        )
                    }
                }
                Box(Modifier
                    .size(36.dp)
                    .align(Alignment.BottomEnd)
                    .offset(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    replyProgress()
                }
            }

            if (needAvatar) {
                Box(Modifier.height(0.dp)) {
                    ChatAvatar(
                        modifier = Modifier
                            .requiredSize(42.dp)
                            .size(42.dp)
                            .offset(-startPadding - 4.dp, (-21).dp),
                        state = message.getAvatar(),
                        isPinned = false,
                        isOnline = false
                    )
                }
            }

            val reactions = message.getReactions()

            AnimatedVisibility(
                visible = reactions.isNotEmpty()
            ) {
                FlowRow(
                    modifier = Modifier.padding(top = 4.dp),
                    maxItemsInEachRow = 4,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    reactions.forEach {
                        ReactionChip(it) {
                            if (it.isChosen) {
                                haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                            }
                            onEvent(MessagesEvent.ToggleReaction(
                                message.getChatId(),
                                message.getMessageId() ?: 0L,
                                it.type
                            ))
                        }
                    }
                }
            }

            AnimatedVisibility(
                isDateShown
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(painterResource(R.drawable.send_24px), "sent", Modifier.size(16.dp))
                        Text(
                            text = formatTimestampToDateTime(message.getMessage()?.date ?: 0),
                            style = MaterialTheme.typography.bodySmallEmphasized
                        )
                    }
                    message.getEditTime()?.let {
                        if (it > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "(",
                                    style = MaterialTheme.typography.bodySmallEmphasized
                                )
                                Icon(
                                    painterResource(R.drawable.edit_note_24px),
                                    "edited",
                                    Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${formatTimestampToDateTime(it)})",
                                    style = MaterialTheme.typography.bodySmallEmphasized
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

