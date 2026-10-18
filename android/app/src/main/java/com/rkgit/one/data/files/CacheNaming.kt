package com.rkgit.one.data.files

import java.util.UUID

/**
 * Generates a deterministic cache file name from an item ID and its remote path.
 * A different remotePath always produces a different name.
 */
fun cacheFileName(itemId: String, remotePath: String, fileName: String): String {
    val safeId = itemId.replace(Regex("[^A-Za-z0-9-]"), "")
    val uuid = UUID.nameUUIDFromBytes(remotePath.toByteArray()).toString()
    val rawExt = fileName.substringAfterLast('.', "")
        .lowercase()
        .replace(Regex("[^a-z0-9]"), "")
        .take(6)
    val ext = rawExt.ifEmpty { "bin" }
    return "${safeId}_${uuid}.$ext"
}

data class CachedFileInfo(
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long
)

/**
 * Returns the files to delete so that the remaining total is within [maxTotalBytes].
 * Deletes oldest-modified first.
 */
fun filesToTrim(files: List<CachedFileInfo>, maxTotalBytes: Long): List<CachedFileInfo> {
    val total = files.sumOf { it.sizeBytes }
    if (total <= maxTotalBytes) return emptyList()

    val sorted = files.sortedBy { it.lastModified }
    val toDelete = mutableListOf<CachedFileInfo>()
    var remaining = total
    for (file in sorted) {
        if (remaining <= maxTotalBytes) break
        toDelete.add(file)
        remaining -= file.sizeBytes
    }
    return toDelete
}

/**
 * Returns download progress as a fraction, or null if total is unknown.
 */
fun downloadProgress(readBytes: Long, totalBytes: Long): Float? =
    if (totalBytes <= 0) null
    else (readBytes.toFloat() / totalBytes).coerceIn(0f, 1f)

fun isCompleteDownload(expectedBytes: Long, actualBytes: Long): Boolean =
    expectedBytes <= 0 || expectedBytes == actualBytes

fun resumeKey(kind: String, itemId: String): String = "$kind:$itemId"
