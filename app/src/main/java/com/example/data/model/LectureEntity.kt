package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lectures")
data class LectureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val subject: String,
    val type: String, // "محاضرة", "سكشن", "معمل", "تمرين"
    val dayOfWeek: Int, // 1 = السبت, 2 = الأحد, 3 = الاثنين, 4 = الثلاثاء, 5 = الأربعاء, 6 = الخميس, 7 = الجمعة
    val startTime: String, // "09:00"
    val endTime: String,   // "11:00"
    val hall: String,      // "مدرج د", "قاعة 302", "معمل الحاسب 2"
    val instructor: String = "",
    val department: String = "",
    val notes: String = ""
)
