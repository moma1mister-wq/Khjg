package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AppDatabase
import com.example.data.local.PrepopulationData
import com.example.data.local.SubjectDuration
import com.example.data.local.TypeDuration
import com.example.data.model.LectureEntity
import com.example.data.model.QuranBookmark
import com.example.data.model.StudySessionEntity
import com.example.data.model.TaskEntity
import com.example.receiver.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudentRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val taskDao = db.taskDao()
    private val lectureDao = db.lectureDao()
    private val sessionDao = db.studySessionDao()
    private val prefs: SharedPreferences =
        context.getSharedPreferences("student_companion_prefs", Context.MODE_PRIVATE)

    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val allLectures: Flow<List<LectureEntity>> = lectureDao.getAllLectures()
    val allSessions: Flow<List<StudySessionEntity>> = sessionDao.getAllSessions()
    val totalStudyMinutes: Flow<Int> = sessionDao.getTotalDurationMinutes()
    val subjectDurations: Flow<List<SubjectDuration>> = sessionDao.getSubjectDurations()
    val typeDurations: Flow<List<TypeDuration>> = sessionDao.getTypeDurations()

    val todayStudyMinutes: Flow<Int>
        get() {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            return sessionDao.getTodayTotalMinutes(today)
        }

    fun getMinutesSince(sinceTimestamp: Long): Flow<Int> = sessionDao.getMinutesSince(sinceTimestamp)

    suspend fun checkInitialSeed() = withContext(Dispatchers.IO) {
        val essentialCount = taskDao.countEssentialTasks()
        if (essentialCount == 0) {
            taskDao.insertTasks(PrepopulationData.essentialTasks)
        } else {
            val hasWorkout = prefs.getBoolean("daily_workout_task_seeded_v1", false)
            if (!hasWorkout) {
                val workoutTask = PrepopulationData.essentialTasks.find { it.title.contains("التمرين") }
                if (workoutTask != null) {
                    val id = taskDao.insertTask(workoutTask)
                    AlarmScheduler.scheduleTaskAlarm(context, workoutTask.copy(id = id.toInt()))
                }
                prefs.edit().putBoolean("daily_workout_task_seeded_v1", true).apply()
            }
        }

        // Wipe old sample schedule and enforce the real Azhar University AI schedule
        val hasLoadedOfficialSchedule = prefs.getBoolean("azhar_official_schedule_v2", false)
        if (!hasLoadedOfficialSchedule) {
            lectureDao.deleteAllLectures()
            lectureDao.insertLectures(PrepopulationData.azharCollegeLectures)
            prefs.edit().putBoolean("azhar_official_schedule_v2", true).apply()
        } else {
            val count = lectureDao.countLectures()
            if (count == 0) {
                lectureDao.insertLectures(PrepopulationData.azharCollegeLectures)
            }
        }

        // Ensure all tasks in database have their exact alarms scheduled with AlarmManager
        AlarmScheduler.scheduleAllPendingAlarms(context)
    }

    suspend fun toggleTask(id: Int, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        taskDao.updateCompletion(id, isCompleted)
    }

    suspend fun addTask(task: TaskEntity): Long = withContext(Dispatchers.IO) {
        val id = taskDao.insertTask(task)
        val createdTask = task.copy(id = id.toInt())
        if (createdTask.hasReminder) {
            AlarmScheduler.scheduleTaskAlarm(context, createdTask)
        }
        id
    }

    suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        taskDao.updateTask(task)
        if (task.hasReminder) {
            AlarmScheduler.scheduleTaskAlarm(context, task)
        } else {
            AlarmScheduler.cancelTaskAlarm(context, task.id)
        }
    }

    suspend fun deleteTask(task: TaskEntity): Boolean = withContext(Dispatchers.IO) {
        // Essential tasks CANNOT be deleted (guaranteed safety requirement)
        if (task.isEssential) {
            return@withContext false
        }
        AlarmScheduler.cancelTaskAlarm(context, task.id)
        taskDao.deleteNonEssentialTaskById(task.id)
        true
    }

    suspend fun addLecture(lecture: LectureEntity): Long = withContext(Dispatchers.IO) {
        lectureDao.insertLecture(lecture)
    }

    suspend fun updateLecture(lecture: LectureEntity) = withContext(Dispatchers.IO) {
        lectureDao.updateLecture(lecture)
    }

    suspend fun deleteLecture(lecture: LectureEntity) = withContext(Dispatchers.IO) {
        lectureDao.deleteLecture(lecture)
    }

    suspend fun replaceAllLectures(lectures: List<LectureEntity>) = withContext(Dispatchers.IO) {
        lectureDao.deleteAllLectures()
        lectureDao.insertLectures(lectures)
    }

    suspend fun findConflictingLecture(dayOfWeek: Int, timeStr: String): LectureEntity? =
        withContext(Dispatchers.IO) {
            val lectures = lectureDao.getAllLecturesSync()
            val targetMinutes = parseTimeToMinutes(timeStr) ?: return@withContext null

            for (lec in lectures) {
                if (lec.dayOfWeek == dayOfWeek || dayOfWeek == 0) {
                    val startMin = parseTimeToMinutes(lec.startTime) ?: continue
                    val endMin = parseTimeToMinutes(lec.endTime) ?: continue
                    if (targetMinutes in startMin..endMin) {
                        return@withContext lec
                    }
                }
            }
            null
        }

    private fun parseTimeToMinutes(timeStr: String): Int? {
        val parts = timeStr.trim().split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return h * 60 + m
    }

    fun saveQuranBookmark(bookmark: QuranBookmark) {
        prefs.edit().apply {
            putInt("bm_surah_num", bookmark.surahNumber)
            putString("bm_surah_name", bookmark.surahName)
            putInt("bm_ayah_num", bookmark.ayahNumber)
            putInt("bm_page_num", bookmark.pageNumber)
            putLong("bm_timestamp", bookmark.savedAt)
            apply()
        }
    }

    fun getQuranBookmark(): QuranBookmark? {
        val surahNum = prefs.getInt("bm_surah_num", 0)
        if (surahNum == 0) return null
        val surahName = prefs.getString("bm_surah_name", "") ?: ""
        val ayahNum = prefs.getInt("bm_ayah_num", 1)
        val pageNum = prefs.getInt("bm_page_num", 1)
        val time = prefs.getLong("bm_timestamp", System.currentTimeMillis())
        return QuranBookmark(surahNum, surahName, ayahNum, pageNum, time)
    }

    // Persisted Dhikr Counts
    fun saveDhikrCount(dhikrId: Int, count: Int) {
        prefs.edit().putInt("dhikr_count_$dhikrId", count).apply()
    }

    fun getDhikrCount(dhikrId: Int): Int {
        return prefs.getInt("dhikr_count_$dhikrId", 0)
    }

    fun getAllDhikrCounts(): Map<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        for (item in PrepopulationData.allDhikrItems) {
            val count = prefs.getInt("dhikr_count_${item.id}", 0)
            if (count > 0) {
                map[item.id] = count
            }
        }
        return map
    }
}
