package com.rkgit.one.ui.screens.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rkgit.one.data.profile.canContinue
import com.rkgit.one.ui.screens.profile.DepartmentPicker
import com.rkgit.one.ui.screens.profile.DepartmentsUiState
import com.rkgit.one.ui.screens.profile.YearPicker

@Composable
fun SetupScreen(
    onDone: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val departmentsState by viewModel.departments.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedDept by viewModel.selectedDepartmentCode.collectAsStateWithLifecycle()

    val departmentsFailed = departmentsState is DepartmentsUiState.Failed
    val canProceed = canContinue(
        year = selectedYear,
        departmentCode = selectedDept,
        departmentsFailed = departmentsFailed
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Welcome to RKGIT ONE",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Choose your year and branch to see the notices meant for you. You can change this any time in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Year",
            style = MaterialTheme.typography.titleMedium
        )
        YearPicker(
            selected = selectedYear,
            onSelect = viewModel::selectYear
        )
        Text(
            text = "Branch",
            style = MaterialTheme.typography.titleMedium
        )
        DepartmentPicker(
            state = departmentsState,
            selectedCode = selectedDept,
            onSelect = viewModel::selectDepartment,
            onRetry = viewModel::retry
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                viewModel.finish()
                onDone()
            },
            enabled = canProceed,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
        TextButton(
            onClick = {
                viewModel.skip()
                onDone()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skip for now")
        }
    }
}
