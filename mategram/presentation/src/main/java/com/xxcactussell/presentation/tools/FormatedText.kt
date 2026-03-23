package com.xxcactussell.presentation.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.LocalBackgroundTextMeasurementExecutor
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.xxcactussell.domain.File
import com.xxcactussell.domain.FormattedText
import com.xxcactussell.domain.Sticker
import com.xxcactussell.domain.StickerFullTypeCustomEmoji
import com.xxcactussell.domain.TextEntity
import com.xxcactussell.domain.TextEntityTypeBlockQuote
import com.xxcactussell.domain.TextEntityTypeBold
import com.xxcactussell.domain.TextEntityTypeCode
import com.xxcactussell.domain.TextEntityTypeCustomEmoji
import com.xxcactussell.domain.TextEntityTypeExpandableBlockQuote
import com.xxcactussell.domain.TextEntityTypeItalic
import com.xxcactussell.domain.TextEntityTypeMention
import com.xxcactussell.domain.TextEntityTypeMentionName
import com.xxcactussell.domain.TextEntityTypePre
import com.xxcactussell.domain.TextEntityTypePreCode
import com.xxcactussell.domain.TextEntityTypeSpoiler
import com.xxcactussell.domain.TextEntityTypeStrikethrough
import com.xxcactussell.domain.TextEntityTypeTextUrl
import com.xxcactussell.domain.TextEntityTypeUnderline
import com.xxcactussell.domain.TextEntityTypeUrl
import com.xxcactussell.mategram.presentation.R
import com.xxcactussell.presentation.LocalRootViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val TEXT_MEASUREMENT_EXECUTOR = Dispatchers.Default.asExecutor()

object TextEntityTypeSpacerStart
object TextEntityTypeSpacerEnd

data class VisualEntity(
    val offset: Int,
    val length: Int,
    val type: Any,
    val originalData: String,
    val originalOffset: Int
)

sealed interface TextBlock {
    data class Paragraph(val text: String, val entities: List<VisualEntity>) : TextBlock
    data class Code(val text: String, val lang: String) : TextBlock
    data class Quote(val text: String, val entities: List<VisualEntity>) : TextBlock
}

@Composable
fun FormattedTextView(
    modifier: Modifier = Modifier,
    text: FormattedText = FormattedText("", emptyList()),
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    clickable: Boolean = true,
    color: Color = Color.Unspecified,
    lineOffset: Dp = 0.dp
) {
    val rootViewModel = LocalRootViewModel.current
    val filesFlow = rootViewModel.files
    val stickers by rootViewModel.stickers.collectAsState()

    var revealedSpoilers by remember(text) { mutableStateOf<Set<Int>>(emptySet()) }

    LaunchedEffect(text) {
        text.entities
            .mapNotNull { (it.type as? TextEntityTypeCustomEmoji)?.customEmojiId }
            .distinct()
            .forEach { rootViewModel.requestStickerInfo(it) }
    }

    val (processedText, visualEntities) = remember(text) {
        preprocessText(text.text, text.entities)
    }

    val blocks = remember(processedText, visualEntities) {
        parseToBlocks(processedText, visualEntities)
    }

    val activeBlocks = if (maxLines == 1) blocks.take(1) else blocks

    val density = LocalDensity.current
    val adjustedStyle = remember(style, lineOffset, density) {
        if (lineOffset == 0.dp) style
        else {
            val extraSp = with(density) { lineOffset.toSp() }
            val baseLineHeight = if (style.lineHeight.isSpecified) style.lineHeight else if (style.fontSize.isSpecified) style.fontSize else 16.sp
            style.copy(lineHeight = (baseLineHeight.value + extraSp.value).sp)
        }
    }

    CompositionLocalProvider(
        LocalBackgroundTextMeasurementExecutor provides TEXT_MEASUREMENT_EXECUTOR
    ) {
        Column(modifier = modifier) {
            activeBlocks.forEach { block ->
                when (block) {
                    is TextBlock.Paragraph -> ParagraphNode(
                        block = block,
                        style = adjustedStyle,
                        color = color,
                        maxLines = maxLines,
                        overflow = overflow,
                        softWrap = softWrap,
                        clickable = clickable,
                        revealedSpoilers = revealedSpoilers,
                        stickers = stickers,
                        filesFlow = filesFlow,
                        onDownloadRequest = { rootViewModel.downloadFile(it) },
                        onSpoilerReveal = { revealedSpoilers = revealedSpoilers + it }
                    )
                    is TextBlock.Code -> CodeNode(
                        block = block,
                        style = adjustedStyle,
                        maxLines = maxLines,
                        overflow = overflow,
                        softWrap = softWrap,
                        lang = block.lang
                    )
                    is TextBlock.Quote -> QuoteNode(
                        block = block,
                        style = adjustedStyle,
                        color = color,
                        maxLines = maxLines,
                        overflow = overflow,
                        softWrap = softWrap,
                        clickable = clickable,
                        revealedSpoilers = revealedSpoilers,
                        stickers = stickers,
                        filesFlow = filesFlow,
                        onDownloadRequest = { rootViewModel.downloadFile(it) },
                        onSpoilerReveal = { revealedSpoilers = revealedSpoilers + it }
                    )
                }
            }
        }
    }
}

@Composable
private fun ParagraphNode(
    block: TextBlock.Paragraph,
    style: TextStyle,
    color: Color,
    maxLines: Int,
    overflow: TextOverflow,
    softWrap: Boolean,
    clickable: Boolean,
    revealedSpoilers: Set<Int>,
    stickers: Map<Long, Sticker>,
    filesFlow: Flow<Map<Int, File>>,
    onDownloadRequest: (Int) -> Unit,
    onSpoilerReveal: (Int) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val density = LocalDensity.current

    val primaryColor = MaterialTheme.colorScheme.primary

    val usernameColor = MaterialTheme.colorScheme.tertiary

    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val typography = MaterialTheme.typography

    val fontSize = style.fontSize

    val inlineContentMap = remember(block.entities, stickers, fontSize, density) {
        val map = mutableMapOf<String, InlineTextContent>()
        val size = if (fontSize.isSpecified) fontSize else 16.sp
        val dpSize = size * 1.4
        val placeholder = Placeholder(size * 1.4, size, PlaceholderVerticalAlign.TextCenter)

        block.entities.forEach { entity ->
            if (entity.type is TextEntityTypeCustomEmoji) {
                val emojiIdKey = "emoji_${entity.type.customEmojiId}_${entity.originalOffset}"
                val sticker = stickers[entity.type.customEmojiId]

                if (sticker != null) {
                    val needRepaint = (sticker.fullType as? StickerFullTypeCustomEmoji)?.needsRepainting ?: false
                    map[emojiIdKey] = InlineTextContent(placeholder) {
                        Box(modifier = Modifier.requiredSize(dpSize.value.dp)) {
                            SmartEmojiBox(
                                fileId = sticker.sticker.id,
                                size = dpSize.value.dp,
                                filesFlow = filesFlow,
                                color = if (needRepaint) color else Color.Unspecified,
                                onDownloadRequest = onDownloadRequest
                            )
                        }
                    }
                }
            } else if (entity.type === TextEntityTypeSpacerStart || entity.type === TextEntityTypeSpacerEnd) {
                val spacerId = "spacer_${entity.originalOffset}_${entity.offset}"
                val spacerWidthSp = with(density) { 8.dp.toSp() }
                map[spacerId] = InlineTextContent(
                    Placeholder(spacerWidthSp, 1.sp, PlaceholderVerticalAlign.TextCenter)
                ) {
                    Box(modifier = Modifier)
                }
            }
        }
        map
    }

    val annotatedString = remember(block, typography, onPrimaryColor) {
        buildInlineStrings(block.text, block.entities, typography, onPrimaryColor)
    }

    val spoilerEntities = block.entities.filter { it.type is TextEntityTypeSpoiler }
    val unrevealedSpoilers = spoilerEntities.filter { it.originalOffset !in revealedSpoilers }
    val hasUnrevealedSpoilers = unrevealedSpoilers.isNotEmpty()

    val textModifier = if (clickable) {
        Modifier.pointerInput(block.entities, revealedSpoilers) {
            detectTapGestures { offset ->
                textLayoutResult?.let { layout ->
                    val charIndex = layout.getOffsetForPosition(offset)

                    val spoiler = block.entities.find {
                        it.type is TextEntityTypeSpoiler && charIndex >= it.offset && charIndex < it.offset + it.length && it.originalOffset !in revealedSpoilers
                    }
                    if (spoiler != null) {
                        onSpoilerReveal(spoiler.originalOffset)
                        return@detectTapGestures
                    }

                    val url = block.entities.find {
                        (it.type is TextEntityTypeTextUrl || it.type is TextEntityTypeUrl) && charIndex >= it.offset && charIndex < it.offset + it.length
                    }
                    if (url != null) {
                        var uri = url.originalData
                        if (!uri.startsWith("http://") && !uri.startsWith("https://")) {
                            uri = "https://$uri"
                        }
                        uriHandler.openUri(uri)
                        return@detectTapGestures
                    }
                }
            }
        }
    } else {
        Modifier
    }

    Box(modifier = Modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            textLayoutResult?.let { layout ->
                block.entities.forEach { entity ->
                    when (entity.type) {
                        is TextEntityTypeSpoiler -> {
                            val path = buildWrappedPath(layout, entity.offset, entity.offset + entity.length, density)
                            drawPath(path, color = primaryColor.copy(alpha = 0.3f))
                        }

                        is TextEntityTypeTextUrl, is TextEntityTypeUrl -> {
                            val path = buildWrappedPath(layout, entity.offset, entity.offset + entity.length, density)
                            drawPath(path, color = primaryColor)
                        }

                        is TextEntityTypeMentionName, is TextEntityTypeMention -> {
                            val path = buildWrappedPath(layout, entity.offset, entity.offset + entity.length, density)
                            drawPath(path, color = usernameColor)
                        }
                    }
                }
            }
        }

        val spoilerPaths = remember(textLayoutResult, unrevealedSpoilers, density) {
            val path = Path()
            textLayoutResult?.let { layout ->
                unrevealedSpoilers.forEach { spoiler ->
                    path.addPath(buildWrappedPath(layout, spoiler.offset, spoiler.offset + spoiler.length, density))
                }
            }
            path
        }

        val baseTextModifier = textModifier.drawWithContent {
            if (hasUnrevealedSpoilers) {
                clipPath(spoilerPaths, clipOp = ClipOp.Difference) {
                    this@drawWithContent.drawContent()
                }
            } else {
                this@drawWithContent.drawContent()
            }
        }

        Text(
            text = annotatedString,
            style = style,
            color = color,
            inlineContent = inlineContentMap,
            maxLines = maxLines,
            overflow = overflow,
            softWrap = softWrap,
            onTextLayout = { textLayoutResult = it },
            modifier = baseTextModifier
        )

        if (hasUnrevealedSpoilers) {
            val spoilerShape = remember(spoilerPaths) {
                object : Shape {
                    override fun createOutline(
                        size: Size,
                        layoutDirection: LayoutDirection,
                        density: Density
                    ): Outline {
                        return Outline.Generic(spoilerPaths)
                    }
                }
            }

            Text(
                text = annotatedString,
                style = style,
                color = color,
                inlineContent = inlineContentMap,
                maxLines = maxLines,
                overflow = overflow,
                softWrap = softWrap,
                modifier = Modifier
                    .graphicsLayer {
                        clip = true
                        shape = spoilerShape
                    }
                    .blur(8.dp)
            )
        }
    }
}

@Composable
private fun CodeNode(
    block: TextBlock.Code,
    style: TextStyle,
    maxLines: Int,
    overflow: TextOverflow,
    softWrap: Boolean,
    lang: String
) {
    if (maxLines != 1) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
        ) {
            if (lang.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = lang.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = block.text,
                style = style.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                maxLines = maxLines,
                overflow = overflow,
                softWrap = softWrap,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    } else {
        Text(
            text = block.text,
            style = style.copy(fontFamily = FontFamily.Monospace),
            maxLines = maxLines
        )
    }
}

@Composable
private fun QuoteNode(
    block: TextBlock.Quote,
    style: TextStyle,
    color: Color,
    maxLines: Int,
    overflow: TextOverflow,
    softWrap: Boolean,
    clickable: Boolean,
    revealedSpoilers: Set<Int>,
    stickers: Map<Long, Sticker>,
    filesFlow: Flow<Map<Int, File>>,
    onDownloadRequest: (Int) -> Unit,
    onSpoilerReveal: (Int) -> Unit
) {
    if (maxLines != 1) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
                .padding(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.format_quote_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box {
                ParagraphNode(
                    block = TextBlock.Paragraph(block.text, block.entities),
                    style = style.copy(fontStyle = FontStyle.Italic),
                    color = color,
                    maxLines = maxLines,
                    overflow = overflow,
                    softWrap = softWrap,
                    clickable = clickable,
                    revealedSpoilers = revealedSpoilers,
                    stickers = stickers,
                    filesFlow = filesFlow,
                    onDownloadRequest = onDownloadRequest,
                    onSpoilerReveal = onSpoilerReveal
                )
            }
        }
    } else {
        Text(
            text = block.text,
            style = style.copy(fontStyle = FontStyle.Italic),
            maxLines = maxLines
        )
    }
}

@Composable
private fun SmartEmojiBox(
    fileId: Int,
    size: Dp,
    filesFlow: Flow<Map<Int, File>>,
    color: Color? = Color.Unspecified,
    onDownloadRequest: (Int) -> Unit
) {
    val file by produceState<File?>(initialValue = null, key1 = fileId) {
        filesFlow
            .map { it[fileId] }
            .distinctUntilChanged()
            .collect { value = it }
    }

    val path = file?.local?.path?.takeIf { it.isNotEmpty() && file?.local?.isDownloadingCompleted == true }
    val needsDownload = file == null || (!file!!.local.isDownloadingActive && !file!!.local.isDownloadingCompleted)

    if (path != null) {
        Sticker(modifier = Modifier, path, size, color)
    } else {
        Spacer(Modifier.size(size))
        if (needsDownload) {
            LaunchedEffect(fileId) {
                onDownloadRequest(fileId)
            }
        }
    }
}

private fun buildWrappedPath(
    layout: TextLayoutResult,
    startOffset: Int,
    endOffset: Int,
    density: Density
): Path {
    val path = Path()
    val textLen = layout.layoutInput.text.length
    if (textLen == 0 || startOffset >= endOffset) return path

    val safeStart = startOffset.coerceIn(0, textLen)
    val safeEnd = endOffset.coerceIn(0, textLen)

    with(density) {
        val startLine = layout.getLineForOffset(safeStart)
        val endLine = layout.getLineForOffset(maxOf(safeStart, safeEnd - 1))

        for (line in startLine..endLine) {
            val isFirstLine = line == startLine
            val isLastLine = line == endLine

            val charStart = if (isFirstLine) safeStart else layout.getLineStart(line)
            var charEnd = if (isLastLine) safeEnd else layout.getLineEnd(line, visibleEnd = true)

            if (charEnd > charStart && layout.layoutInput.text[charEnd - 1] == '\n') {
                charEnd -= 1
            }

            if (charStart >= charEnd && startLine != endLine) continue

            // Получаем физические координаты с учетом inline-блоков эмодзи и пустышек
            val startBox = layout.getBoundingBox(charStart)
            val endBox = layout.getBoundingBox(maxOf(charStart, charEnd - 1))

            val leftPos = if (isFirstLine) startBox.left else layout.getLineLeft(line)
            val rightPos = if (isLastLine) endBox.right else layout.getLineRight(line)

            val left = minOf(leftPos, rightPos)
            val right = maxOf(leftPos, rightPos)

            val topY = layout.getLineTop(line)
            val bottomY = layout.getLineBottom(line)

            // Высота плашки 20.dp
            val centerY = (topY + bottomY) / 2f
            val halfHeight = 10.dp.toPx()

            val top = centerY - halfHeight
            val bottom = centerY + halfHeight

            // Для начала и конца строк плашек — радиус 10.dp. Для разрывов текста (переносов) — 2.dp.
            val startRadius = if (isFirstLine) 10.dp.toPx() else 2.dp.toPx()
            val endRadius = if (isLastLine) 10.dp.toPx() else 2.dp.toPx()

            // Ручные отступы 4.dp только для внутренних переносов строк. В начале и конце отступы по 8.dp уже дает \u200B-пустышка.
            val startPad = if (isFirstLine) 0f else 4.dp.toPx()
            val endPad = if (isLastLine) 0f else 4.dp.toPx()

            path.addRoundRect(
                RoundRect(
                    left = left - startPad,
                    top = top,
                    right = right + endPad,
                    bottom = bottom,
                    topLeftCornerRadius = CornerRadius(startRadius, startRadius),
                    topRightCornerRadius = CornerRadius(endRadius, endRadius),
                    bottomRightCornerRadius = CornerRadius(endRadius, endRadius),
                    bottomLeftCornerRadius = CornerRadius(startRadius, startRadius)
                )
            )
        }
    }
    return path
}

private fun areInSameGroup(p1: VisualEntity, p2: VisualEntity, text: String, entities: List<VisualEntity>): Boolean {
    if (p1.type::class != p2.type::class) return false
    if (p1.type is TextEntityTypeTextUrl && p1.originalData != p2.originalData) return false
    if (p1.type is TextEntityTypeUrl && p1.originalData != p2.originalData) return false

    val gapStart = p1.offset + p1.length
    val gapEnd = p2.offset
    if (gapStart >= gapEnd) return true

    for (i in gapStart until gapEnd) {
        val char = text[i]
        val isSpace = char.isWhitespace()
        val isEmoji = entities.any { it.type is TextEntityTypeCustomEmoji && i >= it.offset && i < it.offset + it.length }
        if (!isSpace && !isEmoji) return false
    }
    return true
}

private fun preprocessText(originalText: String, originalEntities: List<TextEntity>): Pair<String, List<VisualEntity>> {
    var currentText = originalText
    val visualEntities = originalEntities.map {
        VisualEntity(it.offset, it.length, it.type, originalText.substring(it.offset, it.offset + it.length), it.offset)
    }.toMutableList()

    val urlEntities = visualEntities.filter { it.type is TextEntityTypeTextUrl || it.type is TextEntityTypeUrl }.sortedByDescending { it.offset }

    for (entity in urlEntities) {
        val originalSubstring = entity.originalData
        val cleanUrl = originalSubstring.removePrefix("http://").removePrefix("https://")
        val parts = cleanUrl.split("/", limit = 2)
        val domain = parts[0]
        val path = parts.getOrNull(1) ?: ""
        val truncated = if (path.length > 4) "$domain/${path.take(4)}..." else if (path.isEmpty()) domain else "$domain/$path"

        val diff = truncated.length - originalSubstring.length
        currentText = currentText.substring(0, entity.offset) + truncated + currentText.substring(entity.offset + entity.length)

        val entityIndex = visualEntities.indexOf(entity)
        visualEntities[entityIndex] = entity.copy(length = truncated.length)

        for (i in 0 until visualEntities.size) {
            if (i == entityIndex) continue
            val other = visualEntities[i]
            if (other.offset >= entity.offset + originalSubstring.length) {
                visualEntities[i] = other.copy(offset = other.offset + diff)
            } else if (other.offset <= entity.offset && other.offset + other.length >= entity.offset + originalSubstring.length) {
                visualEntities[i] = other.copy(length = other.length + diff)
            }
        }
    }

    val pillTypes = listOf(
        TextEntityTypeTextUrl::class,
        TextEntityTypeUrl::class,
        TextEntityTypeMentionName::class,
        TextEntityTypeMention::class,
        TextEntityTypeSpoiler::class
    )

    // Склеиваем разрывы спойлеров/ссылок, если они разбиты кастомным эмодзи
    val sortedPills = visualEntities.filter { it.type::class in pillTypes }.sortedBy { it.offset }
    val pillGroups = mutableListOf<MutableList<VisualEntity>>()

    for (pill in sortedPills) {
        val group = pillGroups.find { g -> areInSameGroup(g.last(), pill, currentText, visualEntities) }
        if (group != null) {
            group.add(pill)
        } else {
            pillGroups.add(mutableListOf(pill))
        }
    }

    val mergedPills = pillGroups.map { group ->
        val first = group.first()
        val last = group.last()
        val mergedOffset = first.offset
        val mergedLength = (last.offset + last.length) - first.offset
        VisualEntity(mergedOffset, mergedLength, first.type, first.originalData, first.originalOffset)
    }

    visualEntities.removeAll { it.type::class in pillTypes }
    visualEntities.addAll(mergedPills)

    val pillEntities = visualEntities.filter { it.type::class in pillTypes }.sortedByDescending { it.offset }
    val padChar = "\u200B"

    for (pill in pillEntities) {
        val currentPillIndex = visualEntities.indexOfFirst { it.originalOffset == pill.originalOffset && it.type == pill.type }
        if (currentPillIndex == -1) continue

        var currentPill = visualEntities[currentPillIndex]
        val end = currentPill.offset + currentPill.length

        currentText = currentText.substring(0, end) + padChar + currentText.substring(end)
        for (i in 0 until visualEntities.size) {
            val other = visualEntities[i]
            if (i == currentPillIndex) {
                visualEntities[i] = other.copy(length = other.length + 1)
            } else if (other.offset >= end) {
                visualEntities[i] = other.copy(offset = other.offset + 1)
            } else if (other.offset + other.length > end) {
                visualEntities[i] = other.copy(length = other.length + 1)
            }
        }
        visualEntities.add(VisualEntity(end, 1, TextEntityTypeSpacerEnd, padChar, currentPill.originalOffset))

        currentPill = visualEntities[currentPillIndex]
        val start = currentPill.offset

        currentText = currentText.substring(0, start) + padChar + currentText.substring(start)
        for (i in 0 until visualEntities.size) {
            val other = visualEntities[i]
            if (i == currentPillIndex) {
                visualEntities[i] = other.copy(offset = other.offset, length = other.length + 1)
            } else if (other.offset >= start) {
                visualEntities[i] = other.copy(offset = other.offset + 1)
            } else if (other.offset + other.length > start) {
                visualEntities[i] = other.copy(length = other.length + 1)
            }
        }
        visualEntities.add(VisualEntity(start, 1, TextEntityTypeSpacerStart, padChar, currentPill.originalOffset))
    }

    return currentText to visualEntities
}

private fun parseToBlocks(text: String, entities: List<VisualEntity>): List<TextBlock> {
    val blockTypes = listOf(
        TextEntityTypePre::class,
        TextEntityTypePreCode::class,
        TextEntityTypeBlockQuote::class,
        TextEntityTypeExpandableBlockQuote::class
    )

    val blockEntities = entities.filter { it.type::class in blockTypes }.sortedBy { it.offset }
    val inlineEntities = entities.filter { it.type::class !in blockTypes }

    val nodes = mutableListOf<TextBlock>()
    var currentIndex = 0

    for (block in blockEntities) {
        if (currentIndex < block.offset) {
            val subText = text.substring(currentIndex, block.offset)
            val shiftedEntities = inlineEntities
                .filter { it.offset >= currentIndex && it.offset < block.offset }
                .map { it.copy(offset = it.offset - currentIndex) }
            nodes.add(TextBlock.Paragraph(subText, shiftedEntities))
        }

        val blockText = text.substring(block.offset, block.offset + block.length)

        when (block.type) {
            is TextEntityTypePre -> {
                nodes.add(TextBlock.Code(blockText, ""))
            }
            is TextEntityTypePreCode -> {
                nodes.add(TextBlock.Code(blockText, block.type.language))
            }
            is TextEntityTypeBlockQuote, is TextEntityTypeExpandableBlockQuote -> {
                val shiftedEntities = inlineEntities
                    .filter { it.offset >= block.offset && it.offset < block.offset + block.length }
                    .map { it.copy(offset = it.offset - block.offset) }
                nodes.add(TextBlock.Quote(blockText, shiftedEntities))
            }
        }
        currentIndex = block.offset + block.length
    }

    if (currentIndex < text.length) {
        val subText = text.substring(currentIndex, text.length)
        val shiftedEntities = inlineEntities
            .filter { it.offset >= currentIndex }
            .map { it.copy(offset = it.offset - currentIndex) }
        nodes.add(TextBlock.Paragraph(subText, shiftedEntities))
    }

    return nodes
}

private fun buildInlineStrings(
    text: String,
    entities: List<VisualEntity>,
    typography: Typography,
    onPrimaryColor: Color
): AnnotatedString {
    val baseBuilder = AnnotatedString.Builder(text)

    entities.forEach { entity ->
        val start = entity.offset
        val end = entity.offset + entity.length

        when (entity.type) {
            is TextEntityTypeBold -> baseBuilder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
            is TextEntityTypeItalic -> baseBuilder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
            is TextEntityTypeUnderline -> baseBuilder.addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
            is TextEntityTypeStrikethrough -> baseBuilder.addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, end)
            is TextEntityTypeCode -> baseBuilder.addStyle(SpanStyle(fontFamily = FontFamily.Monospace), start, end)
            is TextEntityTypeTextUrl, is TextEntityTypeMentionName, is TextEntityTypeMention, is TextEntityTypeUrl -> {
                baseBuilder.addStyle(
                    typography.labelMedium.toSpanStyle().copy(color = onPrimaryColor),
                    start, end
                )
            }
            is TextEntityTypeSpacerStart, is TextEntityTypeSpacerEnd -> {
                val spacerId = "spacer_${entity.originalOffset}_${entity.offset}"
                baseBuilder.addStringAnnotation(
                    tag = "androidx.compose.foundation.text.inlineContent",
                    annotation = spacerId,
                    start = start,
                    end = end
                )
            }
            is TextEntityTypeCustomEmoji -> {
                val emojiIdKey = "emoji_${entity.type.customEmojiId}_${entity.originalOffset}"
                baseBuilder.addStringAnnotation(
                    tag = "androidx.compose.foundation.text.inlineContent",
                    annotation = emojiIdKey,
                    start = start,
                    end = end
                )
            }
        }
    }
    return baseBuilder.toAnnotatedString()
}