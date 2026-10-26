package com.rkgit.one.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rkgit.one.BuildConfig
import com.rkgit.one.data.profile.formatNoticesCaption
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

    val loadedDepartments = (departmentsState as? DepartmentsUiState.Loaded)?.list ?: emptyList()
    val caption = formatNoticesCaption(
        year = profile.year,
        departmentCode = profile.departmentCode,
        departments = loadedDepartments
    )

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
