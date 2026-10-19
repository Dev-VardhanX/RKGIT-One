package com.rkgit.one.ui.components

import androidx.compose.runtime.staticCompositionLocalOf

val LocalOpenDownloads = staticCompositionLocalOf<(() -> Unit)?> { null }
