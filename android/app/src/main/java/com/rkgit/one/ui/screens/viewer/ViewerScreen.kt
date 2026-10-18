package com.rkgit.one.ui.screens.viewer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rkgit.one.ui.components.ScreenScaffold
import com.rkgit.one.ui.util.openFileExternally
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File
import kotlin.math.roundToInt

private val zoomSteps = listOf(1.0f, 1.5f, 2.0f, 3.0f)

@Composable
fun ViewerScreen(
    onBack: () -> Unit,
    viewModel: ViewerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val localFile: File? = when (val s = state) {
        is ViewerState.PdfReady -> s.file
        is ViewerState.OtherReady -> s.file
        is ViewerState.Error -> s.file
        else -> null
    }

    var zoom by rememberSaveable { mutableFloatStateOf(1f) }

    ScreenScaffold(
        title = viewModel.title,
        onBack = onBack,
        actions = {
            if (state is ViewerState.PdfReady) {
                IconButton(onClick = {
                    val prevIdx = zoomSteps.indexOfLast { it < zoom - 0.01f }.coerceAtLeast(0)
                    zoom = zoomSteps[prevIdx]
                }) {
                    Icon(Icons.Outlined.ZoomOut, contentDescription = "Zoom out")
                }
                IconButton(onClick = {
                    val nextIdx = zoomSteps.indexOfFirst { it > zoom + 0.01f }
                        .let { if (it == -1) zoomSteps.lastIndex else it }
                    zoom = zoomSteps[nextIdx]
                }) {
                    Icon(Icons.Outlined.ZoomIn, contentDescription = "Zoom in")
                }
            }
            if (localFile != null) {
                IconButton(onClick = {
                    context.openFileExternally(localFile, viewModel.fileType)
                }) {
                    Icon(
                        Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = "Open in another app"
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                is ViewerState.Loading -> LoadingContent(s.progress)
                is ViewerState.Error -> ErrorContent(
                    message = s.message,
                    file = s.file,
                    fileType = viewModel.fileType,
                    onRetry = viewModel::load,
                    onRedownload = viewModel::redownload
                )
                is ViewerState.OtherReady -> OtherReadyContent(
                    fileName = viewModel.fileName,
                    file = s.file,
                    fileType = viewModel.fileType
                )
                is ViewerState.PdfReady -> PdfContent(
                    pager = s.pager,
                    zoom = zoom,
                    onZoomChange = { zoom = it },
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(progress: Float?) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(0.6f)
            )
            Text(
                "Downloading… ${(progress * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.6f))
            Text(
                "Preparing…",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    file: File?,
    fileType: String,
    onRetry: () -> Unit,
    onRedownload: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Button(onClick = onRetry) { Text("Retry") }
        if (file != null) {
            Button(onClick = onRedownload) { Text("Re-download") }
            Button(onClick = { context.openFileExternally(file, fileType) }) {
                Text("Open in another app")
            }
        }
    }
}

@Composable
private fun OtherReadyContent(fileName: String, file: File, fileType: String) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            fileName,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            "This file type can't be previewed in the app.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Button(onClick = { context.openFileExternally(file, fileType) }) {
            Text("Open in another app")
        }
    }
}

@Composable
private fun PdfContent(
    pager: PdfPager,
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    viewModel: ViewerViewModel
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val viewportWidthDp = configuration.screenWidthDp.dp
    val viewportWidthPx = with(density) { viewportWidthDp.toPx() }.toInt()
    val renderWidthPx = (viewportWidthPx * 1.5f).toInt().coerceIn(720, 2048)
    val scrollState = rememberScrollState()

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.savedPage().coerceIn(0, pager.pageCount - 1)
    )

    // Persist current page
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { viewModel.savePage(it) }
    }

    val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val pageIndicator = buildString {
        append("Page ${currentPage + 1} / ${pager.pageCount}")
        if (zoom > 1.01f) {
            append(" · ${(zoom * 100).roundToInt()}%")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.count { it.pressed } >= 2) {
                            val newZoom =
                                (zoom * event.calculateZoom()).coerceIn(1f, 3f)
                            onZoomChange(newZoom)
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        val scaledWidth = viewportWidthDp * zoom

        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.width(scaledWidth),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pager.pageCount, key = { it }) { index ->
                    PdfPage(
                        pager = pager,
                        index = index,
                        renderWidthPx = renderWidthPx
                    )
                }
            }
        }

        // Page indicator
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
            tonalElevation = 2.dp
        ) {
            Text(
                text = pageIndicator,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface
            )
        }
    }
}

@Composable
private fun PdfPage(
    pager: PdfPager,
    index: Int,
    renderWidthPx: Int
) {
    var bitmap by remember(index) { mutableStateOf(pager.cached(index, renderWidthPx)) }

    LaunchedEffect(index, renderWidthPx) {
        if (bitmap == null) {
            bitmap = pager.render(index, renderWidthPx)
        }
    }

    val aspect = bitmap?.let { it.width.toFloat() / it.height.toFloat() }
        ?: pager.firstPageAspect

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .aspectRatio(aspect),
        contentAlignment = Alignment.Center
    ) {
        val bmp: Bitmap? = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Page ${index + 1}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            CircularProgressIndicator()
        }
    }
}
