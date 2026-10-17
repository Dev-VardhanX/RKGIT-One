package com.rkgit.one.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent

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
