package com.rkgit.one.data.repository

import com.rkgit.one.data.remote.dto.RecentMaterialDto
import com.rkgit.one.data.remote.dto.StudyMaterialDto
import com.rkgit.one.data.remote.dto.SubjectDto
import com.rkgit.one.data.remote.dto.toDomain
import com.rkgit.one.domain.model.RecentMaterial
import com.rkgit.one.domain.model.StudyMaterial
import com.rkgit.one.domain.model.Subject
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaterialsRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    /** Subjects for a year/semester that have at least one study material. */
    suspend fun subjects(year: Int, semester: Int): List<Subject> =
        supabase.from("subjects")
            .select(Columns.raw("id, year, semester, name, study_materials(count)")) {
                filter { eq("year", year); eq("semester", semester) }
                order("name", Order.ASCENDING)
            }
            .decodeList<SubjectDto>()
            .map { it.toDomain() }
            .filter { it.materialCount > 0 }

    /** Study materials for a subject, newest first. */
    suspend fun materials(subjectId: String): List<StudyMaterial> =
        supabase.from("study_materials")
            .select {
                filter { eq("subject_id", subjectId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<StudyMaterialDto>()
            .map { it.toDomain() }

    suspend fun recent(limit: Long = 5): List<RecentMaterial> =
        supabase.from("study_materials")
            .select(Columns.raw("*, subjects(name, year, semester)")) {
                order("created_at", Order.DESCENDING)
                limit(limit)
            }
            .decodeList<RecentMaterialDto>()
            .map { it.toDomain() }
}
