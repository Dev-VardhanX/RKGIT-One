package com.rkgit.one.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rkgit.one.domain.model.Department

sealed interface DepartmentsUiState {
    data object Loading : DepartmentsUiState
    data class Loaded(val list: List<Department>, val fromCache: Boolean) : DepartmentsUiState
    data class Failed(val message: String, val offline: Boolean) : DepartmentsUiState
}

@Composable
fun YearPicker(
    selected: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        (1..4).forEach { year ->
            val isSelected = selected == year
            FilterChip(
                selected = isSelected,
                onClick = {
                    if (isSelected) onSelect(null) else onSelect(year)
                },
                label = { Text("Year $year") }
            )
        }
    }
}

@Composable
fun DepartmentPicker(
    state: DepartmentsUiState,
    selectedCode: String?,
    onSelect: (String?) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        when (state) {
            is DepartmentsUiState.Loading -> {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Loading branches...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            is DepartmentsUiState.Loaded -> {
                val isMissing = selectedCode != null && state.list.none { it.code == selectedCode }
                if (isMissing) {
                    val missingLabel = "${selectedCode!!.uppercase()} (no longer available)"
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(null) }
                                .semantics { contentDescription = missingLabel }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = true,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = missingLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Text(
                            text = "Please choose your branch again.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 40.dp, bottom = 4.dp)
                        )
                    }
                }

                state.list.forEach { dept ->
                    val isSelected = selectedCode == dept.code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSelected) onSelect(null) else onSelect(dept.code)
                            }
                            .semantics { contentDescription = dept.name }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dept.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            is DepartmentsUiState.Failed -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Couldn't load branches.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (state.offline) {
                        Text(
                            text = "You're offline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
