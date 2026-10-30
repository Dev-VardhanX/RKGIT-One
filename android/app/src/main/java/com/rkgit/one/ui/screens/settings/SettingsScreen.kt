package com.rkgit.one.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rkgit.one.BuildConfig
import com.rkgit.one.data.profile.formatNoticesCaption
import com.rkgit.one.push.PermissionState
import com.rkgit.one.push.ToggleAction
import com.rkgit.one.push.isAllowed
import com.rkgit.one.push.rememberNotificationPermission
import com.rkgit.one.push.settingsAfterPermissionResult
import com.rkgit.one.push.toggleAction
import com.rkgit.one.ui.components.ScreenScaffold
import com.rkgit.one.ui.screens.profile.DepartmentPicker
import com.rkgit.one.ui.screens.profile.DepartmentsUiState
import com.rkgit.one.ui.screens.profile.YearPicker

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val departmentsState by viewModel.departments.collectAsStateWithLifecycle()
    val notificationSettings by viewModel.notificationSettings.collectAsStateWithLifecycle()

    var pendingSwitch by rememberSaveable { mutableStateOf<String?>(null) }
    var showBlockedDialog by rememberSaveable { mutableStateOf(false) }
    var showExplanationDialog by rememberSaveable { mutableStateOf(false) }

    val controller = rememberNotificationPermission { granted ->
        val current = notificationSettings
        val requestedNotices = when (pendingSwitch) {
            "notices" -> true
            "events" -> false
            "status_allow" -> current.noticesEnabled
            else -> current.noticesEnabled
        }
        val requestedEvents = when (pendingSwitch) {
            "notices" -> false
            "events" -> true
            "status_allow" -> current.eventsEnabled
            else -> current.eventsEnabled
        }
        val next = settingsAfterPermissionResult(granted, current, requestedNotices, requestedEvents)
        viewModel.setBothNotifications(next.noticesEnabled, next.eventsEnabled)
        pendingSwitch = null
    }

    val loadedDepartments = (departmentsState as? DepartmentsUiState.Loaded)?.list ?: emptyList()
    val caption = formatNoticesCaption(
        year = profile.year,
        departmentCode = profile.departmentCode,
        departments = loadedDepartments
    )

    if (showExplanationDialog) {
        AlertDialog(
            onDismissRequest = {
                showExplanationDialog = false
                pendingSwitch = null
            },
            title = { Text("Allow notifications?") },
            text = {
                Text("RKGIT ONE sends a notification when a new notice or event is posted for you. Android will ask for your permission next.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExplanationDialog = false
                        controller.request()
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExplanationDialog = false
                        pendingSwitch = null
                    }
                ) {
                    Text("Not now")
                }
            }
        )
    }

    if (showBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedDialog = false },
            title = { Text("Notifications are blocked") },
            text = {
                Text("Turn on notifications for RKGIT ONE in your phone's settings, then come back.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBlockedDialog = false
                        controller.openSystemSettings()
                    }
                ) {
                    Text("Open settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBlockedDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    ScreenScaffold(
        title = "Settings",
        onBack = onBack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Your year and branch",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Notices are filtered to your year and branch. All notices stay available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Year",
                style = MaterialTheme.typography.titleMedium
            )
            YearPicker(
                selected = profile.year,
                onSelect = viewModel::setYear
            )

            Text(
                text = "Branch",
                style = MaterialTheme.typography.titleMedium
            )
            DepartmentPicker(
                state = departmentsState,
                selectedCode = profile.departmentCode,
                onSelect = viewModel::setDepartment,
                onRetry = viewModel::retry
            )

            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = "Notices",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "New notices for your year and branch",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = notificationSettings.noticesEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            when (toggleAction(true, controller.state)) {
                                ToggleAction.APPLY -> viewModel.setNoticesEnabled(true)
                                ToggleAction.REQUEST_PERMISSION -> {
                                    pendingSwitch = "notices"
                                    showExplanationDialog = true
                                }
                                ToggleAction.SHOW_BLOCKED -> {
                                    showBlockedDialog = true
                                }
                            }
                        } else {
                            viewModel.setNoticesEnabled(false)
                        }
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Notices notifications"
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = "Events",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "New college events",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = notificationSettings.eventsEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            when (toggleAction(true, controller.state)) {
                                ToggleAction.APPLY -> viewModel.setEventsEnabled(true)
                                ToggleAction.REQUEST_PERMISSION -> {
                                    pendingSwitch = "events"
                                    showExplanationDialog = true
                                }
                                ToggleAction.SHOW_BLOCKED -> {
                                    showBlockedDialog = true
                                }
                            }
                        } else {
                            viewModel.setEventsEnabled(false)
                        }
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Events notifications"
                    }
                )
            }

            if (!isAllowed(controller.state)) {
                if (controller.state == PermissionState.BLOCKED) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Notifications are turned off for RKGIT ONE in your phone's settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { controller.openSystemSettings() }) {
                            Text("Open settings")
                        }
                    }
                } else if (controller.state == PermissionState.NEEDS_REQUEST &&
                    (notificationSettings.noticesEnabled || notificationSettings.eventsEnabled)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Allow notifications to receive them.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                pendingSwitch = "status_allow"
                                showExplanationDialog = true
                            }
                        ) {
                            Text("Allow")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "About",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "App version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
