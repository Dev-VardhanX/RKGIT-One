package com.rkgit.one.data.files

enum class FileKind(val value: String, val bucket: String) {
    MATERIAL("material", "study-materials"),
    NOTICE("notice", "notice-files");

    companion object {
        fun fromValue(v: String): FileKind =
            entries.firstOrNull { it.value == v } ?: MATERIAL
    }
}
