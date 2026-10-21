package com.rkgit.one.domain.model

enum class MaterialType(val value: String, val label: String) {
    NOTES("notes", "Notes"),
    SYLLABUS("syllabus", "Syllabus"),
    ST_EXAM("st_exam", "ST Exam"),
    PUT_EXAM("put_exam", "PUT Exam"),
    EXTERNAL_EXAM("external_exam", "External Exam"),
    LAB_MANUAL("lab_manual", "Lab Manual");

    companion object {
        fun fromValue(v: String?): MaterialType =
            entries.firstOrNull { it.value == v } ?: NOTES
    }
}

fun availableTypes(materials: List<StudyMaterial>): List<MaterialType> {
    val present = materials.map { it.type }.toSet()
    return MaterialType.entries.filter { it in present }
}

fun filterByType(materials: List<StudyMaterial>, selected: MaterialType?): List<StudyMaterial> {
    if (selected == null) return materials
    return materials.filter { it.type == selected }
}
