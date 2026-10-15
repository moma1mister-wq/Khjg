package com.example.receiver

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.service.AdhanPlayerService
import com.example.util.AlarmDiagnosticStore
import com.example.util.PrayerAlertMode
import com.example.util.PrayerSoundManager
import com.example.util.SoundHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    @SuppressLint("WakelockTimeout")
    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "StudentCompanion:AlarmReceiverWakeLock"
        )
        wakeLock?.acquire(15_000L) // 15 seconds safe timeout

        val taskId = intent.getIntExtra(EXTRA_TASK_ID, 0)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "تنبيه موعد مهمة"
        val taskCategory = intent.getStringExtra(EXTRA_TASK_CATEGORY) ?: "عامة"
        val isPreReminder = intent.getBooleanExtra(EXTRA_IS_PRE_REMINDER, false)
        val preMinutes = intent.getIntExtra(EXTRA_PRE_MINUTES, 10)

        // Check if master notifications enabled
        if (!PrayerSoundManager.isMasterNotificationEnabled(context)) {
            try {
                if (wakeLock?.isHeld == true) wakeLock.release()
            } catch (_: Exception) {}
            return
        }

        var notificationPosted = false

        if (isPreReminder) {
            // Pre-prayer reminder (تذكير اقتراب موعد الصلاة - مثل Prayer Now)
            val prayerKey = detectPrayerKey(taskTitle, taskCategory) ?: "Dhuhr"
            val prayerArabic = PrayerSoundManager.getPrayerArabicName(prayerKey)
            notificationPosted = showHeadsUpNotification(
                context = context,
                taskId = taskId,
                title = "اقترب موعد $prayerArabic 🕌",
                subtitle = "متبقي $preMinutes دقائق على دخول الوقت • استعد للوضوء والصلاة",
                channelId = CHANNEL_PRE_PRAYER,
                soundUri = SoundHelper.getAudioUri(context, R.raw.sarab_short),
                category = "عبادة"
            )
        } else {
            val prayerKey = detectPrayerKey(taskTitle, taskCategory)
            val isPrayer = (prayerKey != null)

            if (isPrayer && prayerKey != null) {
                val alertMode = PrayerSoundManager.getAlertMode(context, prayerKey)
                val selectedSound = PrayerSoundManager.getSelectedSound(context, prayerKey)

                when (alertMode) {
                    PrayerAlertMode.DISABLED -> {
                        // User disabled alert for this prayer
                        notificationPosted = false
                    }
                    PrayerAlertMode.FULL_ADHAN -> {
                        // Start Foreground Service to play full Adhan cleanly without cutting off
                        // AdhanPlayerService handles audio via MediaPlayer with WakeLock + persistent notification with STOP action
                        AdhanPlayerService.start(context, taskTitle, selectedSound.rawResId)
                        notificationPosted = true
                    }
                    PrayerAlertMode.SHORT_TONE -> {
                        // Sound is played natively by the Notification Channel, preventing any double-playback!
                        val channelId = resolvePrayerChannel(prayerKey)
                        notificationPosted = showHeadsUpNotification(
                            context = context,
                            taskId = taskId,
                            title = taskTitle,
                            subtitle = "حيّ على الصلاة، حيّ على الفلاح 🕌",
                            channelId = channelId,
                            soundUri = SoundHelper.getAudioUri(context, selectedSound.rawResId),
                            category = taskCategory
                        )
                    }
                    PrayerAlertMode.SILENT -> {
                        // Post to dedicated silent channel (vibrate only, zero audio)
                        notificationPosted = showHeadsUpNotification(
                            context = context,
                            taskId = taskId,
                            title = taskTitle,
                            subtitle = "حان موعد $taskTitle (تنبيه صامت واهتزاز) 🕌",
                            channelId = CHANNEL_SILENT,
                            soundUri = null,
                            category = taskCategory
                        )
                    }
                }
            } else {
                // Non-prayer task (Lectures, Workouts, Adhkar, Sleep, Tests)
                val categoryKey = when {
                    taskCategory == "رياضة" || taskTitle.contains("تمرين") -> "workout"
                    taskTitle.contains("نوم") -> "sleep"
                    taskCategory == "جامعة" || taskCategory == "مذاكرة" || taskTitle.contains("محاضرة") -> "study"
                    taskCategory == "أذكار" || taskCategory == "قرآن" -> "adhkar"
                    else -> "study"
                }

                val categoryMode = PrayerSoundManager.getCategoryAlertMode(context, categoryKey)
                val categorySound = PrayerSoundManager.getCategorySound(context, categoryKey)

                if (categoryMode != PrayerAlertMode.DISABLED) {
                    val (channelId, _) = resolveChannelAndSound(taskCategory, taskTitle)
                    val targetChannel = if (categoryMode == PrayerAlertMode.SILENT) CHANNEL_SILENT else channelId
                    val soundUri = if (categoryMode == PrayerAlertMode.SILENT) null else SoundHelper.getAudioUri(context, categorySound.rawResId)
                    val subtitle = resolveSubtitle(channelId, taskCategory, taskTitle)

                    // Sound is played by the notification system only (No double audio)
                    notificationPosted = showHeadsUpNotification(
                        context = context,
                        taskId = taskId,
                        title = taskTitle,
                        subtitle = subtitle,
                        channelId = targetChannel,
                        soundUri = soundUri,
                        category = taskCategory
                    )
                }
            }
        }

        // Record diagnostic log
        AlarmDiagnosticStore.recordAlarmReceived(context, taskId, notificationPosted)

        // Auto reschedule daily tasks for next day (skip quick one-shot tests)
        if (taskId != AlarmScheduler.TWO_MINUTE_TEST_TASK_ID &&
            taskId != AlarmScheduler.QUICK_TEST_TASK_ID &&
            taskId != AlarmScheduler.IMMEDIATE_TEST_TASK_ID &&
            !isPreReminder &&
            taskId > 0
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val tasks = db.taskDao().getTasksWithRemindersSync()
                    val task = tasks.find { it.id == taskId }
                    if (task != null && task.hasReminder) {
                        AlarmScheduler.scheduleTaskAlarm(context, task)
                    }
                } finally {
                    try {
                        if (wakeLock?.isHeld == true) {
                            wakeLock.release()
                        }
                    } catch (_: Exception) {}
                    pendingResult.finish()
                }
            }
        } else {
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
            } catch (_: Exception) {}
        }
    }

    private fun detectPrayerKey(title: String, category: String): String? {
        val lower = title.lowercase()
        return when {
            lower.contains("فجر") -> "Fajr"
            lower.contains("ظهر") -> "Dhuhr"
            lower.contains("عصر") -> "Asr"
            lower.contains("مغرب") -> "Maghrib"
            lower.contains("عشاء") -> "Isha"
            category == "عبادة" && lower.contains("صلاة") -> "Dhuhr"
            else -> null
        }
    }

    private fun resolvePrayerChannel(prayerKey: String): String {
        return when (prayerKey) {
            "Fajr" -> CHANNEL_PRAYER_FAJR
            "Dhuhr" -> CHANNEL_PRAYER_DHUHR
            "Asr" -> CHANNEL_PRAYER_ASR
            "Maghrib" -> CHANNEL_PRAYER_MAGHRIB
            "Isha" -> CHANNEL_PRAYER_ISHA
            else -> CHANNEL_PRAYER
        }
    }

    private fun resolveChannelAndSound(category: String, title: String): Pair<String, Int> {
        val lower = title.lowercase()
        return when {
            category == "رياضة" || lower.contains("تمرين") || lower.contains("تمارين") -> {
                Pair(CHANNEL_WORKOUTS, R.raw.lecture_tone)
            }
            lower.contains("نوم") -> {
                Pair(CHANNEL_SLEEP, R.raw.adhkar_tone)
            }
            category == "جامعة" || category == "مذاكرة" || lower.contains("محاضرة") || lower.contains("معمل") -> {
                Pair(CHANNEL_STUDY, R.raw.lecture_tone)
            }
            category == "أذكار" || category == "قرآن" || lower.contains("أذكار") || lower.contains("ورد") -> {
                Pair(CHANNEL_ADHKAR, R.raw.adhkar_tone)
            }
            else -> {
                Pair(CHANNEL_ADHKAR, R.raw.adhkar_tone)
            }
        }
    }

    private fun resolveSubtitle(channelId: String, category: String, title: String): String {
        return when (channelId) {
            CHANNEL_WORKOUTS -> "حان موعد التمرين اليومي وتنشيط الظهر والبطن والأرجل 💪"
            CHANNEL_SLEEP -> "باسمك ربي وضعت جنبي - أذكار النوم والاستعداد للراحة 🌙"
            CHANNEL_STUDY -> "موعد أكاديمي وجلسة تركيز ومذاكرة 📚"
            CHANNEL_ADHKAR -> "ألا بذكر الله تطمئن القلوب 🌿"
            CHANNEL_PRE_PRAYER -> "اقترب موعد الأذان، استعد للوضوء والصلاة 🕌"
            else -> "رفيق الطالب - منبه المواعيد"
        }
    }

    private fun showHeadsUpNotification(
        context: Context,
        taskId: Int,
        title: String,
        subtitle: String,
        channelId: String,
        soundUri: android.net.Uri?,
        category: String
    ): Boolean {
        return try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val navTarget = when {
                channelId == CHANNEL_WORKOUTS || category == "رياضة" || title.contains("تمرين") -> "workouts"
                channelId == CHANNEL_STUDY || category == "مذاكرة" -> "timer"
                category == "جامعة" -> "schedule"
                category == "قرآن" || category == "أذكار" -> "quran"
                else -> "tasks"
            }

            val tapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("EXTRA_NAV_TARGET", navTarget)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                taskId,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Full screen intent for Heads-up presentation on lock screen & high priority
            val fullScreenIntent = PendingIntent.getActivity(
                context,
                taskId + 50000,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(fullScreenIntent, true)
                .setVibrate(longArrayOf(0, 500, 200, 500, 200, 800))

            if (soundUri != null && channelId != CHANNEL_SILENT) {
                builder.setSound(soundUri)
            }

            notificationManager.notify(taskId, builder.build())
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        const val CHANNEL_PRAYER = "channel_prayer_v3"
        const val CHANNEL_PRAYER_FAJR = "prayer_channel_fajr_v3"
        const val CHANNEL_PRAYER_DHUHR = "prayer_channel_dhuhr_v3"
        const val CHANNEL_PRAYER_ASR = "prayer_channel_asr_v3"
        const val CHANNEL_PRAYER_MAGHRIB = "prayer_channel_maghrib_v3"
        const val CHANNEL_PRAYER_ISHA = "prayer_channel_isha_v3"
        const val CHANNEL_PRE_PRAYER = "prayer_channel_pre_reminder_v3"
        const val CHANNEL_SILENT = "channel_silent_alerts_v3"

        const val CHANNEL_STUDY = "channel_study_v3"
        const val CHANNEL_ADHKAR = "channel_adhkar_v3"
        const val CHANNEL_WORKOUTS = "channel_workouts_v3"
        const val CHANNEL_SLEEP = "channel_sleep_v3"
        const val CHANNEL_TIMER_ALERT = "channel_timer_alert_v3"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_CATEGORY = "extra_task_category"
        const val EXTRA_IS_PRE_REMINDER = "extra_is_pre_reminder"
        const val EXTRA_PRE_MINUTES = "extra_pre_minutes"
    }
}
