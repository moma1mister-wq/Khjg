package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Utility for formatting and normalizing time representations.
 * Strictly separates internal storage (normalized 24-hour "HH:mm" and Unix timestamps)
 * from user display (12-hour format with Arabic "صباحًا" and "مساءً").
 */
object TimeFormatHelper {

    /**
     * Converts normalized 24-hour time "HH:mm" into user-facing 12-hour format with Arabic labels.
     * Examples:
     * - "05:00" -> "5:00 ص" or "5:00 صباحًا"
     * - "12:15" -> "12:15 م" or "12:15 مساءً"
     * - "17:30" -> "5:30 م" or "5:30 مساءً"
     * - "00:45" -> "12:45 ص" or "12:45 صباحًا"
     */
    fun format12Hour(time24: String, fullArabicText: Boolean = false): String {
        val normalized = normalizeTime(time24)
        val parts = normalized.split(":")
        if (parts.size < 2) return time24

        val hour24 = parts[0].toIntOrNull() ?: return time24
        val minute = parts[1].toIntOrNull() ?: return time24

        val isPm = hour24 >= 12
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }

        val suffix = if (fullArabicText) {
            when {
                hour24 == 12 && minute == 0 -> "ظهرًا"
                hour24 == 0 && minute == 0 -> "منتصف الليل"
                isPm -> "مساءً"
                else -> "صباحًا"
            }
        } else {
            if (isPm) "م" else "ص"
        }

        val minuteFormatted = String.format(Locale.US, "%02d", minute)
        return "$hour12:$minuteFormatted $suffix"
    }

    /**
     * Normalizes any input time string by converting Eastern Arabic / Persian numerals
     * (٠١٢٣٤٥٦٧٨٩) to standard ASCII digits (0-9), trimming, and ensuring "HH:mm" format.
     */
    fun normalizeTime(input: String): String {
        val trimmed = input.trim()
        val sb = StringBuilder()
        for (ch in trimmed) {
            when (ch) {
                in '٠'..'٩' -> sb.append(ch - '٠')
                in '۰'..'۹' -> sb.append(ch - '۰')
                in '0'..'9', ':' -> sb.append(ch)
            }
        }
        val clean = sb.toString()
        val parts = clean.split(":")
        if (parts.size >= 2) {
            val h = parts[0].toIntOrNull() ?: 12
            val m = parts[1].toIntOrNull() ?: 0
            return String.format(Locale.US, "%02d:%02d", h.coerceIn(0, 23), m.coerceIn(0, 59))
        }
        return "12:00"
    }

    /**
     * Formats a millisecond timestamp to a clear 12-hour Arabic time string.
     */
    fun formatTimestamp12Hour(timestampMillis: Long): String {
        val date = Date(timestampMillis)
        val cal = Calendar.getInstance().apply { time = date }
        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val second = cal.get(Calendar.SECOND)

        val isPm = hour24 >= 12
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val suffix = if (isPm) "مساءً" else "صباحًا"
        val minSec = String.format(Locale.US, "%02d:%02d", minute, second)
        val datePart = SimpleDateFormat("dd MMMM", Locale.forLanguageTag("ar")).format(date)
        return "$hour12:$minSec $suffix ($datePart)"
    }

    /**
     * Returns full date and day in Arabic.
     */
    fun formatArabicFullDate(date: Date = Date()): String {
        return SimpleDateFormat("EEEE، d MMMM yyyy", Locale.forLanguageTag("ar")).format(date)
    }
}
