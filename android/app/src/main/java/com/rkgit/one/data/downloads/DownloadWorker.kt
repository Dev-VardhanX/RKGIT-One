package com.rkgit.one.data.downloads

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.rkgit.one.data.files.DownloadException
import com.rkgit.one.data.files.Downloader
import com.rkgit.one.data.files.FileCache
import com.rkgit.one.data.files.FileKind
import com.rkgit.one.data.files.cacheFileName
import com.rkgit.one.data.local.DownloadDao
import com.rkgit.one.data.local.DownloadedFile
import com.rkgit.one.data.remote.PublicUrls
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.io.File
import java.io.IOException

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val downloader: Downloader,
    private val fileCache: FileCache,
    private val dao: DownloadDao
) : CoroutineWorker(context, params) {

    private val channelId = "downloads"

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val title = inputData.getString("title") ?: "File"
        return createForegroundInfo(title, null)
    }

    private fun createForegroundInfo(title: String, progress: Float?): ForegroundInfo {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "File downloads"
                setSound(null, null)
            }
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        val kind = inputData.getString("kind") ?: ""
        val itemId = inputData.getString("itemId") ?: ""
        val notifId = (uniqueWorkName(kind, itemId).hashCode() and 0x7FFFFFFF) % 100000 + 1000

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Downloading")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (progress != null) {
            val pInt = (progress * 100).toInt().coerceIn(0, 100)
            builder.setProgress(100, pInt, false)
        } else {
            builder.setProgress(0, 0, true)
        }

        val notification = builder.build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notifId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notifId, notification)
        }
    }

    override suspend fun doWork(): Result {
        val kind = inputData.getString("kind") ?: return Result.failure(workDataOf("error" to "Missing kind"))
        val itemId = inputData.getString("itemId") ?: return Result.failure(workDataOf("error" to "Missing itemId"))
        val title = inputData.getString("title") ?: "File"
        val subtitle = inputData.getString("subtitle") ?: ""
        val year = inputData.getInt("year", -1)
        val semester = inputData.getInt("semester", -1)
        val fileName = inputData.getString("fileName") ?: "file"
        val fileType = inputData.getString("fileType") ?: ""
        val sizeBytes = inputData.getLong("sizeBytes", 0L)
        val remotePath = inputData.getString("remotePath") ?: return Result.failure(workDataOf("error" to "Missing remotePath"))

        val targetDir = File(applicationContext.filesDir, "downloads/$kind").also { it.mkdirs() }
        val target = File(targetDir, cacheFileName(itemId, remotePath, fileName))

        try {
            setForeground(createForegroundInfo(title, null))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Ignore foreground setup failures if permission/background restricted
        }

        try {
            val fileKind = FileKind.fromValue(kind)
            val cachedFile = fileCache.cachedFileOrNull(fileKind, itemId, remotePath, fileName, sizeBytes)
            if (cachedFile != null) {
                // Move complete cached copy to target
                val moved = cachedFile.renameTo(target)
                if (!moved) {
                    cachedFile.copyTo(target, overwrite = true)
                    cachedFile.delete()
                }
                setProgress(workDataOf("progress" to 1f))
            } else {
                val freeSpace = applicationContext.filesDir.usableSpace
                val needed = sizeBytes + 10L * 1024 * 1024
                if (freeSpace < needed) {
                    return Result.failure(workDataOf("error" to "Not enough free storage space."))
                }

                var lastProgressTime = 0L
                val url = PublicUrls.file(fileKind.bucket, remotePath)
                downloader.downloadTo(target, url, sizeBytes) { p ->
                    val now = System.currentTimeMillis()
                    if (now - lastProgressTime >= 250L || p == 1f) {
                        lastProgressTime = now
                        setProgressAsync(if (p != null) workDataOf("progress" to p) else workDataOf())
                        try {
                            setForegroundAsync(createForegroundInfo(title, p))
                        } catch (_: Exception) { }
                    }
                }
            }

            // Step 4: After complete, read old row, upsert new row, then delete old file if localPath differs
            val oldRow = dao.get(kind, itemId)
            val newRow = DownloadedFile(
                kind = kind,
                itemId = itemId,
                title = title,
                subtitle = subtitle,
                year = if (year == -1) null else year,
                semester = if (semester == -1) null else semester,
                fileName = fileName,
                fileType = fileType,
                sizeBytes = if (sizeBytes > 0) sizeBytes else target.length(),
                remoteFilePath = remotePath,
                localPath = target.absolutePath,
                downloadedAt = System.currentTimeMillis()
            )
            dao.upsert(newRow)

            if (oldRow != null && oldRow.localPath != target.absolutePath) {
                val oldFile = File(oldRow.localPath)
                if (oldFile.exists()) {
                    oldFile.delete()
                }
            }

            return Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: DownloadException) {
            return Result.failure(workDataOf("error" to (e.message ?: "Download failed")))
        } catch (e: IOException) {
            return if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure(workDataOf("error" to "Couldn't download the file. Please try again."))
            }
        } catch (e: Exception) {
            return Result.failure(workDataOf("error" to (e.message ?: "Download failed")))
        }
    }
}
