package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val subject: String,
    val durationMinutes: Int,
    val pausedSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String, // "YYYY-MM-DD"
    val isPomodoro: Boolean = false,
    val sessionType: String = "مذاكرة حرة" // "مؤقت PDF", "مؤقت الفيديو", "مؤقت المذاكرة الحرة", "مؤقت حل التمارين"
)
