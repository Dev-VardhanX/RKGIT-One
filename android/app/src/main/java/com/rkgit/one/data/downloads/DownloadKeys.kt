package com.rkgit.one.data.downloads

fun downloadKey(kind: String, itemId: String) = "$kind:$itemId"

fun uniqueWorkName(kind: String, itemId: String) = "download:$kind:$itemId"

const val MOBILE_DATA_WARNING_BYTES = 10L * 1024 * 1024

fun shouldConfirmMobileData(
    sizeBytes: Long,
    isMetered: Boolean,
    thresholdBytes: Long = MOBILE_DATA_WARNING_BYTES
) = isMetered && sizeBytes >= thresholdBytes
