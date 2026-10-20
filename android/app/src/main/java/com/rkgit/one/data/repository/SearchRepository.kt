package com.rkgit.one.data.repository

import com.rkgit.one.data.remote.dto.SearchRowDto
import com.rkgit.one.data.remote.dto.toSearchResults
import com.rkgit.one.data.search.SEARCH_PAGE_SIZE
import com.rkgit.one.data.search.isSearchable
import com.rkgit.one.domain.model.SearchResults
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun search(normalizedQuery: String): SearchResults {
        if (!isSearchable(normalizedQuery)) {
            return SearchResults(emptyList(), emptyList(), emptyList())
        }
        val rows = supabase.postgrest.rpc(
            function = "search_all",
            parameters = buildJsonObject {
                put("q", normalizedQuery)
                put("max_per_kind", SEARCH_PAGE_SIZE)
            }
        ).decodeList<SearchRowDto>()

        return rows.toSearchResults(json)
    }
}
