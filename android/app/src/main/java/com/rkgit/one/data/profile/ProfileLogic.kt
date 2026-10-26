package com.rkgit.one.data.profile

import com.rkgit.one.domain.model.Department
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class StudentProfile(
    val setupDone: Boolean = false,
    val year: Int? = null,
    val departmentCode: String? = null
)

fun isValidYear(y: Int?): Boolean = y != null && y in 1..4

fun yearLabel(year: Int): String = "Year $year"

fun List<Department>.activeSorted(): List<Department> =
    filter { it.active }
        .sortedWith(compareBy<Department> { it.sortOrder }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })

enum class DepartmentCheck { NONE, OK, UNAVAILABLE }

fun checkDepartment(code: String?, departments: List<Department>): DepartmentCheck {
    if (code.isNullOrBlank()) return DepartmentCheck.NONE
    val match = departments.firstOrNull { it.code == code }
    return if (match != null && match.active) DepartmentCheck.OK else DepartmentCheck.UNAVAILABLE
}

fun canContinue(year: Int?, departmentCode: String?, departmentsFailed: Boolean): Boolean =
    isValidYear(year) && (departmentCode != null || departmentsFailed)

private val profileJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

fun encodeDepartments(list: List<Department>): String = profileJson.encodeToString(list)

fun decodeDepartments(json: String?): List<Department> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        profileJson.decodeFromString<List<Department>>(json)
    } catch (_: Throwable) {
        emptyList()
    }
}

fun sanitizeStoredYear(rawYear: Int): Int? = if (isValidYear(rawYear)) rawYear else null

fun sanitizeStoredDepartment(rawDept: String?): String? = if (rawDept.isNullOrBlank()) null else rawDept

fun formatNoticesCaption(
    year: Int?,
    departmentCode: String?,
    departments: List<Department>
): String {
    if (year == null && departmentCode == null) {
        return "Showing notices for all years and branches."
    }
    val yearPart = if (year != null) yearLabel(year) else "all years"
    val deptPart = if (departmentCode != null) {
        departments.firstOrNull { it.code == departmentCode && it.active }?.name
            ?: departmentCode.uppercase()
    } else {
        "all branches"
    }
    return "Showing notices for $yearPart, $deptPart"
}

interface ProfileStore {
    fun read(): StudentProfile
    fun write(profile: StudentProfile)
}

interface DepartmentsCacheStore {
    fun read(): List<Department>
    fun write(list: List<Department>)
}
