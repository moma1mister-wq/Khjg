package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.util.SoundHelper

class AdhanPlayerService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_ADHAN) {
            stopAdhan()
            return START_NOT_STICKY
        }

        val prayerName = intent?.getStringExtra(EXTRA_PRAYER_NAME) ?: "الصلاة"
        val rawResId = intent?.getIntExtra(EXTRA_RAW_RES_ID, R.raw.sarab_tone) ?: R.raw.sarab_tone

        startAdhanPlayback(prayerName, rawResId)
        return START_NOT_STICKY
    }

    @SuppressLint("WakelockTimeout")
    private fun startAdhanPlayback(prayerName: String, rawResId: Int) {
        stopAdhan()

        // 1. Post Foreground notification with Stop Action immediately
        val notification = buildAdhanNotification(prayerName)
        startForeground(NOTIFICATION_ID, notification)

        // 2. Acquire wake lock to keep CPU active during full audio playback
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "StudentCompanion:AdhanPlayerWakeLock"
        )?.apply {
            acquire(300_000L) // 5 minutes max timeout
        }

        // 3. Request Audio Focus
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val afr = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        stopAdhan()
                    }
                }
                .build()
            audioFocusRequest = afr
            audioManager?.requestAudioFocus(afr)
        }

        // 4. Ensure alarm stream is unmuted
        try {
            val alarmVol = audioManager?.getStreamVolume(AudioManager.STREAM_ALARM) ?: 0
            val maxAlarm = audioManager?.getStreamMaxVolume(AudioManager.STREAM_ALARM) ?: 15
            if (alarmVol == 0 && maxAlarm > 0) {
                audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, (maxAlarm * 0.85).toInt().coerceAtLeast(1), 0)
            }
        } catch (_: Exception) {}

        // 5. Trigger vibration
        SoundHelper.vibrate(this, isAlarm = true)

        // 6. Play full audio safely
        try {
            val player = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    MediaPlayer.create(applicationContext, rawResId, audioAttributes, 0)
                } catch (_: Exception) {
                    MediaPlayer.create(applicationContext, rawResId)
                }
            } else {
                MediaPlayer.create(applicationContext, rawResId)
            } ?: MediaPlayer.create(applicationContext, rawResId)

            if (player != null) {
                player.setVolume(1.0f, 1.0f)
                player.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                player.setOnCompletionListener {
                    stopAdhan()
                }
                player.setOnErrorListener { _, _, _ ->
                    stopAdhan()
                    true
                }
                player.start()
                mediaPlayer = player
            } else {
                stopAdhan()
            }
        } catch (_: Exception) {
            stopAdhan()
        }
    }

    private fun stopAdhan() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Exception) {} finally {
            mediaPlayer = null
        }

        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            }
        } catch (_: Exception) {}

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {} finally {
            wakeLock = null
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildAdhanNotification(prayerName: String): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_NAV_TARGET", "tasks")
        }
        val tapPending = PendingIntent.getActivity(
            this,
            101,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AdhanPlayerService::class.java).apply {
            action = ACTION_STOP_ADHAN
        }
        val stopPending = PendingIntent.getService(
            this,
            102,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("حيّ على الصلاة - حان موعد $prayerName 🕌")
            .setContentText("الأذان قيد التشغيل كاملاً • اضغط لإيقاف الصوت")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(tapPending)
            .addAction(android.R.drawable.ic_media_pause, "إيقاف الأذان ⏹", stopPending)
            .setSound(null) // Prevent duplicate sound from channel
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تشغيل الأذان في الخلفية",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "خدمة تشغيل صوت الأذان كاملاً مع زر إيقاف فوري"
                setSound(null, null) // No sound here to avoid duplicate audio
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopAdhan()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "adhan_player_service_channel"
        const val NOTIFICATION_ID = 5001

        const val ACTION_PLAY_ADHAN = "com.example.action.PLAY_ADHAN"
        const val ACTION_STOP_ADHAN = "com.example.action.STOP_ADHAN"

        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_RAW_RES_ID = "extra_raw_res_id"

        fun start(context: Context, prayerName: String, rawResId: Int) {
            val intent = Intent(context, AdhanPlayerService::class.java).apply {
                action = ACTION_PLAY_ADHAN
                putExtra(EXTRA_PRAYER_NAME, prayerName)
                putExtra(EXTRA_RAW_RES_ID, rawResId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AdhanPlayerService::class.java).apply {
                action = ACTION_STOP_ADHAN
            }
            context.startService(intent)
        }
    }
}
