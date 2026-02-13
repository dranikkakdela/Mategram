package com.xxcactussell.presentation.messages.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.xxcactussell.customdomain.ForwardFullInfo
import com.xxcactussell.mategram.presentation.R
import com.xxcactussell.presentation.localization.localizedString
import com.xxcactussell.presentation.tools.formatTimestampToDateTime
import kotlin.math.abs

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ForwardMarkup(
    forwardInfo: ForwardFullInfo,
    isDateShown: Boolean = false,
    isOutgoing: Boolean,
    onLinkClicked: (Long, Long?) -> Unit
) {
    Row(
        modifier = Modifier.clickable { forwardInfo.link?.chatId?.let { onLinkClicked(it, forwardInfo.link?.messageId) } },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isOutgoing) {
            Icon(
                painterResource(R.drawable.forward_24px),
                "",
                tint = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.tertiary
                )[abs(forwardInfo.link?.chatId?.rem(3) ?: 0).toInt()],
                modifier = Modifier.size(16.dp)
            )

            AnimatedVisibility(
                isDateShown
            ) {
                Text(
                    text = formatTimestampToDateTime(forwardInfo.date),
                    style = MaterialTheme.typography.labelSmallEmphasized,
                    color = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.tertiary
                    )[abs(forwardInfo.link?.chatId?.rem(3) ?: 0).toInt()]
                )
            }
        }
        val text = when {
            !forwardInfo.chat.isNullOrEmpty() && !forwardInfo.signature.isNullOrEmpty() -> "${forwardInfo.chat} (${forwardInfo.signature})"
            !forwardInfo.chat.isNullOrEmpty() -> forwardInfo.chat!!
            !forwardInfo.signature.isNullOrEmpty() -> forwardInfo.signature!!
            else -> null
        }
        Text(
            modifier = if(isOutgoing) Modifier.padding(end = 16.dp) else Modifier.padding(start = 16.dp),
            text = text ?: if (forwardInfo.isHidden) localizedString("AnonymousForward") else localizedString("HiddenName"),
            style = MaterialTheme.typography.labelLargeEmphasized,
            color = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.tertiary
            )[abs(forwardInfo.link?.chatId?.rem(3) ?: 0).toInt()]
        )
        if (!isOutgoing) {
            AnimatedVisibility(
                isDateShown
            ) {
                Text(
                    text = formatTimestampToDateTime(forwardInfo.date),
                    style = MaterialTheme.typography.labelSmallEmphasized,
                    color = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.tertiary
                    )[abs(forwardInfo.link?.chatId?.rem(3) ?: 0).toInt()]
                )
            }
            Icon(
                painterResource(R.drawable.forward_24px),
                "",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}