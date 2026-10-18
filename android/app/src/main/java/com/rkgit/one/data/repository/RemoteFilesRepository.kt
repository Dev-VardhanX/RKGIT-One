package com.rkgit.one.data.repository

import com.rkgit.one.data.downloads.RemoteFileInfo
import com.rkgit.one.data.downloads.downloadKey
import com.rkgit.one.data.local.DownloadedFile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class MaterialRemoteDto(
    val id: String,
    val title: String,
    @SerialName("file_path") val filePath: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("file_type") val fileType: String,
    @SerialName("file_size") val fileSize: Long
)

@Serializable
private data class NoticeRemoteDto(
    val id: String,
    val title: String,
    @SerialName("file_path") val filePath: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("file_type") val fileType: String? = null,
    @SerialName("file_size") val fileSize: Long? = null
)

@Singleton
class RemoteFilesRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun check(rows: List<DownloadedFile>): Map<String, RemoteFileInfo> {
        if (rows.isEmpty()) return emptyMap()

        val materialIds = rows.filter { it.kind == "material" }.map { it.itemId }
        val noticeIds = rows.filter { it.kind == "notice" }.map { it.itemId }

        val result = mutableMapOf<String, RemoteFileInfo>()

        if (materialIds.isNotEmpty()) {
            val materials = supabase.from("study_materials")
                .select(Columns.list("id", "title", "file_path", "file_name", "file_type", "file_size")) {
                    filter { isIn("id", materialIds) }
                }
                .decodeList<MaterialRemoteDto>()

            for (m in materials) {
                result[downloadKey("material", m.id)] = RemoteFileInfo(
                    title = m.title,
                    fileName = m.fileName,
                    fileType = m.fileType,
                    fileSize = m.fileSize,
                    filePath = m.filePath
                )
            }
        }

        if (noticeIds.isNotEmpty()) {
            val notices = supabase.from("notices")
                .select(Columns.list("id", "title", "file_path", "file_name", "file_type", "file_size")) {
                    filter { isIn("id", noticeIds) }
                }
                .decodeList<NoticeRemoteDto>()

            for (n in notices) {
                val path = n.filePath
                val name = n.fileName
                val type = n.fileType
                val size = n.fileSize
                if (path != null && name != null && type != null && size != null) {
                    result[downloadKey("notice", n.id)] = RemoteFileInfo(
                        title = n.title,
                        fileName = name,
                        fileType = type,
                        fileSize = size,
                        filePath = path
                    )
                }
            }
        }

        return result
    }
}
