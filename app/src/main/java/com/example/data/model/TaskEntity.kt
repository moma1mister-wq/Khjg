package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val category: String, // "عبادة", "قرآن", "أذكار", "مذاكرة", "جامعة", "عامة"
    val isEssential: Boolean = false, // Critical: Essential tasks can NOT be deleted!
    val isCompleted: Boolean = false,
    val targetTime: String = "12:00", // HH:mm
    val dayOfWeek: Int = 0, // 0 = every day, 1 = Saturday ... 7 = Friday
    val hasReminder: Boolean = true,
    val notes: String = ""
)
