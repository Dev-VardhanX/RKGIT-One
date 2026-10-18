package com.rkgit.one.ui.screens.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rkgit.one.data.files.ContentKind
import com.rkgit.one.data.files.DownloadException
import com.rkgit.one.data.files.FileCache
import com.rkgit.one.data.files.FileKind
import com.rkgit.one.data.files.ViewerPrefs
import com.rkgit.one.data.files.contentKindOf
import com.rkgit.one.data.files.resumeKey
import com.rkgit.one.ui.navigation.ViewerRoute
import com.rkgit.one.ui.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface ViewerState {
    data class Loading(val progress: Float?) : ViewerState
    data class PdfReady(val file: File, val pager: PdfPager) : ViewerState
    data class ImageReady(val file: File) : ViewerState
    data class OtherReady(val file: File) : ViewerState
    data class Error(val message: String, val file: File?) : ViewerState
}

@HiltViewModel
class ViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fileCache: FileCache,
    private val prefs: ViewerPrefs
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ViewerRoute>()
    val title: String = route.title
    val fileName: String = route.fileName
    val fileType: String = route.fileType
    val kindValue: String = route.kind
    val itemId: String = route.itemId
    val contentKind: ContentKind = contentKindOf(fileType)
    val isPdf: Boolean = contentKind == ContentKind.PDF
    val resumeKey: String = resumeKey(kindValue, itemId)

    private val _state = MutableStateFlow<ViewerState>(ViewerState.Loading(null))
    val state: StateFlow<ViewerState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var currentPager: PdfPager? = null

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = ViewerState.Loading(null)
            try {
                val file = fileCache.ensureLocal(
                    kind = FileKind.fromValue(kindValue),
                    itemId = itemId,
                    remotePath = route.remotePath,
                    fileName = fileName,
                    expectedSize = route.fileSize,
                    onProgress = { _state.value = ViewerState.Loading(it) }
                )

                when (contentKind) {
                    ContentKind.PDF -> {
                        try {
                            val pager = kotlinx.coroutines.withContext(Dispatchers.IO) {
                                PdfPager(file)
                            }
                            if (pager.pageCount == 0) {
                                pager.close()
                                _state.value = ViewerState.Error("This PDF has no pages.", file)
                            } else {
                                currentPager = pager
                                _state.value = ViewerState.PdfReady(file, pager)
                            }
                        } catch (_: SecurityException) {
                            _state.value = ViewerState.Error(
                                "This PDF is password protected and can't be shown here.", file
                            )
                        } catch (_: java.io.IOException) {
                            _state.value = ViewerState.Error(
                                "This file couldn't be opened. It may be damaged.", file
                            )
                        } catch (_: IllegalArgumentException) {
                            _state.value = ViewerState.Error(
                                "This file couldn't be opened. It may be damaged.", file
                            )
                        } catch (_: IllegalStateException) {
                            _state.value = ViewerState.Error(
                                "This file couldn't be opened. It may be damaged.", file
                            )
                        }
                    }
                    ContentKind.IMAGE -> {
                        _state.value = ViewerState.ImageReady(file)
                    }
                    ContentKind.OTHER -> {
                        _state.value = ViewerState.OtherReady(file)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: DownloadException) {
                _state.value = ViewerState.Error(e.message ?: "Download failed.", null)
            } catch (e: Exception) {
                _state.value = ViewerState.Error(e.toUserMessage(), null)
            }
        }
    }

    fun redownload() {
        val currentFile = when (val s = _state.value) {
            is ViewerState.PdfReady -> s.file
            is ViewerState.ImageReady -> s.file
            is ViewerState.OtherReady -> s.file
            is ViewerState.Error -> s.file
            else -> null
        }
        currentFile?.let { fileCache.delete(it) }
        load()
    }

    fun savedPage(): Int = prefs.getPage(resumeKey)

    fun savePage(p: Int) = prefs.setPage(resumeKey, p)

    override fun onCleared() {
        super.onCleared()
        currentPager?.let { pager ->
            CoroutineScope(Dispatchers.IO).launch { pager.close() }
        }
    }

    init {
        load()
    }
}
