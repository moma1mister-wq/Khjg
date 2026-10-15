package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.R

data class SoundOption(
    val id: String,
    val displayName: String,
    val rawResId: Int,
    val isFullAdhan: Boolean
)

enum class PrayerAlertMode(val id: String, val title: String, val subtitle: String) {
    FULL_ADHAN("full_adhan", "أذان كامل 🕌", "تشغيل صوت الأذان كاملاً في الخلفية عبر Foreground Service مع زر إيقاف فوري ⏹"),
    SHORT_TONE("short_tone", "تكبيرات / نغمة قصيرة 🔔", "إشعار منبثق Heads-up مع تكبيرات أو نغمة تنبيه سريعة"),
    SILENT("silent", "تنبيه صامت / اهتزاز فقط 📳", "إشعار منبثق مع اهتزاز منبه دون صوت"),
    DISABLED("disabled", "إيقاف التنبيه 🚫", "عدم إرسال أي إشعار أو تنبيه لهذه الصلاة")
}

object PrayerSoundManager {

    private const val PREFS_NAME = "prayer_sound_prefs"

    // High quality distinct sounds bundled with the app (including the custom uploaded Sarab nasheed)
    val SOUND_OPTIONS = listOf(
        SoundOption("sarab_tone", "نشيد سراب (فلا خبر ولا أمل يواسيني) 🎵", R.raw.sarab_tone, true),
        SoundOption("sarab_short", "نغمة سراب المختصرة (فلا خبر ولا أمل) 🔔", R.raw.sarab_short, false),
        SoundOption("adhan_standard", "أذان ندي كامل (صوت الأذان والتكبيرات)", R.raw.adhan_tone, true),
        SoundOption("takbeerat_short", "تكبيرات وتنبيه مختصر", R.raw.adhan_tone, false),
        SoundOption("adhkar_peace", "نغمة أذكار الصباح والمساء الهادئة", R.raw.adhkar_tone, false),
        SoundOption("lecture_alert", "نغمة تنبيه المحاضرات والمواعيد", R.raw.lecture_tone, false),
        SoundOption("study_bell", "رنين المذاكرة والإنجاز", R.raw.study_tone, false)
    )

    val PRAYER_KEYS = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

    private var previewPlayer: MediaPlayer? = null
    var currentlyPlayingResId: Int? = null
        private set

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getPrayerArabicName(prayerKey: String): String {
        return when (prayerKey) {
            "Fajr" -> "صلاة الفجر"
            "Dhuhr" -> "صلاة الظهر"
            "Asr" -> "صلاة العصر"
            "Maghrib" -> "صلاة المغرب"
            "Isha" -> "صلاة العشاء"
            else -> prayerKey
        }
    }

    fun getAlertMode(context: Context, prayerKey: String): PrayerAlertMode {
        val raw = getPrefs(context).getString("alert_mode_$prayerKey", PrayerAlertMode.FULL_ADHAN.id)
        return PrayerAlertMode.values().find { it.id == raw } ?: PrayerAlertMode.FULL_ADHAN
    }

    fun setAlertMode(context: Context, prayerKey: String, mode: PrayerAlertMode) {
        getPrefs(context).edit().putString("alert_mode_$prayerKey", mode.id).apply()
    }

    fun getSelectedSound(context: Context, prayerKey: String): SoundOption {
        val defaultSoundId = "sarab_tone"
        val soundId = getPrefs(context).getString("sound_id_$prayerKey", defaultSoundId)
        return SOUND_OPTIONS.find { it.id == soundId } ?: SOUND_OPTIONS.first()
    }

    fun setSelectedSound(context: Context, prayerKey: String, soundId: String) {
        getPrefs(context).edit().putString("sound_id_$prayerKey", soundId).apply()
    }

    fun getPreReminderMinutes(context: Context, prayerKey: String): Int {
        return getPrefs(context).getInt("pre_reminder_$prayerKey", 0) // 0 means disabled
    }

    fun setPreReminderMinutes(context: Context, prayerKey: String, minutes: Int) {
        getPrefs(context).edit().putInt("pre_reminder_$prayerKey", minutes).apply()
    }

    // Category settings (Adhkar, Sleep, Study, Workouts)
    fun getCategoryAlertMode(context: Context, categoryKey: String): PrayerAlertMode {
        val defaultMode = PrayerAlertMode.SHORT_TONE.id
        val raw = getPrefs(context).getString("category_mode_$categoryKey", defaultMode)
        return PrayerAlertMode.values().find { it.id == raw } ?: PrayerAlertMode.SHORT_TONE
    }

    fun setCategoryAlertMode(context: Context, categoryKey: String, mode: PrayerAlertMode) {
        getPrefs(context).edit().putString("category_mode_$categoryKey", mode.id).apply()
    }

    fun getCategorySound(context: Context, categoryKey: String): SoundOption {
        val defaultSoundId = when (categoryKey) {
            "adhkar" -> "sarab_short"
            "sleep" -> "adhkar_peace"
            "study" -> "sarab_short"
            "workout" -> "lecture_alert"
            "timer" -> "study_bell"
            else -> "sarab_short"
        }
        val soundId = getPrefs(context).getString("category_sound_$categoryKey", defaultSoundId)
        return SOUND_OPTIONS.find { it.id == soundId } ?: SOUND_OPTIONS.find { it.id == defaultSoundId } ?: SOUND_OPTIONS.first()
    }

    fun setCategorySound(context: Context, categoryKey: String, soundId: String) {
        getPrefs(context).edit().putString("category_sound_$categoryKey", soundId).apply()
    }

    fun isMasterNotificationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("master_notifications_enabled", true)
    }

    fun setMasterNotificationEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("master_notifications_enabled", enabled).apply()
    }

    /**
     * Preview sound inside settings before saving
     */
    fun playPreview(context: Context, rawResId: Int, onComplete: (() -> Unit)? = null) {
        stopPreview()
        try {
            currentlyPlayingResId = rawResId
            val appContext = context.applicationContext

            // Ensure media stream is audible
            try {
                val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                val currentVol = audioManager?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) ?: 0
                val maxVol = audioManager?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) ?: 15
                if (currentVol == 0 && maxVol > 0) {
                    audioManager?.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, (maxVol * 0.85).toInt().coerceAtLeast(1), 0)
                }
            } catch (_: Exception) {}

            val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()

            val player = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                try {
                    MediaPlayer.create(appContext, rawResId, attributes, 0)
                } catch (_: Exception) {
                    MediaPlayer.create(appContext, rawResId)
                }
            } else {
                MediaPlayer.create(appContext, rawResId)
            } ?: MediaPlayer.create(appContext, rawResId)

            if (player != null) {
                player.setVolume(1.0f, 1.0f)
                player.setOnCompletionListener {
                    stopPreview()
                    onComplete?.invoke()
                }
                player.setOnErrorListener { _, _, _ ->
                    stopPreview()
                    true
                }
                player.start()
                previewPlayer = player
            } else {
                stopPreview()
            }
        } catch (_: Exception) {
            stopPreview()
        }
    }

    fun isPreviewPlaying(): Boolean {
        return try {
            previewPlayer?.isPlaying == true
        } catch (_: Exception) {
            false
        }
    }

    fun stopPreview() {
        try {
            previewPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Exception) {} finally {
            previewPlayer = null
            currentlyPlayingResId = null
        }
    }
}
