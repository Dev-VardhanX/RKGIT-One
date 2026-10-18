package com.rkgit.one.data.files

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Downloader @Inject constructor() {
    suspend fun downloadTo(
        target: File,
        url: String,
        expectedSize: Long,
        onProgress: (Float?) -> Unit
    ) = withContext(Dispatchers.IO) {
        val partFile = File(target.parentFile, "${target.name}.part")
        var connection: HttpURLConnection? = null
        try {
            val u = URL(url)
            connection = u.openConnection() as HttpURLConnection
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
                partFile.copyTo(target, overwrite = true)
                partFile.delete()
            }
        } catch (e: Exception) {
            partFile.delete()
            throw e
        } finally {
            connection?.disconnect()
        }
    }
}
