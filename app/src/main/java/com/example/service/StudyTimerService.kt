package com.example.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.StudySessionEntity
import com.example.receiver.AlarmReceiver
import com.example.util.SoundHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudyTimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    private var resumeRealtime: Long = 0L
    private var baseElapsedSeconds: Long = 0L

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("study_timer_prefs", Context.MODE_PRIVATE)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val subject = intent.getStringExtra(EXTRA_SUBJECT) ?: "مذاكرة عامة"
                val sessionType = intent.getStringExtra(EXTRA_SESSION_TYPE) ?: "مؤقت المذاكرة الحرة"
                val targetMinutes = intent.getIntExtra(EXTRA_TARGET_MINUTES, 25)
                val isPomodoro = intent.getBooleanExtra(EXTRA_IS_POMODORO, true)
                startTimer(subject, sessionType, targetMinutes, isPomodoro)
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_STOP -> stopAndSaveSession()
        }
        return START_STICKY
    }

    private fun startTimer(
        subject: String,
        sessionType: String,
        targetMinutes: Int,
        isPomodoro: Boolean
    ) {
        val targetSec = targetMinutes * 60L
        baseElapsedSeconds = 0L
        resumeRealtime = SystemClock.elapsedRealtime()

        _timerState.value = ActiveTimerState(
            isRunning = true,
            isPaused = false,
            subject = subject,
            sessionType = sessionType,
            elapsedSeconds = 0L,
            targetSeconds = targetSec,
            isPomodoro = isPomodoro,
            totalPausedSeconds = 0L
        )

        saveStateToPrefs()
        startForeground(NOTIFICATION_ID, buildNotification())

        // Schedule exact alarm for Pomodoro completion so screen-off deep sleep doesn't block it
        if (isPomodoro && targetSec > 0) {
            scheduleCompletionAlarm(targetSec)
        }

        launchTicker()
    }

    private fun launchTicker() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive && _timerState.value.isRunning) {
                delay(1000L)
                val current = _timerState.value
                if (!current.isPaused) {
                    val currentRealtime = SystemClock.elapsedRealtime()
                    val sessionRunSeconds = (currentRealtime - resumeRealtime) / 1000L
                    val totalElapsed = baseElapsedSeconds + sessionRunSeconds

                    _timerState.value = current.copy(elapsedSeconds = totalElapsed)

                    // Auto-finish Pomodoro if target reached
                    if (current.isPomodoro && totalElapsed >= current.targetSeconds) {
                        notifyCompletion(current.subject, current.sessionType)
                        stopAndSaveSession()
                        break
                    }
                } else {
                    _timerState.value = current.copy(
                        totalPausedSeconds = current.totalPausedSeconds + 1
                    )
                }
                updateNotification()
            }
        }
    }

    private fun pauseTimer() {
        val current = _timerState.value
        if (current.isRunning && !current.isPaused) {
            val currentRealtime = SystemClock.elapsedRealtime()
            baseElapsedSeconds += (currentRealtime - resumeRealtime) / 1000L
            _timerState.value = current.copy(isPaused = true, elapsedSeconds = baseElapsedSeconds)
            saveStateToPrefs()
            cancelCompletionAlarm()
            updateNotification()
        }
    }

    private fun resumeTimer() {
        val current = _timerState.value
        if (current.isRunning && current.isPaused) {
            resumeRealtime = SystemClock.elapsedRealtime()
            _timerState.value = current.copy(isPaused = false)
            saveStateToPrefs()

            if (current.isPomodoro) {
                val remainingSeconds = (current.targetSeconds - baseElapsedSeconds).coerceAtLeast(1L)
                scheduleCompletionAlarm(remainingSeconds)
            }

            updateNotification()
        }
    }

    private fun stopAndSaveSession() {
        val current = _timerState.value
        timerJob?.cancel()
        timerJob = null
        cancelCompletionAlarm()

        if (current.elapsedSeconds > 10) {
            val durationMinutes = (current.elapsedSeconds / 60).toInt().coerceAtLeast(1)
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val session = StudySessionEntity(
                subject = current.subject,
                durationMinutes = durationMinutes,
                pausedSeconds = current.totalPausedSeconds.toInt(),
                timestamp = System.currentTimeMillis(),
                dateString = dateStr,
                isPomodoro = current.isPomodoro,
                sessionType = current.sessionType
            )

            serviceScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getInstance(applicationContext)
                db.studySessionDao().insertSession(session)
            }
        }

        clearStatePrefs()
        _timerState.value = ActiveTimerState(isRunning = false, isPaused = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notifyCompletion(subject: String, sessionType: String) {
        try {
            SoundHelper.playSound(applicationContext, R.raw.study_tone, isAlarm = true)
            SoundHelper.vibrate(applicationContext, isAlarm = true)

            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val tapIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("EXTRA_NAV_TARGET", "timer")
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                NOTIFICATION_ID + 1,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val finishNotification = NotificationCompat.Builder(this, AlarmReceiver.CHANNEL_TIMER_ALERT)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("أحسنت! اكتملت الجلسة بنجاح 🎯")
                .setContentText("انتهى موعد $sessionType لمادة $subject")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(SoundHelper.getAudioUri(this, R.raw.study_tone))
                .setVibrate(longArrayOf(0, 500, 200, 500, 200, 800))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            nm.notify(NOTIFICATION_ID + 1, finishNotification)
        } catch (_: Exception) {}
    }

    private fun scheduleCompletionAlarm(remainingSeconds: Long) {
        val triggerAtMillis = System.currentTimeMillis() + (remainingSeconds * 1000L)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_TASK_ID, POMODORO_ALARM_ID)
            putExtra(AlarmReceiver.EXTRA_TASK_TITLE, "اكتملت جلسة المذاكرة بنجاح")
            putExtra(AlarmReceiver.EXTRA_TASK_CATEGORY, "مذاكرة")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            POMODORO_ALARM_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: Exception) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } catch (_: Exception) {}
        }
    }

    private fun cancelCompletionAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            POMODORO_ALARM_ID,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun saveStateToPrefs() {
        val state = _timerState.value
        prefs.edit()
            .putBoolean("is_running", state.isRunning)
            .putBoolean("is_paused", state.isPaused)
            .putString("subject", state.subject)
            .putString("session_type", state.sessionType)
            .putLong("elapsed_seconds", state.elapsedSeconds)
            .putLong("target_seconds", state.targetSeconds)
            .putBoolean("is_pomodoro", state.isPomodoro)
            .apply()
    }

    private fun clearStatePrefs() {
        prefs.edit().clear().apply()
    }

    private fun buildNotification(): Notification {
        val state = _timerState.value
        val minutes = state.elapsedSeconds / 60
        val seconds = state.elapsedSeconds % 60
        val timeFormatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)

        val statusText = if (state.isPaused) {
            "موقوف مؤقتًا (${state.sessionType}) - $timeFormatted"
        } else {
            "جلسة نشطة (${state.sessionType}) - $timeFormatted"
        }

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_NAV_TARGET", "timer")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = Intent(this, StudyTimerService::class.java).apply {
            action = if (state.isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val pauseResumePending = PendingIntent.getService(
            this,
            1,
            pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pauseResumeActionTitle = if (state.isPaused) "استئناف" else "إيقاف مؤقت"

        val stopIntent = Intent(this, StudyTimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("مؤقت المذاكرة: ${state.subject}")
            .setContentText(statusText)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .addAction(0, pauseResumeActionTitle, pauseResumePending)
            .addAction(0, "إنهاء وحفظ", stopPending)
            .build()
    }

    private fun updateNotification() {
        if (!_timerState.value.isRunning) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    companion object {
        const val CHANNEL_ID = "study_timer_channel"
        const val NOTIFICATION_ID = 2001
        const val POMODORO_ALARM_ID = 66666

        const val ACTION_START = "action_start_timer"
        const val ACTION_PAUSE = "action_pause_timer"
        const val ACTION_RESUME = "action_resume_timer"
        const val ACTION_STOP = "action_stop_timer"

        const val EXTRA_SUBJECT = "extra_subject"
        const val EXTRA_SESSION_TYPE = "extra_session_type"
        const val EXTRA_TARGET_MINUTES = "extra_target_minutes"
        const val EXTRA_IS_POMODORO = "extra_is_pomodoro"

        private val _timerState = MutableStateFlow(ActiveTimerState())
        val timerState: StateFlow<ActiveTimerState> = _timerState.asStateFlow()

        fun start(
            context: Context,
            subject: String,
            sessionType: String = "مؤقت المذاكرة الحرة",
            targetMinutes: Int = 25,
            isPomodoro: Boolean = true
        ) {
            val intent = Intent(context, StudyTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SUBJECT, subject)
                putExtra(EXTRA_SESSION_TYPE, sessionType)
                putExtra(EXTRA_TARGET_MINUTES, targetMinutes)
                putExtra(EXTRA_IS_POMODORO, isPomodoro)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pause(context: Context) {
            val intent = Intent(context, StudyTimerService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, StudyTimerService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, StudyTimerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
