package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.os.Build
import com.example.data.repository.StudentRepository
import com.example.receiver.AlarmReceiver
import com.example.receiver.AlarmScheduler
import com.example.service.AdhanPlayerService
import com.example.service.StudyTimerService
import com.example.util.LocationAndPrayerManager
import com.example.util.SoundHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StudentApp : Application() {

    lateinit var repository: StudentRepository
        private set

    lateinit var quranRepository: com.example.data.repository.QuranRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = StudentRepository(this)
        quranRepository = com.example.data.repository.QuranRepository(this)

        createNotificationChannels()

        // Verify initial seed, synchronize prayer times for selected city, and schedule all active alarms
        CoroutineScope(Dispatchers.IO).launch {
            repository.checkInitialSeed()
            LocationAndPrayerManager.syncPrayerTimesToTasks(this@StudentApp)
            AlarmScheduler.scheduleAllPendingAlarms(this@StudentApp)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return

            val audioAttributesAlarm = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val audioAttributesNotification = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val strongVibration = longArrayOf(0, 600, 250, 600, 250, 1000)
            val gentleVibration = longArrayOf(0, 400, 200, 400)

            val prayerSoundUri = SoundHelper.getAudioUri(this, R.raw.sarab_tone)
            val sarabShortSoundUri = SoundHelper.getAudioUri(this, R.raw.sarab_short)
            val adhkarSoundUri = SoundHelper.getAudioUri(this, R.raw.sarab_short)
            val lectureSoundUri = SoundHelper.getAudioUri(this, R.raw.lecture_tone)
            val studySoundUri = SoundHelper.getAudioUri(this, R.raw.study_tone)

            // 1. Separate channels for each of the 5 prayers (Prayer Now pattern)
            val prayerChannels = listOf(
                Triple(AlarmReceiver.CHANNEL_PRAYER_FAJR, "أذان صلاة الفجر", "تنبيهات وأذان الفجر بصوت ندي واهتزاز منبه"),
                Triple(AlarmReceiver.CHANNEL_PRAYER_DHUHR, "أذان صلاة الظهر", "تنبيهات وأذان الظهر عند دخول الوقت"),
                Triple(AlarmReceiver.CHANNEL_PRAYER_ASR, "أذان صلاة العصر", "تنبيهات وأذان العصر عند دخول الوقت"),
                Triple(AlarmReceiver.CHANNEL_PRAYER_MAGHRIB, "أذان صلاة المغرب", "تنبيهات وأذان المغرب عند غروب الشمس"),
                Triple(AlarmReceiver.CHANNEL_PRAYER_ISHA, "أذان صلاة العشاء", "تنبيهات وأذان العشاء وسنة الوتر")
            )

            for ((channelId, name, desc) in prayerChannels) {
                val channel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = desc
                    enableVibration(true)
                    vibrationPattern = strongVibration
                    setSound(prayerSoundUri, audioAttributesAlarm)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                manager.createNotificationChannel(channel)
            }

            // General Prayer Channel fallback
            val generalPrayerChannel = NotificationChannel(
                AlarmReceiver.CHANNEL_PRAYER,
                "مواقيت الصلاة العامة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات مواعيد الصلوات الخمس"
                enableVibration(true)
                vibrationPattern = strongVibration
                setSound(prayerSoundUri, audioAttributesAlarm)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(generalPrayerChannel)

            // 2. Pre-Prayer Reminder Channel (تنبيه اقتراب موعد الصلاة)
            val preReminderChannel = NotificationChannel(
                AlarmReceiver.CHANNEL_PRE_PRAYER,
                "تذكير اقتراب موعد الصلاة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات الاستعداد للصلاة والوضوء قبل الأذان ببضع دقائق"
                enableVibration(true)
                vibrationPattern = gentleVibration
                setSound(adhkarSoundUri, audioAttributesNotification)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(preReminderChannel)

            // 3. Adhkar Channel (أذكار الصباح والمساء والقرآن)
            val adhkarChannel = NotificationChannel(
                AlarmReceiver.CHANNEL_ADHKAR,
                "أذكار الصباح والمساء والقرآن",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات أذكار الصباح والمساء والورد القرآني بنغمة هادئة"
                enableVibration(true)
                vibrationPattern = gentleVibration
                setSound(adhkarSoundUri, audioAttributesNotification)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(adhkarChannel)

            // 4. Academic Lectures & Study Tasks (المذاكرة والمحاضرات الجامعية)
            val studyChannel = NotificationChannel(
                AlarmReceiver.CHANNEL_STUDY,
                "المذاكرة والمحاضرات الجامعية",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات مواعيد محاضرات ومعامل وجلسات المذاكرة الأكاديمية"
                enableVibration(true)
                vibrationPattern = strongVibration
                setSound(lectureSoundUri, audioAttributesNotification)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(studyChannel)

            // 5. Workouts & Physical Fitness (التمارين الرياضية وتنشيط البدن)
            val workoutsChannel = NotificationChannel(
                AlarmReceiver.CHANNEL_WORKOUTS,
                "التمارين الرياضية وتنشيط البدن",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات التمارين اليومية وتنشيط الظهر والبطن والقدمين"
                enableVibration(true)
                vibrationPattern = strongVibration
                setSound(lectureSoundUri, audioAttributesNotification)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(workoutsChannel)

            // 6. Sleep & Rest (أذكار ومواعيد النوم)
            val sleepChannel = NotificationChannel(
                AlarmReceiver.CHANNEL_SLEEP,
                "أذكار ومواعيد النوم والراحة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات أذكار النوم والاستعداد للراحة"
                enableVibration(true)
                vibrationPattern = gentleVibration
                setSound(adhkarSoundUri, audioAttributesNotification)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(sleepChannel)

            // 7. Timer Alert Completion (اكتمال جلسة مؤقت المذاكرة)
            val timerAlertChannel = NotificationChannel(
                AlarmReceiver.CHANNEL_TIMER_ALERT,
                "اكتمال مؤقت المذاكرة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيه صوتي واهتزاز عند اكتمال جلسة البومودورو أو المؤقت"
                enableVibration(true)
                vibrationPattern = strongVibration
                setSound(studySoundUri, audioAttributesAlarm)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(timerAlertChannel)

            // 8. Ongoing Study Timer (مؤقت المذاكرة والتركيز المستمر)
            val timerOngoingChannel = NotificationChannel(
                StudyTimerService.CHANNEL_ID,
                "مؤقت المذاكرة المستمر",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "عرض حالة مؤقت المذاكرة والوقت المتبقي بشكل دائم"
                setShowBadge(false)
            }
            manager.createNotificationChannel(timerOngoingChannel)

            // 9. Full Adhan Background Service Channel
            val adhanServiceChannel = NotificationChannel(
                AdhanPlayerService.CHANNEL_ID,
                "خدمة تشغيل الأذان في الخلفية",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "خدمة تشغيل صوت الأذان كاملاً مع زر إيقاف فوري"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(adhanServiceChannel)

            // 10. Dedicated Silent Alert Channel (No sound, vibration only)
            val silentChannel = NotificationChannel(
                AlarmReceiver.CHANNEL_SILENT,
                "تنبيهات صامتة واهتزاز فقط",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات منبثقة واهتزاز بدون صوت عند اختيار التنبيه الصامت"
                enableVibration(true)
                vibrationPattern = strongVibration
                setSound(null, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(silentChannel)
        }
    }
}
