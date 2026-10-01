package com.moltobene.app.ui.edit

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.moltobene.app.R
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
 * Am Anfang umfasst der Rahmen die ganze Seite; wer nichts ändern will, tippt nur „Text erkennen“.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaSelectionScreen(
    selection: AreaSelection,
    preview: Bitmap?,
    failed: Boolean,
    snackbarHostState: SnackbarHostState,
    language: String?,
    onLanguageChange: (String?) -> Unit,
    onAreaChange: (CropArea) -> Unit,
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
                else -> CropFrame(preview = preview, area = selection.area, onAreaChange = onAreaChange, modifier = frameModifier)
            }
            Text(
                text = stringResource(R.string.area_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.m, vertical = Spacing.s),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.m, end = Spacing.m, bottom = Spacing.m),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                OutlinedButton(
                    onClick = onWholePage,
                    enabled = preview != null && !selection.area.isWholePage,
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

/** Foto mit verschiebbarem Rahmen; außerhalb des Rahmens ist das Foto abgedunkelt. */
@Composable
private fun CropFrame(
    preview: Bitmap,
    area: CropArea,
    onAreaChange: (CropArea) -> Unit,
    modifier: Modifier = Modifier,
) {
    val image = remember(preview) { preview.asImageBitmap() }
    val currentArea by rememberUpdatedState(area)
    val currentOnAreaChange by rememberUpdatedState(onAreaChange)
    val frameColor = MaterialTheme.colorScheme.primary
    val handleOutline = MaterialTheme.colorScheme.onPrimary
    val scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA)
    val description = stringResource(R.string.area_frame_description)
    val density = LocalDensity.current
    val margin = with(density) { Spacing.l.toPx() }
    // Griffe lassen sich auf 48 dp × 48 dp greifen; kleiner als das wird der Rahmen nicht.
    val touchRadius = with(density) { 24.dp.toPx() }
    val minSize = with(density) { 48.dp.toPx() }
    val handleRadius = with(density) { 8.dp.toPx() }
    val strokeWidth = with(density) { 2.dp.toPx() }

    BoxWithConstraints(modifier = modifier) {
        val boxWidth = constraints.maxWidth.toFloat()
        val boxHeight = constraints.maxHeight.toFloat()
        val scale = min((boxWidth - 2 * margin) / image.width, (boxHeight - 2 * margin) / image.height).coerceAtLeast(0.01f)
        val shown = Size(image.width * scale, image.height * scale)
        val origin = Offset((boxWidth - shown.width) / 2, (boxHeight - shown.height) / 2)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = description }
                .pointerInput(image, boxWidth, boxHeight) {
                    var handle: CropHandle? = null
                    detectDragGestures(
                        onDragStart = { start ->
                            handle = currentArea.handleAt(
                                x = (start.x - origin.x) / shown.width,
                                y = (start.y - origin.y) / shown.height,
                                toleranceX = touchRadius / shown.width,
                                toleranceY = touchRadius / shown.height,
                            )
                        },
                        onDragEnd = { handle = null },
                        onDragCancel = { handle = null },
                    ) { change, amount ->
                        val dragging = handle ?: return@detectDragGestures
                        change.consume()
                        currentOnAreaChange(
                            currentArea.dragged(
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
            val left = origin.x + area.left * shown.width
            val top = origin.y + area.top * shown.height
            val right = origin.x + area.right * shown.width
            val bottom = origin.y + area.bottom * shown.height
            val imageRight = origin.x + shown.width
            val imageBottom = origin.y + shown.height
            drawRect(scrimColor, Offset(origin.x, origin.y), Size(shown.width, top - origin.y))
            drawRect(scrimColor, Offset(origin.x, bottom), Size(shown.width, imageBottom - bottom))
            drawRect(scrimColor, Offset(origin.x, top), Size(left - origin.x, bottom - top))
            drawRect(scrimColor, Offset(right, top), Size(imageRight - right, bottom - top))
            drawRect(frameColor, Offset(left, top), Size(right - left, bottom - top), style = Stroke(strokeWidth))
            listOf(Offset(left, top), Offset(right, top), Offset(left, bottom), Offset(right, bottom)).forEach { corner ->
                drawCircle(handleOutline, handleRadius + strokeWidth, corner)
                drawCircle(frameColor, handleRadius, corner)
            }
        }
    }
}

/** Abdunklung außerhalb des Rahmens. */
private const val SCRIM_ALPHA = 0.55f

/** Name einer Sprache in ihr selbst („Italiano“, „Français“), damit jeder seine Sprache findet. */
private fun languageName(code: String): String {
    val locale = Locale.forLanguageTag(code)
    return locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }
}
