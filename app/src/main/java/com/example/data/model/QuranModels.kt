package com.example.data.model

data class Surah(
    val number: Int,
    val name: String,
    val englishName: String,
    val versesCount: Int,
    val revelationType: String, // "مكية" or "مدنية"
    val startPage: Int,
    val fullName: String = "سورة $name"
)

data class Ayah(
    val numberInSurah: Int,
    val text: String
)

data class AyahDetail(
    val number: Int,
    val surahNumber: Int,
    val surahName: String,
    val numberInSurah: Int,
    val juz: Int,
    val page: Int,
    val hizbQuarter: Int,
    val text: String
)

data class AyahSearchResult(
    val number: Int,
    val surahNumber: Int,
    val surahName: String,
    val numberInSurah: Int,
    val page: Int,
    val text: String
)

data class QuranBookmark(
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val pageNumber: Int,
    val savedAt: Long = System.currentTimeMillis()
)
