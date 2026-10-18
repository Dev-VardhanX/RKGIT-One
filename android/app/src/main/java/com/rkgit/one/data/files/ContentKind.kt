package com.rkgit.one.data.files

enum class ContentKind {
    PDF,
    IMAGE,
    OTHER
}

fun contentKindOf(fileType: String): ContentKind = when {
    fileType.equals("application/pdf", ignoreCase = true) -> ContentKind.PDF
    fileType.startsWith("image/", ignoreCase = true) -> ContentKind.IMAGE
    else -> ContentKind.OTHER
}
