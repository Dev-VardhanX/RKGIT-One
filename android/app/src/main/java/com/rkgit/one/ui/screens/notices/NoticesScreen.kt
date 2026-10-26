package com.rkgit.one.ui.screens.notices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rkgit.one.data.profile.audienceCaption
import com.rkgit.one.data.profile.audienceText
import com.rkgit.one.data.profile.isAudienceFiltered
import com.rkgit.one.data.profile.matchingAudience
import com.rkgit.one.data.profile.yearLabel
import com.rkgit.one.domain.model.Notice
import com.rkgit.one.domain.model.NoticeCategory
import com.rkgit.one.ui.components.EmptyView
import com.rkgit.one.ui.components.LoadableContent
import com.rkgit.one.ui.components.ScreenScaffold
import com.rkgit.one.ui.util.toDateDisplay

@Composable
fun NoticesScreen(
    onNoticeClick: (String) -> Unit,
    viewModel: NoticesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val filter by viewModel.effectiveFilter.collectAsStateWithLifecycle()
    val departments by viewModel.departments.collectAsStateWithLifecycle()

    var selectedValue by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedCategory = selectedValue?.let { NoticeCategory.fromValue(it) }

    var yearMenuExpanded by remember { mutableStateOf(false) }
    var branchMenuExpanded by remember { mutableStateOf(false) }

    val effectiveYear = filter.year
    val effectiveDept = filter.departmentCode

    ScreenScaffold("Notices") { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LoadableContent(
                state = state,
                refreshing = refreshing,
                onRefresh = viewModel::refresh,
                onRetry = viewModel::load,
                refreshErrors = viewModel.refreshErrors
            ) { notices ->
                Column(Modifier.fillMaxSize()) {
                    // Audience row (Year chip, Branch chip)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Year chip
                        Box {
                            FilterChip(
                                selected = effectiveYear != null,
                                onClick = { yearMenuExpanded = true },
                                label = {
                                    Text(if (effectiveYear != null) yearLabel(effectiveYear) else "All years")
                                },
                                trailingIcon = {
                                    Icon(
                                        Icons.Filled.ArrowDropDown,
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier.semantics { contentDescription = "Year filter" }
                            )
                            DropdownMenu(
                                expanded = yearMenuExpanded,
                                onDismissRequest = { yearMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All years") },
                                    onClick = {
                                        viewModel.setYear(null)
                                        yearMenuExpanded = false
                                    },
                                    leadingIcon = if (effectiveYear == null) {
                                        { Icon(Icons.Filled.Check, contentDescription = null) }
                                    } else null
                                )
                                (1..4).forEach { y ->
                                    DropdownMenuItem(
                                        text = { Text("Year $y") },
                                        onClick = {
                                            viewModel.setYear(y)
                                            yearMenuExpanded = false
                                        },
                                        leadingIcon = if (effectiveYear == y) {
                                            { Icon(Icons.Filled.Check, contentDescription = null) }
                                        } else null
                                    )
                                }
                            }
                        }

                        // Branch chip
                        Box {
                            FilterChip(
                                selected = effectiveDept != null,
                                onClick = { branchMenuExpanded = true },
                                label = {
                                    Text(if (effectiveDept != null) effectiveDept.uppercase() else "All branches")
                                },
                                trailingIcon = {
                                    Icon(
                                        Icons.Filled.ArrowDropDown,
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier.semantics { contentDescription = "Branch filter" }
                            )
                            DropdownMenu(
                                expanded = branchMenuExpanded,
                                onDismissRequest = { branchMenuExpanded = false }
                            ) {
                                if (effectiveDept != null && departments.none { it.code == effectiveDept }) {
                                    DropdownMenuItem(
                                        text = { Text("${effectiveDept.uppercase()} (no longer available)") },
                                        onClick = {
                                            viewModel.setDepartment(effectiveDept)
                                            branchMenuExpanded = false
                                        },
                                        leadingIcon = { Icon(Icons.Filled.Check, contentDescription = null) }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("All branches") },
                                    onClick = {
                                        viewModel.setDepartment(null)
                                        branchMenuExpanded = false
                                    },
                                    leadingIcon = if (effectiveDept == null) {
                                        { Icon(Icons.Filled.Check, contentDescription = null) }
                                    } else null
                                )
                                departments.forEach { dept ->
                                    DropdownMenuItem(
                                        text = { Text(dept.name) },
                                        onClick = {
                                            viewModel.setDepartment(dept.code)
                                            branchMenuExpanded = false
                                        },
                                        leadingIcon = if (effectiveDept == dept.code) {
                                            { Icon(Icons.Filled.Check, contentDescription = null) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }

                    // Caption + "Show all notices" row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = audienceCaption(effectiveYear, effectiveDept),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isAudienceFiltered(effectiveYear, effectiveDept)) {
                            TextButton(onClick = viewModel::showAll) {
                                Text("Show all notices")
                            }
                        }
                    }

                    // Category chips row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedValue == null,
                                onClick = { selectedValue = null },
                                label = { Text("All") }
                            )
                        }
                        items(NoticeCategory.entries, key = { it.value }) { category ->
                            FilterChip(
                                selected = selectedValue == category.value,
                                onClick = { selectedValue = category.value },
                                label = { Text(category.label) }
                            )
                        }
                    }

                    val audienceNotices = notices.matchingAudience(effectiveYear, effectiveDept)
                    val filteredNotices = if (selectedCategory == null) {
                        audienceNotices
                    } else {
                        audienceNotices.filter { it.category == selectedCategory }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (notices.isEmpty()) {
                            item {
                                Box(Modifier.fillParentMaxSize()) {
                                    EmptyView("No notices right now.")
                                }
                            }
                        } else if (audienceNotices.isEmpty()) {
                            item {
                                Box(Modifier.fillParentMaxSize()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(32.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        val suffix = audienceCaption(effectiveYear, effectiveDept)
                                            .removePrefix("Showing notices for ")
                                        Text(
                                            text = "No notices for $suffix.",
                                            style = MaterialTheme.typography.bodyLarge,
                                            textAlign = TextAlign.Center
                                        )
                                        if (isAudienceFiltered(effectiveYear, effectiveDept)) {
                                            Spacer(Modifier.height(8.dp))
                                            TextButton(onClick = viewModel::showAll) {
                                                Text("Show all notices")
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (filteredNotices.isEmpty()) {
                            item {
                                Box(Modifier.fillParentMaxSize()) {
                                    EmptyView("No notices in this category.")
                                }
                            }
                        } else {
                            items(filteredNotices, key = { it.id }) { notice ->
                                NoticeCard(
                                    notice = notice,
                                    onClick = { onNoticeClick(notice.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun NoticeCard(
    notice: Notice,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (notice.pinned) {
                    Icon(
                        Icons.Filled.PushPin,
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = notice.category.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = notice.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            notice.body?.let { body ->
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val audText = audienceText(notice.targetYears, notice.targetDepartments)
            if (audText != null) {
                Text(
                    text = "For $audText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = notice.publishedAt.toDateDisplay(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                notice.attachment?.let { attachment ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.AttachFile,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = attachment.typeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
