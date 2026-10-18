package com.rkgit.one.data.downloads

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.rkgit.one.data.files.FileKind
import com.rkgit.one.data.files.isCompleteDownload
import com.rkgit.one.data.local.DownloadDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dao: DownloadDao
) {
    private val workManager by lazy { WorkManager.getInstance(context) }

    fun enqueue(
        kind: FileKind,
        itemId: String,
        title: String,
        subtitle: String,
        year: Int?,
        semester: Int?,
        fileName: String,
        fileType: String,
        sizeBytes: Long,
        remotePath: String,
        isUpdate: Boolean = false
    ): OneTimeWorkRequest {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = workDataOf(
            "kind" to kind.value,
            "itemId" to itemId,
            "title" to title,
            "subtitle" to subtitle,
            "year" to (year ?: -1),
            "semester" to (semester ?: -1),
            "fileName" to fileName,
            "fileType" to fileType,
            "sizeBytes" to sizeBytes,
            "remotePath" to remotePath
        )

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(inputData)
            .addTag("download")
            .build()

        val policy = if (isUpdate) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
        workManager.enqueueUniqueWork(uniqueWorkName(kind.value, itemId), policy, request)
        return request
    }

    fun cancel(kind: FileKind, itemId: String) {
        workManager.cancelUniqueWork(uniqueWorkName(kind.value, itemId))
        val safeId = itemId.replace(Regex("[^A-Za-z0-9-]"), "")
        val dir = File(context.filesDir, "downloads/${kind.value}")
        if (dir.exists()) {
            dir.listFiles()
                ?.filter { it.isFile && it.name.startsWith("${safeId}_") && it.name.endsWith(".part") }
                ?.forEach { it.delete() }
        }
    }

    suspend fun remove(kind: FileKind, itemId: String) {
        val row = dao.get(kind.value, itemId)
        if (row != null) {
            val file = File(row.localPath)
            if (file.exists()) {
                file.delete()
            }
            dao.delete(kind.value, itemId)
        }
    }

    fun observeState(
        kind: FileKind,
        itemId: String,
        currentRemotePath: String?
    ): Flow<DownloadState> {
        val workFlow = workManager.getWorkInfosForUniqueWorkFlow(uniqueWorkName(kind.value, itemId))
        val daoFlow = dao.observe(kind.value, itemId)

        return combine(daoFlow, workFlow) { row, workInfos ->
            val active = workInfos.firstOrNull { it.state == WorkInfo.State.RUNNING }
                ?: workInfos.firstOrNull { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED }
                ?: workInfos.lastOrNull()

            val snapshot = active?.let { info ->
                val (status, progress) = when (info.state) {
                    WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> WorkStatus.ENQUEUED to null
                    WorkInfo.State.RUNNING -> {
                        val p = info.progress.getFloat("progress", -1f)
                        WorkStatus.RUNNING to (if (p in 0f..1f) p else null)
                    }
                    WorkInfo.State.SUCCEEDED -> WorkStatus.SUCCEEDED to null
                    WorkInfo.State.FAILED -> WorkStatus.FAILED to null
                    WorkInfo.State.CANCELLED -> WorkStatus.CANCELLED to null
                }
                val error = info.outputData.getString("error")
                WorkSnapshot(status, progress, error)
            }

            val downloadRow = row?.let { DownloadRow(it.remoteFilePath, it.localPath) }
            deriveDownloadState(downloadRow, currentRemotePath, snapshot)
        }
    }

    fun observeAllKeys(): Flow<Set<String>> =
        dao.observeAll().map { rows -> rows.map { downloadKey(it.kind, it.itemId) }.toSet() }

    suspend fun downloadedFileFor(kind: FileKind, itemId: String, remotePath: String): File? {
        val row = dao.get(kind.value, itemId) ?: return null
        if (row.remoteFilePath != remotePath) {
            return null
        }
        val file = File(row.localPath)
        return if (file.exists() && isCompleteDownload(row.sizeBytes, file.length())) {
            file
        } else {
            dao.delete(kind.value, itemId)
            if (file.exists()) {
                file.delete()
            }
            null
        }
    }
}
