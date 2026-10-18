package com.rkgit.one.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.FileProvider
import java.io.File

/** Only http and https links are ever opened. */
fun String.isSafeWebUrl(): Boolean =
    runCatching { Uri.parse(this).scheme?.lowercase() in setOf("http", "https") }.getOrDefault(false)

fun Context.openInCustomTab(url: String) {
    if (!url.isSafeWebUrl()) return
    val uri = Uri.parse(url)
    val opened = runCatching { CustomTabsIntent.Builder().build().launchUrl(this, uri) }.isSuccess ||
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }.isSuccess
    if (!opened) Toast.makeText(this, "No browser found to open this link", Toast.LENGTH_SHORT).show()
}

fun Context.openFileExternally(file: File, mimeType: String) {
    try {
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show()
    }
}

