package com.rkgit.one.ui.util

import android.content.Context
import android.net.ConnectivityManager

fun isActiveNetworkMetered(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    return cm?.isActiveNetworkMetered == true
}
