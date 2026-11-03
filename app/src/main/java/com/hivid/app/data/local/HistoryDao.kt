package com.hivid.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM conversion_history ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HistoryEntity): Long

    @Delete
    suspend fun delete(entity: HistoryEntity)

    @Query("DELETE FROM conversion_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM conversion_history")
    suspend fun clearAll()
}
