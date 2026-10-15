package com.example.data.model

data class DhikrCategory(
    val id: String,
    val title: String,
    val description: String,
    val badge: String
)

data class DhikrItem(
    val id: Int,
    val categoryId: String,
    val text: String,
    val targetCount: Int,
    val currentCount: Int = 0,
    val virtue: String = "" // فضل الذكر وسنده
)
