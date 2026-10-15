package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.model.TaskEntity
import com.example.util.AlarmDiagnosticStore
import com.example.util.PrayerSoundManager
import com.example.util.TimeFormatHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

object AlarmScheduler {

    const val QUICK_TEST_TASK_ID = 77777
    const val TWO_MINUTE_TEST_TASK_ID = 88888
    const val IMMEDIATE_TEST_TASK_ID = 99999
    const val PRE_REMINDER_ID_BASE = 200000

    fun canScheduleExact(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            }
        }
    }

    fun calculateNextTriggerMillis(targetTime: String, dayOfWeek: Int): Long {
        val normalized = TimeFormatHelper.normalizeTime(targetTime)
        val parts = normalized.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = System.currentTimeMillis()

        if (dayOfWeek == 0) {
            // Daily task: if time has passed (or is within next 20 seconds), advance to tomorrow
            if (calendar.timeInMillis <= now + 20_000L) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else {
            // Specific day of week (1 = Sat, 2 = Sun, 3 = Mon, 4 = Tue, 5 = Wed, 6 = Thu, 7 = Fri)
            val targetCalDay = when (dayOfWeek) {
                1 -> Calendar.SATURDAY
                2 -> Calendar.SUNDAY
                3 -> Calendar.MONDAY
                4 -> Calendar.TUESDAY
                5 -> Calendar.WEDNESDAY
                6 -> Calendar.THURSDAY
                7 -> Calendar.FRIDAY
                else -> Calendar.SATURDAY
            }

            while (calendar.get(Calendar.DAY_OF_WEEK) != targetCalDay || calendar.timeInMillis <= now + 20_000L) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return calendar.timeInMillis
    }

    fun scheduleTaskAlarm(context: Context, task: TaskEntity): Boolean {
        if (!task.hasReminder) {
            cancelTaskAlarm(context, task.id)
            return false
        }

        val triggerMillis = calculateNextTriggerMillis(task.targetTime, task.dayOfWeek)
        val success = scheduleSpecificAlarm(
            context = context,
            taskId = task.id,
            title = task.title,
            category = task.category,
            triggerMillis = triggerMillis
        )

        // Pre-Prayer Reminder (Prayer Now style: 5, 10, 15, 20 mins before)
        val prayerKey = detectPrayerKey(task.title)
        val preTaskId = PRE_REMINDER_ID_BASE + task.id
        if (prayerKey != null) {
            val preMinutes = PrayerSoundManager.getPreReminderMinutes(context, prayerKey)
            if (preMinutes > 0) {
                val preTriggerMillis = triggerMillis - (preMinutes * 60 * 1000L)
                if (preTriggerMillis > System.currentTimeMillis() + 10_000L) {
                    scheduleSpecificAlarm(
                        context = context,
                        taskId = preTaskId,
                        title = task.title,
                        category = "عبادة",
                        triggerMillis = preTriggerMillis,
                        isPreReminder = true,
                        preMinutes = preMinutes
                    )
                }
            } else {
                cancelSpecificAlarm(context, preTaskId)
            }
        }

        AlarmDiagnosticStore.recordSchedule(
            context = context,
            taskId = task.id,
            taskTitle = task.title,
            category = task.category,
            targetTime = task.targetTime,
            nextTriggerMillis = triggerMillis,
            isExactAllowed = canScheduleExact(context),
            scheduleSuccess = success,
            lastError = if (success) null else "فشلت جدولة المنبه"
        )

        return success
    }

    private fun scheduleSpecificAlarm(
        context: Context,
        taskId: Int,
        title: String,
        category: String,
        triggerMillis: Long,
        isPreReminder: Boolean = false,
        preMinutes: Int = 0
    ): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_TASK_ID, taskId)
            putExtra(AlarmReceiver.EXTRA_TASK_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_TASK_CATEGORY, category)
            putExtra(AlarmReceiver.EXTRA_IS_PRE_REMINDER, isPreReminder)
            putExtra(AlarmReceiver.EXTRA_PRE_MINUTES, preMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            taskId + 100000,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isExactAllowed = canScheduleExact(context)

        return try {
            if (isExactAllowed) {
                try {
                    val clockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
                    alarmManager.setAlarmClock(clockInfo, pendingIntent)
                    true
                } catch (_: Exception) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            triggerMillis,
                            pendingIntent
                        )
                    }
                    true
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
                true
            }
        } catch (e: Exception) {
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun scheduleQuickTestAlarm(context: Context, secondsFromNow: Int = 15): Long {
        val triggerMillis = System.currentTimeMillis() + (secondsFromNow * 1000L)
        scheduleSpecificAlarm(
            context = context,
            taskId = QUICK_TEST_TASK_ID,
            title = "اختبار منبه مجدول سريع ($secondsFromNow ثانية)",
            category = "عبادة",
            triggerMillis = triggerMillis
        )
        AlarmDiagnosticStore.recordSchedule(
            context = context,
            taskId = QUICK_TEST_TASK_ID,
            taskTitle = "اختبار منبه مجدول سريع ($secondsFromNow ثانية)",
            category = "عبادة",
            targetTime = "+$secondsFromNow sec",
            nextTriggerMillis = triggerMillis,
            isExactAllowed = canScheduleExact(context),
            scheduleSuccess = true,
            lastError = null
        )
        return triggerMillis
    }

    fun scheduleTwoMinuteTestAlarm(context: Context): Long {
        val triggerMillis = System.currentTimeMillis() + (120 * 1000L)
        scheduleSpecificAlarm(
            context = context,
            taskId = TWO_MINUTE_TEST_TASK_ID,
            title = "اختبار منبه مجدول (بعد دقيقتين)",
            category = "عبادة",
            triggerMillis = triggerMillis
        )
        AlarmDiagnosticStore.setTwoMinuteTestTarget(context, triggerMillis)
        AlarmDiagnosticStore.recordSchedule(
            context = context,
            taskId = TWO_MINUTE_TEST_TASK_ID,
            taskTitle = "اختبار منبه مجدول (بعد دقيقتين)",
            category = "عبادة",
            targetTime = "+2 min",
            nextTriggerMillis = triggerMillis,
            isExactAllowed = canScheduleExact(context),
            scheduleSuccess = true,
            lastError = null
        )
        return triggerMillis
    }

    fun cancelTaskAlarm(context: Context, taskId: Int) {
        cancelSpecificAlarm(context, taskId)
        cancelSpecificAlarm(context, PRE_REMINDER_ID_BASE + taskId)
    }

    private fun cancelSpecificAlarm(context: Context, taskId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    suspend fun scheduleAllPendingAlarms(context: Context) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            val tasks = db.taskDao().getTasksWithRemindersSync()
            for (task in tasks) {
                scheduleTaskAlarm(context, task)
            }
        } catch (_: Exception) {}
    }

    fun sendImmediateTestNotification(
        context: Context,
        category: String = "عبادة",
        title: String = "حيّ على الصلاة - حان موعد الصلاة 🕌"
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_TASK_ID, IMMEDIATE_TEST_TASK_ID)
            putExtra(AlarmReceiver.EXTRA_TASK_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_TASK_CATEGORY, category)
        }
        context.sendBroadcast(intent)
    }

    private fun detectPrayerKey(title: String): String? {
        val lower = title.lowercase()
        return when {
            lower.contains("فجر") -> "Fajr"
            lower.contains("ظهر") -> "Dhuhr"
            lower.contains("عصر") -> "Asr"
            lower.contains("مغرب") -> "Maghrib"
            lower.contains("عشاء") -> "Isha"
            else -> null
        }
    }
}
