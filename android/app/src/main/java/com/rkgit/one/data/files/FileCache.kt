package com.rkgit.one.data.files

import android.content.Context
import com.rkgit.one.data.remote.PublicUrls
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

class DownloadException(message: String) : IOException(message)

@Singleton
class FileCache @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val root: File get() = File(context.cacheDir, "files")

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
        val partFile = File(dir, "$name.part")

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

        // Download
        val url = URL(PublicUrls.file(kind.bucket, remotePath))
        var connection: HttpURLConnection? = null
        try {
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.connect()

            val code = connection.responseCode
            if (code == 404) {
                throw DownloadException("This file is no longer available.")
            }
            if (code != 200) {
                throw DownloadException("Couldn't download the file. Please try again.")
            }

            val contentLength = connection.contentLengthLong
            val totalForProgress = if (contentLength > 0) contentLength else expectedSize

            var bytesWritten = 0L
            connection.inputStream.use { input ->
                partFile.outputStream().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        bytesWritten += read
                        onProgress(downloadProgress(bytesWritten, totalForProgress))
                    }
                }
            }

            if (!isCompleteDownload(expectedSize, bytesWritten)) {
                partFile.delete()
                throw DownloadException("The download was incomplete. Please try again.")
            }

            if (!partFile.renameTo(target)) {
                // Fallback: copy and delete
                partFile.copyTo(target, overwrite = true)
                partFile.delete()
            }

            target
        } catch (e: Exception) {
            partFile.delete()
            throw e
        } finally {
            connection?.disconnect()
        }
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
