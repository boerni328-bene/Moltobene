package com.moltobene.app.ui.edit

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.moltobene.app.R
import com.moltobene.app.data.ocr.AreaFrame
import com.moltobene.app.data.ocr.AreaKind
import com.moltobene.app.data.ocr.CropArea
import com.moltobene.app.data.ocr.CropHandle
import com.moltobene.app.data.ocr.TextLanguage
import com.moltobene.app.ui.components.CenteredMessage
import com.moltobene.app.ui.theme.Spacing
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * „Bereich auswählen“ vor der Texterkennung: Das Foto erscheint mit einem Rahmen, gelesen wird nur,
 * was darin liegt. So bleiben Knöpfe, Werbung oder Fotos auf Bildschirmfotos außen vor.
 * Weitere Rahmen lassen sich hinzufügen und als „Titel“, „Zutaten“ oder „Zubereitung“ bezeichnen (#39) –
 * z. B. für Kochbuchseiten mit zwei Spalten. Am Anfang umfasst ein Rahmen „Alles“ die ganze Seite;
 * wer nichts ändern will, tippt nur „Text erkennen“. Alles Wichtige geht auch ohne Ziehen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AreaSelectionScreen(
    selection: AreaSelection,
    preview: Bitmap?,
    failed: Boolean,
    snackbarHostState: SnackbarHostState,
    language: String?,
    onLanguageChange: (String?) -> Unit,
    onFrameChange: (index: Int, area: CropArea) -> Unit,
    onSelectFrame: (Int) -> Unit,
    onKindChange: (AreaKind) -> Unit,
    onAddFrame: () -> Unit,
    onRemoveFrame: () -> Unit,
    onWholePage: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    BackHandler(onBack = onCancel)
    var languageMenuOpen by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.area_title), modifier = Modifier.semantics { heading() })
                        if (selection.pageCount > 1) {
                            Text(
                                text = stringResource(R.string.area_page, selection.page, selection.pageCount),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    // Sprache des Textes (#42): meist erkennt die App sie selbst; bei kurzen Ausschnitten hilft die Wahl.
                    Box {
                        IconButton(onClick = { languageMenuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.ocr_language))
                        }
                        DropdownMenu(expanded = languageMenuOpen, onDismissRequest = { languageMenuOpen = false }) {
                            Text(
                                text = stringResource(R.string.ocr_language),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier
                                    .padding(horizontal = Spacing.m, vertical = Spacing.s)
                                    .semantics { heading() },
                            )
                            (listOf<String?>(null) + TextLanguage.SUPPORTED).forEach { code ->
                                val selected = code == language
                                DropdownMenuItem(
                                    text = { Text(code?.let { languageName(it) } ?: stringResource(R.string.ocr_language_auto)) },
                                    leadingIcon = {
                                        if (selected) Icon(Icons.Filled.Check, contentDescription = null)
                                    },
                                    onClick = {
                                        languageMenuOpen = false
                                        onLanguageChange(code)
                                    },
                                    modifier = Modifier.semantics { this.selected = selected },
                                )
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            val frameModifier = Modifier.weight(1f).fillMaxWidth()
            when {
                failed -> CenteredMessage(text = stringResource(R.string.area_load_error), modifier = frameModifier)
                preview == null -> CenteredMessage(text = "", modifier = frameModifier) { CircularProgressIndicator() }
                else -> CropFrame(
                    preview = preview,
                    frames = selection.frames,
                    active = selection.active,
                    onFrameChange = onFrameChange,
                    onSelectFrame = onSelectFrame,
                    modifier = frameModifier,
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = stringResource(R.string.area_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                // Was im gerade gewählten Rahmen steht.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.area_kind_label),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                    AreaKind.entries.forEach { kind ->
                        FilterChip(
                            selected = selection.frames[selection.active].kind == kind,
                            onClick = { onKindChange(kind) },
                            label = { Text(kindLabel(kind)) },
                            enabled = preview != null,
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(onClick = onAddFrame, enabled = preview != null && selection.frames.size < MAX_FRAMES) {
                        Text(stringResource(R.string.area_add))
                    }
                    TextButton(onClick = onRemoveFrame, enabled = preview != null && selection.frames.size > 1) {
                        Text(stringResource(R.string.area_remove))
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.m, end = Spacing.m, bottom = Spacing.m),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                OutlinedButton(
                    onClick = onWholePage,
                    enabled = preview != null && !selection.isWholePage,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.area_whole_page), textAlign = TextAlign.Center)
                }
                Button(onClick = onConfirm, enabled = preview != null && !failed, modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (selection.page < selection.pageCount) R.string.area_next_page else R.string.area_recognize,
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** Bezeichnung eines Rahmens – mit den festen Begriffen der App. */
@Composable
private fun kindLabel(kind: AreaKind): String = stringResource(
    when (kind) {
        AreaKind.ALL -> R.string.area_kind_all
        AreaKind.TITLE -> R.string.field_title
        AreaKind.INGREDIENTS -> R.string.ingredients
        AreaKind.STEPS -> R.string.instructions
    },
)

/**
 * Foto mit verschiebbaren Rahmen; außerhalb der Rahmen ist das Foto abgedunkelt. Der gewählte Rahmen hat
 * Griffe an den Ecken; ein anderer Rahmen wird durch Antippen oder Ziehen gewählt. Die Bezeichnung steht
 * als Text am Rahmen, und die Linien sind zweifarbig, damit sie auf hellen und dunklen Fotos sichtbar sind.
 */
@Composable
private fun CropFrame(
    preview: Bitmap,
    frames: List<AreaFrame>,
    active: Int,
    onFrameChange: (index: Int, area: CropArea) -> Unit,
    onSelectFrame: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val image = remember(preview) { preview.asImageBitmap() }
    val currentFrames by rememberUpdatedState(frames)
    val currentActive by rememberUpdatedState(active)
    val currentOnFrameChange by rememberUpdatedState(onFrameChange)
    val currentOnSelectFrame by rememberUpdatedState(onSelectFrame)
    val frameColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.onPrimary
    val scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA)
    val labelStyle = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onPrimary)
    val labels = AreaKind.entries.associateWith { kindLabel(it) }
    val showLabels = frames.size > 1 || frames.any { it.kind != AreaKind.ALL }
    val textMeasurer = rememberTextMeasurer()
    val description = stringResource(R.string.area_frame_description)
    val density = LocalDensity.current
    val margin = with(density) { Spacing.l.toPx() }
    // Griffe lassen sich auf 48 dp × 48 dp greifen; kleiner als das wird ein Rahmen nicht.
    val touchRadius = with(density) { 24.dp.toPx() }
    val minSize = with(density) { 48.dp.toPx() }
    val handleRadius = with(density) { 8.dp.toPx() }
    val strokeWidth = with(density) { 2.dp.toPx() }
    val labelPadding = with(density) { Spacing.xs.toPx() }

    BoxWithConstraints(modifier = modifier) {
        val boxWidth = constraints.maxWidth.toFloat()
        val boxHeight = constraints.maxHeight.toFloat()
        val scale = min((boxWidth - 2 * margin) / image.width, (boxHeight - 2 * margin) / image.height).coerceAtLeast(0.01f)
        val shown = Size(image.width * scale, image.height * scale)
        val origin = Offset((boxWidth - shown.width) / 2, (boxHeight - shown.height) / 2)

        fun rectOf(area: CropArea) = Rect(
            left = origin.x + area.left * shown.width,
            top = origin.y + area.top * shown.height,
            right = origin.x + area.right * shown.width,
            bottom = origin.y + area.bottom * shown.height,
        )

        // Kleinster Rahmen an dieser Stelle – so lässt sich auch ein Rahmen innerhalb eines anderen wählen.
        fun frameAt(point: Offset): Int? =
            currentFrames.indices.filter { rectOf(currentFrames[it].area).contains(point) }
                .minByOrNull { currentFrames[it].area.let { area -> (area.right - area.left) * (area.bottom - area.top) } }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = description }
                .pointerInput(image, boxWidth, boxHeight) {
                    detectTapGestures { point -> frameAt(point)?.let { currentOnSelectFrame(it) } }
                }
                .pointerInput(image, boxWidth, boxHeight) {
                    var target = -1
                    var handle: CropHandle? = null
                    detectDragGestures(
                        onDragStart = { start ->
                            val activeHandle = currentFrames.getOrNull(currentActive)?.area?.handleAt(
                                x = (start.x - origin.x) / shown.width,
                                y = (start.y - origin.y) / shown.height,
                                toleranceX = touchRadius / shown.width,
                                toleranceY = touchRadius / shown.height,
                            )
                            if (activeHandle != null) {
                                target = currentActive
                                handle = activeHandle
                            } else {
                                // Ein anderer Rahmen wird gewählt und gleich verschoben.
                                target = frameAt(start) ?: -1
                                handle = if (target >= 0) CropHandle.MOVE else null
                                if (target >= 0) currentOnSelectFrame(target)
                            }
                        },
                        onDragEnd = { handle = null },
                        onDragCancel = { handle = null },
                    ) { change, amount ->
                        val dragging = handle ?: return@detectDragGestures
                        val frame = currentFrames.getOrNull(target) ?: return@detectDragGestures
                        change.consume()
                        currentOnFrameChange(
                            target,
                            frame.area.dragged(
                                handle = dragging,
                                dx = amount.x / shown.width,
                                dy = amount.y / shown.height,
                                minWidth = min(minSize / shown.width, 1f),
                                minHeight = min(minSize / shown.height, 1f),
                            ),
                        )
                    }
                },
        ) {
            drawImage(
                image = image,
                dstOffset = IntOffset(origin.x.roundToInt(), origin.y.roundToInt()),
                dstSize = IntSize(shown.width.roundToInt(), shown.height.roundToInt()),
            )
            // Abdunkeln, was in keinem Rahmen liegt.
            val inside = Path().apply { frames.forEach { addRect(rectOf(it.area)) } }
            clipPath(inside, ClipOp.Difference) { drawRect(scrimColor, origin, shown) }

            frames.forEachIndexed { index, frame ->
                val rect = rectOf(frame.area)
                // Zweifarbig: helle Linie unter der Hauptfarbe, damit der Rahmen auf jedem Foto sichtbar bleibt.
                drawRect(outlineColor, rect.topLeft, rect.size, style = Stroke(strokeWidth * OUTLINE_FACTOR))
                drawRect(frameColor, rect.topLeft, rect.size, style = Stroke(strokeWidth))
                if (showLabels) {
                    val text = textMeasurer.measure(labels.getValue(frame.kind), labelStyle)
                    drawRect(
                        frameColor,
                        rect.topLeft,
                        Size(text.size.width + 2 * labelPadding, text.size.height + labelPadding),
                    )
                    drawText(text, topLeft = Offset(rect.left + labelPadding, rect.top + labelPadding / 2))
                }
                if (index == active) {
                    listOf(rect.topLeft, rect.topRight, rect.bottomLeft, rect.bottomRight).forEach { corner ->
                        drawCircle(outlineColor, handleRadius + strokeWidth, corner)
                        drawCircle(frameColor, handleRadius, corner)
                    }
                }
            }
        }
    }
}

/** Abdunklung außerhalb der Rahmen. */
private const val SCRIM_ALPHA = 0.55f

/** Breite der hellen Linie unter dem Rahmen im Verhältnis zur Rahmenlinie. */
private const val OUTLINE_FACTOR = 2.5f

/** Mehr Rahmen braucht eine Seite kaum; so bleibt die Erkennung auch auf günstigen Handys schnell genug. */
private const val MAX_FRAMES = 6

/** Name einer Sprache in ihr selbst („Italiano“, „Français“), damit jeder seine Sprache findet. */
private fun languageName(code: String): String {
    val locale = Locale.forLanguageTag(code)
    return locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }
}
