package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.StudySessionEntity
import kotlinx.coroutines.flow.Flow

data class SubjectDuration(
    val subject: String,
    val totalMinutes: Int
)

data class TypeDuration(
    val sessionType: String,
    val totalMinutes: Int
)

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE dateString = :dateString ORDER BY timestamp DESC")
    fun getSessionsForDate(dateString: String): Flow<List<StudySessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM study_sessions")
    fun getTotalDurationMinutes(): Flow<Int>

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM study_sessions WHERE dateString = :dateString")
    fun getTodayTotalMinutes(dateString: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM study_sessions WHERE timestamp >= :sinceTimestamp")
    fun getMinutesSince(sinceTimestamp: Long): Flow<Int>

    @Query("SELECT subject, COALESCE(SUM(durationMinutes), 0) as totalMinutes FROM study_sessions GROUP BY subject ORDER BY totalMinutes DESC")
    fun getSubjectDurations(): Flow<List<SubjectDuration>>

    @Query("SELECT sessionType, COALESCE(SUM(durationMinutes), 0) as totalMinutes FROM study_sessions GROUP BY sessionType ORDER BY totalMinutes DESC")
    fun getTypeDurations(): Flow<List<TypeDuration>>
}
