package com.example.util

import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.R

object SoundHelper {

    private var activePlayer: MediaPlayer? = null

    fun getAudioUri(context: Context, rawResId: Int): Uri {
        return Uri.parse(
            "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/$rawResId"
        )
    }

    /**
     * Bulletproof audio playback using MediaPlayer.create with AudioAttributes
     * ensuring proper volume stream and error handling.
     */
    fun playSound(context: Context, rawResId: Int, isAlarm: Boolean = false) {
        try {
            stopSound()

            val appContext = context.applicationContext

            // Ensure audio is not muted on emulator / device
            ensureAudioUnmuted(appContext, isAlarm)

            val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(if (isAlarm) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA)
                .build()

            val player = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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
                    try {
                        it.release()
                    } catch (_: Exception) {}
                    if (activePlayer == it) {
                        activePlayer = null
                    }
                }
                player.setOnErrorListener { _, _, _ ->
                    stopSound()
                    fallbackSystemRingtone(appContext, isAlarm)
                    true
                }
                player.start()
                activePlayer = player
            } else {
                fallbackSystemRingtone(appContext, isAlarm)
            }
        } catch (_: Exception) {
            fallbackSystemRingtone(context, isAlarm)
        }
    }

    private fun ensureAudioUnmuted(context: Context, isAlarm: Boolean) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val streamType = if (isAlarm) AudioManager.STREAM_ALARM else AudioManager.STREAM_MUSIC
            val currentVol = audioManager.getStreamVolume(streamType)
            val maxVol = audioManager.getStreamMaxVolume(streamType)
            if (currentVol == 0 && maxVol > 0) {
                audioManager.setStreamVolume(streamType, (maxVol * 0.85).toInt().coerceAtLeast(1), 0)
            }
        } catch (_: Exception) {}
    }

    private fun fallbackSystemRingtone(context: Context, isAlarm: Boolean) {
        try {
            val type = if (isAlarm) RingtoneManager.TYPE_ALARM else RingtoneManager.TYPE_NOTIFICATION
            val ringtoneUri = RingtoneManager.getDefaultUri(type)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, ringtoneUri)
            ringtone?.play()
        } catch (_: Exception) {}
    }

    fun vibrate(context: Context, isAlarm: Boolean = true) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return

            val timings = if (isAlarm) {
                longArrayOf(0, 500, 200, 500, 200, 800)
            } else {
                longArrayOf(0, 300, 150, 300)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(timings, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, -1)
            }
        } catch (_: Exception) {}
    }

    fun stopSound() {
        try {
            activePlayer?.stop()
            activePlayer?.release()
        } catch (_: Exception) {} finally {
            activePlayer = null
        }
    }
}
