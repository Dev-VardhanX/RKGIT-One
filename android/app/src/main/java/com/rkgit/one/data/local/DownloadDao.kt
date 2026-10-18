package com.rkgit.one.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads")
    fun observeAll(): Flow<List<DownloadedFile>>

    @Query("SELECT * FROM downloads WHERE kind = :kind AND itemId = :itemId")
    fun observe(kind: String, itemId: String): Flow<DownloadedFile?>

    @Query("SELECT * FROM downloads WHERE kind = :kind AND itemId = :itemId")
    suspend fun get(kind: String, itemId: String): DownloadedFile?

    @Upsert
    suspend fun upsert(file: DownloadedFile)

    @Query("DELETE FROM downloads WHERE kind = :kind AND itemId = :itemId")
    suspend fun delete(kind: String, itemId: String)
}
