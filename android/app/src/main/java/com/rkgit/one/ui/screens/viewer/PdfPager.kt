package com.rkgit.one.ui.screens.viewer

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class PdfPager(file: File) {
    private val pfd: ParcelFileDescriptor =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer: PdfRenderer = PdfRenderer(pfd)
    private var closed = false
    private val mutex = Mutex()

    val pageCount: Int = renderer.pageCount

    val firstPageAspect: Float = if (pageCount > 0) {
        renderer.openPage(0).use { page ->
            page.width.toFloat() / page.height.toFloat()
        }
    } else {
        1f
    }

    private val cache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 6).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    fun cached(index: Int, widthPx: Int): Bitmap? = cache.get("$index@$widthPx")

    suspend fun render(index: Int, widthPx: Int): Bitmap? = mutex.withLock {
        if (closed) return@withLock null
        withContext(Dispatchers.IO) {
            val key = "$index@$widthPx"
            cache.get(key)?.let { return@withContext it }

            renderer.openPage(index).use { page ->
                val height = (widthPx * page.height / page.width.toFloat())
                    .toInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                cache.put(key, bmp)
                bmp
            }
        }
    }

    suspend fun close() = mutex.withLock {
        if (closed) return@withLock
        closed = true
        withContext(Dispatchers.IO) {
            renderer.close()
            pfd.close()
        }
        cache.evictAll()
    }
}
