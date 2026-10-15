package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import com.example.data.model.AyahDetail
import com.example.data.model.AyahSearchResult
import com.example.data.model.QuranBookmark
import com.example.data.model.Surah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class QuranRepository(private val context: Context) {

    private val dbName = "quran.db"
    private val prefs: SharedPreferences =
        context.getSharedPreferences("quran_prefs", Context.MODE_PRIVATE)

    @Volatile
    private var database: SQLiteDatabase? = null

    init {
        ensureDatabaseCopied()
    }

    private fun ensureDatabaseCopied() {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists() || dbFile.length() == 0L) {
            dbFile.parentFile?.mkdirs()
            context.assets.open(dbName).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private fun getDb(): SQLiteDatabase {
        return database ?: synchronized(this) {
            database ?: run {
                ensureDatabaseCopied()
                val dbFile = context.getDatabasePath(dbName)
                SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                ).also { database = it }
            }
        }
    }

    fun close() {
        try {
            database?.close()
            database = null
        } catch (_: Exception) {}
    }

    suspend fun getAllSurahs(): List<Surah> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Surah>()
        val db = getDb()
        val cursor = db.rawQuery(
            "SELECT number, name, full_name, english_name, verses_count, revelation_type, start_page FROM surahs ORDER BY number ASC",
            null
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    Surah(
                        number = c.getInt(0),
                        name = c.getString(1),
                        fullName = c.getString(2),
                        englishName = c.getString(3),
                        versesCount = c.getInt(4),
                        revelationType = c.getString(5),
                        startPage = c.getInt(6)
                    )
                )
            }
        }
        list
    }

    suspend fun getSurah(number: Int): Surah? = withContext(Dispatchers.IO) {
        val db = getDb()
        val cursor = db.rawQuery(
            "SELECT number, name, full_name, english_name, verses_count, revelation_type, start_page FROM surahs WHERE number = ?",
            arrayOf(number.toString())
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                Surah(
                    number = c.getInt(0),
                    name = c.getString(1),
                    fullName = c.getString(2),
                    englishName = c.getString(3),
                    versesCount = c.getInt(4),
                    revelationType = c.getString(5),
                    startPage = c.getInt(6)
                )
            } else null
        }
    }

    suspend fun getAyahsForPage(page: Int): List<AyahDetail> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AyahDetail>()
        val db = getDb()
        val query = """
            SELECT a.number, a.surah_number, s.name, a.number_in_surah, a.juz, a.page, a.hizb_quarter, a.text
            FROM ayahs a
            JOIN surahs s ON a.surah_number = s.number
            WHERE a.page = ?
            ORDER BY a.number ASC
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(page.toString()))
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    AyahDetail(
                        number = c.getInt(0),
                        surahNumber = c.getInt(1),
                        surahName = c.getString(2),
                        numberInSurah = c.getInt(3),
                        juz = c.getInt(4),
                        page = c.getInt(5),
                        hizbQuarter = c.getInt(6),
                        text = c.getString(7)
                    )
                )
            }
        }
        list
    }

    suspend fun getAyahsForSurah(surahNumber: Int): List<AyahDetail> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AyahDetail>()
        val db = getDb()
        val query = """
            SELECT a.number, a.surah_number, s.name, a.number_in_surah, a.juz, a.page, a.hizb_quarter, a.text
            FROM ayahs a
            JOIN surahs s ON a.surah_number = s.number
            WHERE a.surah_number = ?
            ORDER BY a.number ASC
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(surahNumber.toString()))
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    AyahDetail(
                        number = c.getInt(0),
                        surahNumber = c.getInt(1),
                        surahName = c.getString(2),
                        numberInSurah = c.getInt(3),
                        juz = c.getInt(4),
                        page = c.getInt(5),
                        hizbQuarter = c.getInt(6),
                        text = c.getString(7)
                    )
                )
            }
        }
        list
    }

    suspend fun getAyahsForJuz(juzNumber: Int): List<AyahDetail> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AyahDetail>()
        val db = getDb()
        val query = """
            SELECT a.number, a.surah_number, s.name, a.number_in_surah, a.juz, a.page, a.hizb_quarter, a.text
            FROM ayahs a
            JOIN surahs s ON a.surah_number = s.number
            WHERE a.juz = ?
            ORDER BY a.number ASC
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(juzNumber.toString()))
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    AyahDetail(
                        number = c.getInt(0),
                        surahNumber = c.getInt(1),
                        surahName = c.getString(2),
                        numberInSurah = c.getInt(3),
                        juz = c.getInt(4),
                        page = c.getInt(5),
                        hizbQuarter = c.getInt(6),
                        text = c.getString(7)
                    )
                )
            }
        }
        list
    }

    suspend fun searchQuran(query: String): List<AyahSearchResult> = withContext(Dispatchers.IO) {
        val cleaned = query.trim()
        if (cleaned.length < 2) return@withContext emptyList()

        val normalized = normalizeArabic(cleaned)
        val list = mutableListOf<AyahSearchResult>()
        val db = getDb()
        val sql = """
            SELECT a.number, a.surah_number, s.name, a.number_in_surah, a.page, a.text
            FROM ayahs a
            JOIN surahs s ON a.surah_number = s.number
            WHERE a.search_text LIKE ? OR a.text LIKE ?
            ORDER BY a.number ASC
            LIMIT 50
        """.trimIndent()

        val patternNorm = "%$normalized%"
        val patternRaw = "%$cleaned%"
        val cursor = db.rawQuery(sql, arrayOf(patternNorm, patternRaw))
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    AyahSearchResult(
                        number = c.getInt(0),
                        surahNumber = c.getInt(1),
                        surahName = c.getString(2),
                        numberInSurah = c.getInt(3),
                        page = c.getInt(4),
                        text = c.getString(5)
                    )
                )
            }
        }
        list
    }

    fun saveLastReadPage(page: Int) {
        prefs.edit().putInt("last_read_page", page).apply()
    }

    fun getLastReadPage(): Int {
        return prefs.getInt("last_read_page", 1)
    }

    fun saveBookmark(bookmark: QuranBookmark) {
        prefs.edit()
            .putInt("bm_surah_num", bookmark.surahNumber)
            .putString("bm_surah_name", bookmark.surahName)
            .putInt("bm_ayah_num", bookmark.ayahNumber)
            .putInt("bm_page_num", bookmark.pageNumber)
            .putLong("bm_saved_at", bookmark.savedAt)
            .apply()
    }

    fun getBookmark(): QuranBookmark? {
        val surahNum = prefs.getInt("bm_surah_num", -1)
        if (surahNum == -1) return null
        return QuranBookmark(
            surahNumber = surahNum,
            surahName = prefs.getString("bm_surah_name", "الفاتحة") ?: "الفاتحة",
            ayahNumber = prefs.getInt("bm_ayah_num", 1),
            pageNumber = prefs.getInt("bm_page_num", 1),
            savedAt = prefs.getLong("bm_saved_at", System.currentTimeMillis())
        )
    }

    private fun normalizeArabic(text: String): String {
        val regex = Regex("[\u0617-\u061A\u064B-\u0652\u06D6-\u06ED\u0670\u0640]")
        var s = regex.replace(text, "")
        s = s.replace('ٱ', 'ا').replace('إ', 'ا').replace('أ', 'ا').replace('آ', 'ا')
        s = s.replace('ة', 'ه').replace('ى', 'ي')
        return s.trim()
    }
}
