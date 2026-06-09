package com.xxcactussell.presentation.chats.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xxcactussell.presentation.chats.model.ChatItemUiState
import com.xxcactussell.presentation.localization.localizedString
import com.xxcactussell.presentation.messages.screen.MessagePreview
import com.xxcactussell.presentation.tools.formatTimestampToDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatItem(
    index: Int,
    uiState: ChatItemUiState,
    onChatClicked: (Long) -> Unit
) {
    // Плавная анимация цвета при выборе/прочтении
    val targetColor = when {
        uiState.isSelected -> MaterialTheme.colorScheme.inversePrimary
        uiState.isUnread -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }
    val containerColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chatItemColor"
    )

    val containerShape = RoundedCornerShape(if (uiState.isSelected) 32.dp else 24.dp)

    ListItem(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(containerShape)
            .clickable { onChatClicked(uiState.chat.id) },
        headlineContent = {
            Text(
                text = uiState.chat.title.ifBlank { localizedString("HiddenName") },
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                // Непрочитанные чаты — жирным шрифтом
                fontWeight = if (uiState.isUnread) FontWeight.SemiBold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            if (uiState.chat.lastMessage != null) {
                MessagePreview(uiState.chat.lastMessage!!)
            } else {
                Text(
                    text = localizedString("EventLogOriginalCaptionEmpty"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            ChatAvatar(
                modifier = Modifier.size(52.dp),
                state = uiState.photo,
                isPinned = uiState.isPinned,
                isOnline = uiState.isOnline
            )
        },
        trailingContent = {
            uiState.chat.lastMessage?.let { lastMessage ->
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTimestampToDate(lastMessage.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.isUnread)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    if (uiState.isUnread) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = if (uiState.chat.unreadCount > 99) "99+"
                                       else uiState.chat.unreadCount.toString(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = containerColor
        )
    )
}
