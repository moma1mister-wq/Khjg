package com.example.service

data class ActiveTimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val subject: String = "مذاكرة عامة",
    val sessionType: String = "مؤقت المذاكرة الحرة",
    val elapsedSeconds: Long = 0L,
    val targetSeconds: Long = 25 * 60L,
    val isPomodoro: Boolean = true,
    val totalPausedSeconds: Long = 0L
)
