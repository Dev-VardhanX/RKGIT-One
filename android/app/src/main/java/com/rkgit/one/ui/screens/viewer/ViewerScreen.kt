package com.rkgit.one.ui.screens.viewer

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import com.rkgit.one.data.downloads.DownloadState
import com.rkgit.one.data.downloads.shouldConfirmMobileData
import com.rkgit.one.ui.components.ScreenScaffold
import com.rkgit.one.ui.util.isActiveNetworkMetered
import com.rkgit.one.ui.util.openFileExternally
import com.rkgit.one.ui.util.toFileSize
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File
import kotlin.math.roundToInt

private val pdfZoomSteps = listOf(1.0f, 1.5f, 2.0f, 3.0f)
private val imageZoomSteps = listOf(1.0f, 2.0f, 3.0f, 4.0f)

@Composable
fun ViewerScreen(
    onBack: () -> Unit,
    viewModel: ViewerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val localFile: File? = when (val s = state) {
        is ViewerState.PdfReady -> s.file
        is ViewerState.ImageReady -> s.file
        is ViewerState.OtherReady -> s.file
        is ViewerState.Error -> s.file
        else -> null
    }

    val isReady = state is ViewerState.PdfReady || state is ViewerState.ImageReady || state is ViewerState.OtherReady

    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showMobileDataDialog by remember { mutableStateOf(false) }
    var notificationPermissionRequested by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Download starts regardless of response
    }

    LaunchedEffect(downloadState) {
        if (downloadState is DownloadState.Failed) {
            Toast.makeText(context, (downloadState as DownloadState.Failed).reason, Toast.LENGTH_SHORT).show()
        }
    }

    val handleDownloadClick: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !notificationPermissionRequested
        ) {
            notificationPermissionRequested = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (shouldConfirmMobileData(viewModel.fileSize, isActiveNetworkMetered(context))) {
            showMobileDataDialog = true
        } else {
            viewModel.startDownload()
        }
    }

    ScreenScaffold(
        title = viewModel.title,
        onBack = onBack,
        actions = {
            if (state is ViewerState.PdfReady) {
                IconButton(onClick = {
                    val prevIdx = pdfZoomSteps.indexOfLast { it < zoom - 0.01f }.coerceAtLeast(0)
                    zoom = pdfZoomSteps[prevIdx]
                }) {
                    Icon(Icons.Outlined.ZoomOut, contentDescription = "Zoom out")
                }
                IconButton(onClick = {
                    val nextIdx = pdfZoomSteps.indexOfFirst { it > zoom + 0.01f }
                        .let { if (it == -1) pdfZoomSteps.lastIndex else it }
                    zoom = pdfZoomSteps[nextIdx]
                }) {
                    Icon(Icons.Outlined.ZoomIn, contentDescription = "Zoom in")
                }
            } else if (state is ViewerState.ImageReady) {
                IconButton(onClick = {
                    val prevIdx = imageZoomSteps.indexOfLast { it < zoom - 0.01f }.coerceAtLeast(0)
                    zoom = imageZoomSteps[prevIdx]
                }) {
                    Icon(Icons.Outlined.ZoomOut, contentDescription = "Zoom out")
                }
                IconButton(onClick = {
                    val nextIdx = imageZoomSteps.indexOfFirst { it > zoom + 0.01f }
                        .let { if (it == -1) imageZoomSteps.lastIndex else it }
                    zoom = imageZoomSteps[nextIdx]
                }) {
                    Icon(Icons.Outlined.ZoomIn, contentDescription = "Zoom in")
                }
            }

            if (isReady) {
                when (val ds = downloadState) {
                    is DownloadState.NotDownloaded, is DownloadState.Failed -> {
                        IconButton(onClick = handleDownloadClick) {
                            Icon(Icons.Outlined.Download, contentDescription = "Download")
                        }
                    }
                    is DownloadState.Downloading -> {
                        IconButton(
                            onClick = viewModel::cancelDownload,
                            modifier = Modifier.semantics { contentDescription = "Cancel download" }
                        ) {
                            val p = ds.progress
                            if (p != null) {
                                CircularProgressIndicator(
                                    progress = { p },
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp
                                )
                            }
                        }
                    }
                    is DownloadState.Downloaded -> {
                        IconButton(onClick = { showRemoveDialog = true }) {
                            Icon(Icons.Outlined.DownloadDone, contentDescription = "Downloaded. Tap to remove")
                        }
                    }
                    is DownloadState.UpdateAvailable -> {
                        IconButton(onClick = handleDownloadClick) {
                            Icon(Icons.Outlined.Update, contentDescription = "Update download")
                        }
                    }
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
                is ViewerState.ImageReady -> ImageViewer(
                    file = s.file,
                    fileType = viewModel.fileType,
                    zoom = zoom,
                    onZoomChange = { zoom = it },
                    onRedownload = viewModel::redownload
                )
            }
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove download?") },
            text = { Text("You will need an internet connection to open this file again.") },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveDialog = false
                    viewModel.removeDownload()
                }) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showMobileDataDialog) {
        AlertDialog(
            onDismissRequest = { showMobileDataDialog = false },
            title = { Text("Download on mobile data?") },
            text = { Text("This file is ${viewModel.fileSize.toFileSize()} and you're not on Wi-Fi.") },
            confirmButton = {
                TextButton(onClick = {
                    showMobileDataDialog = false
                    viewModel.startDownload()
                }) {
                    Text("Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMobileDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ImageViewer(
    file: File,
    fileType: String,
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    onRedownload: () -> Unit
) {
    val context = LocalContext.current
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
    val currentZoom by rememberUpdatedState(zoom)

    fun clampOffset(proposed: Offset, targetZoom: Float): Offset {
        if (targetZoom <= 1.01f) return Offset.Zero
        val maxX = (containerSize.width * (targetZoom - 1f) / 2f).coerceAtLeast(0f)
        val maxY = (containerSize.height * (targetZoom - 1f) / 2f).coerceAtLeast(0f)
        return Offset(
            x = proposed.x.coerceIn(-maxX, maxX),
            y = proposed.y.coerceIn(-maxY, maxY)
        )
    }

    LaunchedEffect(zoom) {
        if (zoom <= 1.01f) {
            offset = Offset.Zero
        } else {
            offset = clampOffset(offset, zoom)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    val newZoom = (currentZoom * gestureZoom).coerceIn(1f, 4f)
                    onZoomChange(newZoom)
                    offset = clampOffset(offset + pan, newZoom)
                }
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(file)
                .size(2048)
                .build(),
            contentDescription = "Attachment image",
            contentScale = ContentScale.Fit,
            onState = { imageState = it },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = zoom,
                    scaleY = zoom,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )

        when (imageState) {
            is AsyncImagePainter.State.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is AsyncImagePainter.State.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "This image couldn't be displayed. It may be damaged.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = onRedownload) {
                        Text("Re-download")
                    }
                    Button(onClick = { context.openFileExternally(file, fileType) }) {
                        Text("Open in another app")
                    }
                }
            }
            else -> {
                // Success or Empty
            }
        }

        if (zoom > 1.01f) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
                tonalElevation = 2.dp
            ) {
                Text(
                    text = "${(zoom * 100).roundToInt()}%",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface
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
