package com.rkgit.one.data.profile

import com.rkgit.one.domain.model.Notice

fun matchesAudience(
    targetYears: List<Int>,
    targetDepartments: List<String>,
    filterYear: Int?,
    filterDepartment: String?
): Boolean {
    val yearOk = targetYears.isEmpty() || filterYear == null || filterYear in targetYears
    val deptOk = targetDepartments.isEmpty() || filterDepartment == null || filterDepartment in targetDepartments
    return yearOk && deptOk
}

fun List<Notice>.matchingAudience(filterYear: Int?, filterDepartment: String?): List<Notice> =
    filter { matchesAudience(it.targetYears, it.targetDepartments, filterYear, filterDepartment) }

fun audienceText(targetYears: List<Int>, targetDepartments: List<String>): String? {
    if (targetYears.isEmpty() && targetDepartments.isEmpty()) return null

    val yearsPart = if (targetYears.isEmpty()) {
        "All years"
    } else {
        val sortedYears = targetYears.distinct().sorted()
        "Year " + sortedYears.joinToString(", ")
    }

    val deptsPart = if (targetDepartments.isEmpty()) {
        "All branches"
    } else {
        targetDepartments.map { it.uppercase() }.distinct().joinToString(", ")
    }

    return "$yearsPart · $deptsPart"
}

fun audienceCaption(filterYear: Int?, filterDepartment: String?): String {
    if (filterYear == null && filterDepartment == null) {
        return "Showing all notices"
    }
    val yearPart = if (filterYear != null) yearLabel(filterYear) else "all years"
    val deptPart = if (!filterDepartment.isNullOrBlank()) filterDepartment.uppercase() else "all branches"
    return "Showing notices for $yearPart · $deptPart"
}

fun isAudienceFiltered(filterYear: Int?, filterDepartment: String?): Boolean =
    filterYear != null || filterDepartment != null
