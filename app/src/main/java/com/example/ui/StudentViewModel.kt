package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.StudentApp
import com.example.data.local.PrepopulationData
import com.example.data.local.SubjectDuration
import com.example.data.local.TypeDuration
import com.example.data.model.AyahDetail
import com.example.data.model.AyahSearchResult
import com.example.data.model.DhikrItem
import com.example.data.model.LectureEntity
import com.example.data.model.QuranBookmark
import com.example.data.model.Surah
import com.example.data.model.TaskEntity
import com.example.receiver.AlarmScheduler
import com.example.service.ActiveTimerState
import com.example.service.StudyTimerService
import com.example.util.AlarmDiagnosticStore
import com.example.util.TaskAlarmDiagnostic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class StudentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as StudentApp).repository
    private val quranRepository = (application as StudentApp).quranRepository

    val allTasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLectures: StateFlow<List<LectureEntity>> = repository.allLectures
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalStudyMinutes: StateFlow<Int> = repository.totalStudyMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayStudyMinutes: StateFlow<Int> = repository.todayStudyMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val subjectDurations: StateFlow<List<SubjectDuration>> = repository.subjectDurations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val typeDurations: StateFlow<List<TypeDuration>> = repository.typeDurations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Timer state from foreground service
    val timerState: StateFlow<ActiveTimerState> = StudyTimerService.timerState

    // Current selected day of week (1 = السبت ... 7 = الجمعة)
    private val _selectedDay = MutableStateFlow(getCurrentDayOfWeek())
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    // Selected Tab within Timer Screen (0 = مؤقت المذاكرة, 1 = التمارين الرياضية والاستراحة, 2 = تقارير الإنجاز)
    private val _timerScreenTab = MutableStateFlow(0)
    val timerScreenTab: StateFlow<Int> = _timerScreenTab.asStateFlow()

    fun setTimerScreenTab(tabIndex: Int) {
        _timerScreenTab.value = tabIndex
    }

    // Interactive Dhikr counts state: Persisted in SharedPreferences
    private val _dhikrCounts = MutableStateFlow<Map<Int, Int>>(repository.getAllDhikrCounts())
    val dhikrCounts: StateFlow<Map<Int, Int>> = _dhikrCounts.asStateFlow()

    // Quran Bookmark
    private val _quranBookmark = MutableStateFlow<QuranBookmark?>(quranRepository.getBookmark() ?: repository.getQuranBookmark())
    val quranBookmark: StateFlow<QuranBookmark?> = _quranBookmark.asStateFlow()

    // Quran State
    private val _allSurahs = MutableStateFlow<List<Surah>>(emptyList())
    val allSurahs: StateFlow<List<Surah>> = _allSurahs.asStateFlow()

    private val _currentMushafPage = MutableStateFlow(quranRepository.getLastReadPage())
    val currentMushafPage: StateFlow<Int> = _currentMushafPage.asStateFlow()

    private val _currentPageAyahs = MutableStateFlow<List<AyahDetail>>(emptyList())
    val currentPageAyahs: StateFlow<List<AyahDetail>> = _currentPageAyahs.asStateFlow()

    private val _searchResults = MutableStateFlow<List<AyahSearchResult>>(emptyList())
    val searchResults: StateFlow<List<AyahSearchResult>> = _searchResults.asStateFlow()

    // Conflict detection warning message for current task operation
    private val _conflictWarning = MutableStateFlow<String?>(null)
    val conflictWarning: StateFlow<String?> = _conflictWarning.asStateFlow()

    private val _diagnostics = MutableStateFlow<List<TaskAlarmDiagnostic>>(emptyList())
    val diagnostics: StateFlow<List<TaskAlarmDiagnostic>> = _diagnostics.asStateFlow()

    // Location & Prayer Times State
    private val _prayerTimes = MutableStateFlow(
        com.example.util.LocationAndPrayerManager.calculatePrayerTimes(application)
    )
    val prayerTimes: StateFlow<com.example.util.PrayerTimesResult> = _prayerTimes.asStateFlow()

    init {
        loadSurahs()
        loadMushafPage(_currentMushafPage.value)
        refreshPrayerTimes()
    }

    private fun loadSurahs() {
        viewModelScope.launch {
            val list = quranRepository.getAllSurahs()
            _allSurahs.value = if (list.isNotEmpty()) list else PrepopulationData.allSurahs
        }
    }

    fun loadMushafPage(pageNumber: Int) {
        val pageClamped = pageNumber.coerceIn(1, 604)
        _currentMushafPage.value = pageClamped
        quranRepository.saveLastReadPage(pageClamped)
        viewModelScope.launch {
            _currentPageAyahs.value = quranRepository.getAyahsForPage(pageClamped)
        }
    }

    fun jumpToSurah(surahNumber: Int) {
        viewModelScope.launch {
            val surah = quranRepository.getSurah(surahNumber)
            val startPage = surah?.startPage ?: PrepopulationData.allSurahs.find { it.number == surahNumber }?.startPage ?: 1
            loadMushafPage(startPage)
        }
    }

    fun jumpToJuz(juzNumber: Int) {
        viewModelScope.launch {
            val ayahs = quranRepository.getAyahsForJuz(juzNumber)
            val firstPage = ayahs.firstOrNull()?.page ?: 1
            loadMushafPage(firstPage)
        }
    }

    fun searchQuranText(query: String) {
        if (query.trim().length < 2) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searchResults.value = quranRepository.searchQuran(query)
        }
    }

    fun clearQuranSearch() {
        _searchResults.value = emptyList()
    }

    fun setSelectedDay(day: Int) {
        _selectedDay.value = day
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTask(task.id, !task.isCompleted)
        }
    }

    fun addTask(task: TaskEntity, onConflict: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            val conflict = repository.findConflictingLecture(task.dayOfWeek, task.targetTime)
            if (conflict != null) {
                val warning = "تنبيه تعارض: موعد المهمة يتزامن مع ${conflict.type} (${conflict.subject}) في ${conflict.hall} [${conflict.startTime} - ${conflict.endTime}]"
                _conflictWarning.value = warning
                onConflict?.invoke(warning)
            } else {
                _conflictWarning.value = null
            }
            repository.addTask(task)
            refreshDiagnostics()
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            val conflict = repository.findConflictingLecture(task.dayOfWeek, task.targetTime)
            if (conflict != null) {
                _conflictWarning.value = "تنبيه: يتزامن مع ${conflict.subject} [${conflict.startTime} - ${conflict.endTime}]"
            } else {
                _conflictWarning.value = null
            }
            repository.updateTask(task)
            refreshDiagnostics()
        }
    }

    fun deleteTask(task: TaskEntity, onCannotDelete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val deleted = repository.deleteTask(task)
            if (!deleted) {
                onCannotDelete?.invoke()
            } else {
                refreshDiagnostics()
            }
        }
    }

    fun clearConflictWarning() {
        _conflictWarning.value = null
    }

    fun addLecture(lecture: LectureEntity) {
        viewModelScope.launch {
            repository.addLecture(lecture)
        }
    }

    fun updateLecture(lecture: LectureEntity) {
        viewModelScope.launch {
            repository.updateLecture(lecture)
        }
    }

    fun deleteLecture(lecture: LectureEntity) {
        viewModelScope.launch {
            repository.deleteLecture(lecture)
        }
    }

    fun resetScheduleToDefault() {
        viewModelScope.launch {
            repository.replaceAllLectures(PrepopulationData.azharCollegeLectures)
        }
    }

    // Timer Controls
    fun startTimer(
        subject: String,
        sessionType: String = "مؤقت المذاكرة الحرة",
        targetMinutes: Int = 25,
        isPomodoro: Boolean = true
    ) {
        StudyTimerService.start(
            context = getApplication(),
            subject = subject,
            sessionType = sessionType,
            targetMinutes = targetMinutes,
            isPomodoro = isPomodoro
        )
    }

    fun pauseTimer() {
        StudyTimerService.pause(getApplication())
    }

    fun resumeTimer() {
        StudyTimerService.resume(getApplication())
    }

    fun stopTimer() {
        StudyTimerService.stop(getApplication())
    }

    // Adhkar Counter (with persistent storage)
    fun incrementDhikr(dhikr: DhikrItem) {
        val current = _dhikrCounts.value[dhikr.id] ?: 0
        if (current < dhikr.targetCount) {
            val next = current + 1
            val updated = _dhikrCounts.value.toMutableMap()
            updated[dhikr.id] = next
            _dhikrCounts.value = updated
            repository.saveDhikrCount(dhikr.id, next)
        }
    }

    fun resetDhikr(dhikrId: Int) {
        val updated = _dhikrCounts.value.toMutableMap()
        updated[dhikrId] = 0
        _dhikrCounts.value = updated
        repository.saveDhikrCount(dhikrId, 0)
    }

    // Quran Bookmark
    fun saveBookmark(surahNumber: Int, surahName: String, ayahNumber: Int, pageNumber: Int) {
        val bm = QuranBookmark(surahNumber, surahName, ayahNumber, pageNumber)
        quranRepository.saveBookmark(bm)
        repository.saveQuranBookmark(bm)
        _quranBookmark.value = bm
    }

    // Test Notification
    fun sendTestNotification(category: String = "عبادة", title: String = "حان الآن موعد أذان الصلاة") {
        AlarmScheduler.sendImmediateTestNotification(getApplication(), category, title)
    }

    fun refreshDiagnostics() {
        _diagnostics.value = AlarmDiagnosticStore.getDiagnostics(getApplication())
    }

    fun refreshPrayerTimes() {
        _prayerTimes.value = com.example.util.LocationAndPrayerManager.calculatePrayerTimes(getApplication())
    }

    fun detectLocationAndSetCity(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = com.example.util.LocationAndPrayerManager.detectLocationAndSetCity(getApplication())
            if (result.isSuccess) {
                val city = result.getOrNull()
                com.example.util.LocationAndPrayerManager.syncPrayerTimesToTasks(getApplication())
                refreshPrayerTimes()
                refreshDiagnostics()
                onResult(true, "تم تحديد المدينة بنجاح: ${city?.arabicName ?: ""}")
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "تعذر تحديد الموقع تلقائيًا")
            }
        }
    }

    fun setManualCity(cityId: String) {
        com.example.util.LocationAndPrayerManager.setSelectedCity(getApplication(), cityId)
        viewModelScope.launch {
            com.example.util.LocationAndPrayerManager.syncPrayerTimesToTasks(getApplication())
            refreshPrayerTimes()
            refreshDiagnostics()
        }
    }

    fun setPrayerOffset(prayer: String, offsetMinutes: Int) {
        com.example.util.LocationAndPrayerManager.setOffset(getApplication(), prayer, offsetMinutes)
        viewModelScope.launch {
            com.example.util.LocationAndPrayerManager.syncPrayerTimesToTasks(getApplication())
            refreshPrayerTimes()
            refreshDiagnostics()
        }
    }

    fun scheduleQuickTest(seconds: Int = 15, onScheduled: (Long) -> Unit) {
        val triggerMillis = AlarmScheduler.scheduleQuickTestAlarm(getApplication(), seconds)
        refreshDiagnostics()
        onScheduled(triggerMillis)
    }

    fun scheduleTwoMinuteTest(onScheduled: (Long) -> Unit) {
        val triggerMillis = AlarmScheduler.scheduleTwoMinuteTestAlarm(getApplication())
        refreshDiagnostics()
        onScheduled(triggerMillis)
    }

    fun scheduleAllAlarmsNow() {
        viewModelScope.launch {
            AlarmScheduler.scheduleAllPendingAlarms(getApplication())
            refreshDiagnostics()
        }
    }

    fun openExactAlarmSettings() {
        AlarmScheduler.openExactAlarmSettings(getApplication())
    }

    fun canScheduleExactAlarms(): Boolean {
        return AlarmScheduler.canScheduleExact(getApplication())
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        return com.example.util.BatteryOptimizationHelper.isIgnoringBatteryOptimizations(getApplication())
    }

    fun requestIgnoreBatteryOptimizations() {
        com.example.util.BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(getApplication())
    }

    fun isOppoDevice(): Boolean {
        return com.example.util.BatteryOptimizationHelper.isOppoOrColorOs()
    }

    fun openColorOsAutoStartSettings(): Boolean {
        return com.example.util.BatteryOptimizationHelper.openColorOsAutoStartSettings(getApplication())
    }

    // Prayer Now Notification & Audio Management
    fun isMasterNotificationEnabled(): Boolean =
        com.example.util.PrayerSoundManager.isMasterNotificationEnabled(getApplication())

    fun setMasterNotificationEnabled(enabled: Boolean) {
        com.example.util.PrayerSoundManager.setMasterNotificationEnabled(getApplication(), enabled)
    }

    fun getPrayerAlertMode(prayerKey: String): com.example.util.PrayerAlertMode =
        com.example.util.PrayerSoundManager.getAlertMode(getApplication(), prayerKey)

    fun setPrayerAlertMode(prayerKey: String, mode: com.example.util.PrayerAlertMode) {
        com.example.util.PrayerSoundManager.setAlertMode(getApplication(), prayerKey, mode)
    }

    fun getPrayerSound(prayerKey: String): com.example.util.SoundOption =
        com.example.util.PrayerSoundManager.getSelectedSound(getApplication(), prayerKey)

    fun setPrayerSound(prayerKey: String, soundId: String) {
        com.example.util.PrayerSoundManager.setSelectedSound(getApplication(), prayerKey, soundId)
    }

    fun getPreReminderMinutes(prayerKey: String): Int =
        com.example.util.PrayerSoundManager.getPreReminderMinutes(getApplication(), prayerKey)

    fun setPreReminderMinutes(prayerKey: String, minutes: Int) {
        com.example.util.PrayerSoundManager.setPreReminderMinutes(getApplication(), prayerKey, minutes)
    }

    fun getCalculationMethod(): com.example.util.CalculationMethod =
        com.example.util.LocationAndPrayerManager.getCalculationMethod(getApplication())

    fun setCalculationMethod(method: com.example.util.CalculationMethod) {
        com.example.util.LocationAndPrayerManager.setCalculationMethod(getApplication(), method)
        viewModelScope.launch {
            com.example.util.LocationAndPrayerManager.syncPrayerTimesToTasks(getApplication())
            refreshPrayerTimes()
            refreshDiagnostics()
        }
    }

    fun getJuristicSchool(): com.example.util.JuristicSchool =
        com.example.util.LocationAndPrayerManager.getJuristicSchool(getApplication())

    fun setJuristicSchool(school: com.example.util.JuristicSchool) {
        com.example.util.LocationAndPrayerManager.setJuristicSchool(getApplication(), school)
        viewModelScope.launch {
            com.example.util.LocationAndPrayerManager.syncPrayerTimesToTasks(getApplication())
            refreshPrayerTimes()
            refreshDiagnostics()
        }
    }

    fun playAudioPreview(rawResId: Int, onComplete: (() -> Unit)? = null) {
        com.example.util.PrayerSoundManager.playPreview(getApplication(), rawResId, onComplete)
    }

    fun stopAudioPreview() {
        com.example.util.PrayerSoundManager.stopPreview()
    }

    fun isPreviewPlaying(): Boolean {
        return com.example.util.PrayerSoundManager.isPreviewPlaying()
    }

    fun getCategoryAlertMode(categoryKey: String): com.example.util.PrayerAlertMode =
        com.example.util.PrayerSoundManager.getCategoryAlertMode(getApplication(), categoryKey)

    fun setCategoryAlertMode(categoryKey: String, mode: com.example.util.PrayerAlertMode) {
        com.example.util.PrayerSoundManager.setCategoryAlertMode(getApplication(), categoryKey, mode)
    }

    fun getCategorySound(categoryKey: String): com.example.util.SoundOption =
        com.example.util.PrayerSoundManager.getCategorySound(getApplication(), categoryKey)

    fun setCategorySound(categoryKey: String, soundId: String) {
        com.example.util.PrayerSoundManager.setCategorySound(getApplication(), categoryKey, soundId)
    }

    fun testFullAdhan(prayerName: String = "الفجر", rawResId: Int = com.example.R.raw.sarab_tone) {
        com.example.service.AdhanPlayerService.start(getApplication(), prayerName, rawResId)
    }

    fun stopFullAdhan() {
        com.example.service.AdhanPlayerService.stop(getApplication())
    }

    companion object {
        fun getCurrentDayOfWeek(): Int {
            return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                Calendar.SATURDAY -> 1
                Calendar.SUNDAY -> 2
                Calendar.MONDAY -> 3
                Calendar.TUESDAY -> 4
                Calendar.WEDNESDAY -> 5
                Calendar.THURSDAY -> 6
                Calendar.FRIDAY -> 7
                else -> 1
            }
        }

        fun getDayName(day: Int): String {
            return when (day) {
                1 -> "السبت"
                2 -> "الأحد"
                3 -> "الاثنين"
                4 -> "الثلاثاء"
                5 -> "الأربعاء"
                6 -> "الخميس"
                7 -> "الجمعة"
                else -> "كل الأيام"
            }
        }
    }
}
