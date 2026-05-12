package com.example.worktimer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkSessionDao {
    @Query("SELECT * FROM work_sessions WHERE date = :date LIMIT 1")
    fun getSessionByDate(date: String): Flow<WorkSessionEntity?>

    @Query("SELECT * FROM work_sessions WHERE date = :date LIMIT 1")
    suspend fun getSessionByDateOnce(date: String): WorkSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(session: WorkSessionEntity)

    @Query("SELECT * FROM work_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<WorkSessionEntity>>

    @Query("SELECT * FROM work_sessions WHERE date IN (:dates) ORDER BY date ASC")
    fun getSessionsByDates(dates: List<String>): Flow<List<WorkSessionEntity>>

    @Query("DELETE FROM work_sessions")
    suspend fun deleteAll()
}
