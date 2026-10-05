package com.moltobene.app.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import com.moltobene.app.R
import com.moltobene.app.ui.theme.Spacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Zustand der Seitenansicht (#38) für ein ViewModel: welche Seite offen ist und ihr Bild.
 * Es wird immer nur die sichtbare Seite geladen.
 */
class PageViewerModel(
    private val scope: CoroutineScope,
    private val load: suspend (index: Int) -> Bitmap,
) {
    /** Offene Seite (ab 0); null, wenn die Ansicht geschlossen ist. */
    var page by mutableStateOf<Int?>(null)
        private set
    var bitmap by mutableStateOf<Bitmap?>(null)
        private set
    var failed by mutableStateOf(false)
        private set
    private var job: Job? = null

    fun open(index: Int) {
        job?.cancel()
        page = index
        bitmap = null
        failed = false
        job = scope.launch {
            try {
                bitmap = load(index)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed = true
            }
        }
    }

    fun close() {
        job?.cancel()
        page = null
        bitmap = null
        failed = false
    }
}

/**
 * Seiten als Vollbild (#38) zum Prüfen und Nachlesen – oder das Foto eines Rezepts ganz, ohne Zuschnitt.
 * Vergrößern geht mit zwei Fingern, doppeltem Tippen oder den Schaltflächen – die Ansicht ist also auch
 * ohne Gesten und mit Screenreader bedienbar. Bei nur einer Seite gibt es kein Blättern.
 * [actions] sind zusätzliche Schaltflächen für die offene Seite.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PageViewer(
    page: Int,
    pageCount: Int,
    bitmap: Bitmap?,
    failed: Boolean,
    onPageChange: (Int) -> Unit,
    onClose: () -> Unit,
    title: String = stringResource(R.string.area_page, page + 1, pageCount),
    errorText: String = stringResource(R.string.page_load_error),
    actions: @Composable () -> Unit = {},
) {
    BackHandler(onBack = onClose)
    var scale by remember(page) { mutableFloatStateOf(1f) }
    var offset by remember(page) { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    // Verschieben nur so weit, dass die vergrößerte Seite den Rand nicht verlässt.
    fun clamp(value: Offset, zoom: Float): Offset {
        val maxX = size.width * (zoom - 1f) / 2f
        val maxY = size.height * (zoom - 1f) / 2f
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }

    fun zoomTo(zoom: Float) {
        scale = zoom.coerceIn(1f, MAX_ZOOM)
        offset = clamp(offset, scale)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = Spacing.s, vertical = Spacing.xs)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth(),
                    ) { actions() }
                    val paged = pageCount > 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (paged) Arrangement.SpaceBetween else Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (paged) {
                            IconButton(onClick = { onPageChange(page - 1) }, enabled = page > 0) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.page_previous))
                            }
                        }
                        Row {
                            IconButton(onClick = { zoomTo(scale / ZOOM_STEP) }, enabled = bitmap != null && scale > 1f) {
                                Icon(painterResource(R.drawable.ic_zoom_out), contentDescription = stringResource(R.string.zoom_out))
                            }
                            IconButton(onClick = { zoomTo(scale * ZOOM_STEP) }, enabled = bitmap != null && scale < MAX_ZOOM) {
                                Icon(painterResource(R.drawable.ic_zoom_in), contentDescription = stringResource(R.string.zoom_in))
                            }
                        }
                        if (paged) {
                            IconButton(onClick = { onPageChange(page + 1) }, enabled = page < pageCount - 1) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.page_next))
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        val frame = Modifier.padding(padding).fillMaxSize()
        when {
            bitmap != null -> {
                val image = remember(bitmap) { bitmap.asImageBitmap() }
                Box(
                    modifier = frame
                        .clipToBounds()
                        .onSizeChanged { size = it }
                        .pointerInput(page) {
                            detectTapGestures(onDoubleTap = { zoomTo(if (scale > 1f) 1f else DOUBLE_TAP_ZOOM) })
                        }
                        .pointerInput(page) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
                                offset = clamp(offset + pan, scale)
                            }
                        },
                ) {
                    Image(
                        bitmap = image,
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            },
                    )
                }
            }
            failed -> CenteredMessage(text = errorText, modifier = frame)
            else -> CenteredMessage(text = "", modifier = frame) { CircularProgressIndicator() }
        }
    }
}

private const val MAX_ZOOM = 5f
private const val ZOOM_STEP = 1.5f
private const val DOUBLE_TAP_ZOOM = 2.5f
