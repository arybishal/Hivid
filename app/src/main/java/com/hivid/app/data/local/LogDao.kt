package com.hivid.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {

    @Query("SELECT * FROM app_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntity>>

    @Query("SELECT * FROM app_logs WHERE level = :level ORDER BY timestamp DESC")
    fun getLogsByLevel(level: String): Flow<List<LogEntity>>

    @Query("""
        SELECT * FROM app_logs 
        WHERE (message LIKE '%' || :query || '%' 
           OR tag LIKE '%' || :query || '%' 
           OR exceptionMessage LIKE '%' || :query || '%' 
           OR diagnosticId LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
    """)
    fun searchLogs(query: String): Flow<List<LogEntity>>

    @Query("""
        SELECT * FROM app_logs 
        WHERE level = :level 
          AND (message LIKE '%' || :query || '%' 
           OR tag LIKE '%' || :query || '%' 
           OR exceptionMessage LIKE '%' || :query || '%' 
           OR diagnosticId LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
    """)
    fun searchLogsWithLevel(query: String, level: String): Flow<List<LogEntity>>

    @Query("SELECT * FROM app_logs WHERE diagnosticId = :diagnosticId ORDER BY timestamp ASC")
    fun getLogsForDiagnostic(diagnosticId: String): Flow<List<LogEntity>>

    @Query("SELECT COUNT(*) FROM app_logs")
    fun getTotalLogCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM app_logs WHERE level = :level")
    fun getCountByLevel(level: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: LogEntity): Long

    @Query("DELETE FROM app_logs WHERE id NOT IN (SELECT id FROM app_logs ORDER BY timestamp DESC LIMIT :keepCount)")
    suspend fun pruneOldLogs(keepCount: Int)

    @Query("DELETE FROM app_logs")
    suspend fun clearAll()
}
