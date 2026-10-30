package com.rkgit.one.push

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rkgit.one.RkgitApplication
import com.rkgit.one.data.push.NotificationSettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationPermissionEntryPoint {
    fun notificationSettingsRepository(): NotificationSettingsRepository
}

class NotificationPermissionController internal constructor(
    private val context: Context,
    private val activity: Activity?,
    private val repository: NotificationSettingsRepository?,
    private val requestLauncher: () -> Unit,
    initialState: PermissionState
) {
    var state: PermissionState by mutableStateOf(initialState)
        internal set

    fun refresh() {
        val askedBefore = repository?.permissionRequested?.value ?: false
        state = computeState(context, activity, askedBefore)
    }

    fun request() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestLauncher()
        }
    }

    fun openSystemSettings() {
        try {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(fallback)
            } catch (_: Exception) {}
        }
    }
}

private fun computeState(
    context: Context,
    activity: Activity?,
    askedBefore: Boolean
): PermissionState {
    val sdkInt = Build.VERSION.SDK_INT
    val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val permissionGranted = if (sdkInt >= 33) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    val shouldShowRationale = if (sdkInt >= 33 && activity != null) {
        ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        false
    }
    return permissionState(
        sdkInt = sdkInt,
        permissionGranted = permissionGranted,
        notificationsEnabled = notificationsEnabled,
        shouldShowRationale = shouldShowRationale,
        askedBefore = askedBefore
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun rememberNotificationPermission(
    onRequestResult: (granted: Boolean) -> Unit
): NotificationPermissionController {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    val repository = remember(context) {
        (context.applicationContext as? RkgitApplication)?.settingsRepository
            ?: try {
                EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    NotificationPermissionEntryPoint::class.java
                ).notificationSettingsRepository()
            } catch (_: Exception) {
                null
            }
    }

    var controllerRef: NotificationPermissionController? by remember { mutableStateOf(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        repository?.markPermissionRequested()
        controllerRef?.refresh()
        onRequestResult(granted)
    }

    val initialAskedBefore = repository?.permissionRequested?.value ?: false
    val initialState = remember(context, activity, initialAskedBefore) {
        computeState(context, activity, initialAskedBefore)
    }

    val controller = remember(context, activity, repository) {
        NotificationPermissionController(
            context = context,
            activity = activity,
            repository = repository,
            requestLauncher = {
                if (Build.VERSION.SDK_INT >= 33) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onRequestResult(NotificationManagerCompat.from(context).areNotificationsEnabled())
                }
            },
            initialState = initialState
        ).also { controllerRef = it }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                controller.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return controller
}

