package com.xxcactussell.presentation.messages.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xxcactussell.domain.FormattedText
import com.xxcactussell.presentation.tools.FormattedTextView

@Composable
fun Caption(caption: FormattedText?, textColor: Color = MaterialTheme.colorScheme.onSurface) {
    if (caption != null && caption.text.isNotEmpty()) {
        FormattedTextView(
            text = caption,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .wrapContentWidth(align = Alignment.Start),
            color = textColor,
            lineOffset = 2.dp
        )
    }
}