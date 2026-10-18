package com.rkgit.one.data.files

import android.content.Context
import com.rkgit.one.data.remote.PublicUrls
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

class DownloadException(message: String) : IOException(message)

@Singleton
class FileCache @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloader: Downloader
) {
    private val root: File get() = File(context.cacheDir, "files")

    fun cachedFileOrNull(
        kind: FileKind,
        itemId: String,
        remotePath: String,
        fileName: String,
        expectedSize: Long
    ): File? {
        val dir = File(root, kind.value)
        val name = cacheFileName(itemId, remotePath, fileName)
        val target = File(dir, name)
        return if (target.exists() && isCompleteDownload(expectedSize, target.length())) {
            target
        } else {
            null
        }
    }

    /**
     * Returns a local file for the given item, downloading it if necessary.
     */
    suspend fun ensureLocal(
        kind: FileKind,
        itemId: String,
        remotePath: String,
        fileName: String,
        expectedSize: Long,
        onProgress: (Float?) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val dir = File(root, kind.value).also { it.mkdirs() }
        val name = cacheFileName(itemId, remotePath, fileName)
        val target = File(dir, name)

        // Already cached and complete
        if (target.exists() && isCompleteDownload(expectedSize, target.length())) {
            target.setLastModified(System.currentTimeMillis())
            return@withContext target
        }

        // Exists but incomplete
        if (target.exists()) target.delete()

        // Free space check
        val freeSpace = context.cacheDir.usableSpace
        val needed = expectedSize + 10L * 1024 * 1024
        if (freeSpace < needed) {
            throw DownloadException("Not enough free storage space.")
        }

        downloader.downloadTo(
            target = target,
            url = PublicUrls.file(kind.bucket, remotePath),
            expectedSize = expectedSize,
            onProgress = onProgress
        )

        target
    }.also {
        // After a successful download, trim the cache
        trimCache(100L * 1024 * 1024)
    }

    fun delete(file: File) {
        file.delete()
    }

    private fun trimCache(maxTotalBytes: Long) {
        if (!root.exists()) return
        val allFiles = root.walkTopDown()
            .filter { it.isFile && !it.name.endsWith(".part") }
            .map { CachedFileInfo(it.absolutePath, it.length(), it.lastModified()) }
            .toList()
        val toDelete = filesToTrim(allFiles, maxTotalBytes)
        for (info in toDelete) {
            File(info.name).delete()
        }
    }

    /**
     * Delete leftover .part files. Called at app start.
     */
    fun cleanPartFiles() {
        if (!root.exists()) return
        root.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".part") }
            .forEach { it.delete() }
    }
}
