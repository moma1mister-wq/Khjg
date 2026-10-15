package com.example.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TaskAlarmDiagnostic(
    val taskId: Int,
    val taskTitle: String,
    val category: String,
    val targetTime: String,
    val nextTriggerMillis: Long,
    val nextTriggerFormatted: String,
    val isExactAllowed: Boolean,
    val scheduleSuccess: Boolean,
    val lastScheduledAt: Long,
    val lastError: String? = null,
    val alarmReceivedAt: Long? = null,
    val notificationPostedAt: Long? = null,
    val statusText: String = "مجدول وفي انتظار الموعد"
)

object AlarmDiagnosticStore {

    private const val PREFS_NAME = "alarm_diagnostics_prefs"
    private const val KEY_RECORDS = "records_json"
    private const val KEY_TWO_MIN_TEST = "two_minute_test_target"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun recordSchedule(
        context: Context,
        taskId: Int,
        taskTitle: String,
        category: String,
        targetTime: String,
        nextTriggerMillis: Long,
        isExactAllowed: Boolean,
        scheduleSuccess: Boolean,
        lastError: String? = null
    ) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.forLanguageTag("ar"))
        val nextFormatted = dateFormat.format(Date(nextTriggerMillis))

        val existing = getDiagnostics(context).toMutableList()
        val index = existing.indexOfFirst { it.taskId == taskId }

        val newRecord = TaskAlarmDiagnostic(
            taskId = taskId,
            taskTitle = taskTitle,
            category = category,
            targetTime = targetTime,
            nextTriggerMillis = nextTriggerMillis,
            nextTriggerFormatted = nextFormatted,
            isExactAllowed = isExactAllowed,
            scheduleSuccess = scheduleSuccess,
            lastScheduledAt = System.currentTimeMillis(),
            lastError = lastError,
            alarmReceivedAt = if (index >= 0) existing[index].alarmReceivedAt else null,
            notificationPostedAt = if (index >= 0) existing[index].notificationPostedAt else null,
            statusText = if (scheduleSuccess) "تمت الجدولة بنجاح في المنبه الأصلي" else "فشلت الجدولة: $lastError"
        )

        if (index >= 0) {
            existing[index] = newRecord
        } else {
            existing.add(newRecord)
        }

        saveDiagnostics(context, existing)
    }

    fun recordAlarmReceived(context: Context, taskId: Int, notificationPosted: Boolean) {
        val existing = getDiagnostics(context).toMutableList()
        val index = existing.indexOfFirst { it.taskId == taskId }
        val now = System.currentTimeMillis()

        if (index >= 0) {
            val old = existing[index]
            existing[index] = old.copy(
                alarmReceivedAt = now,
                notificationPostedAt = if (notificationPosted) now else null,
                statusText = if (notificationPosted) "تم استلام التنبيه وإطلاق الإشعار والصوت ✓" else "تم استلام الحدث مع تعذر إظهار الإشعار"
            )
            saveDiagnostics(context, existing)
        }
    }

    fun getDiagnostics(context: Context): List<TaskAlarmDiagnostic> {
        val jsonStr = getPrefs(context).getString(KEY_RECORDS, "[]") ?: "[]"
        val list = mutableListOf<TaskAlarmDiagnostic>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    TaskAlarmDiagnostic(
                        taskId = obj.optInt("taskId"),
                        taskTitle = obj.optString("taskTitle"),
                        category = obj.optString("category"),
                        targetTime = obj.optString("targetTime"),
                        nextTriggerMillis = obj.optLong("nextTriggerMillis"),
                        nextTriggerFormatted = obj.optString("nextTriggerFormatted"),
                        isExactAllowed = obj.optBoolean("isExactAllowed"),
                        scheduleSuccess = obj.optBoolean("scheduleSuccess"),
                        lastScheduledAt = obj.optLong("lastScheduledAt"),
                        lastError = if (obj.has("lastError") && !obj.isNull("lastError")) obj.optString("lastError") else null,
                        alarmReceivedAt = if (obj.has("alarmReceivedAt")) obj.optLong("alarmReceivedAt") else null,
                        notificationPostedAt = if (obj.has("notificationPostedAt")) obj.optLong("notificationPostedAt") else null,
                        statusText = obj.optString("statusText", "مجدول")
                    )
                )
            }
        } catch (_: Exception) {
        }
        return list
    }

    private fun saveDiagnostics(context: Context, list: List<TaskAlarmDiagnostic>) {
        val jsonArray = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("taskId", item.taskId)
                put("taskTitle", item.taskTitle)
                put("category", item.category)
                put("targetTime", item.targetTime)
                put("nextTriggerMillis", item.nextTriggerMillis)
                put("nextTriggerFormatted", item.nextTriggerFormatted)
                put("isExactAllowed", item.isExactAllowed)
                put("scheduleSuccess", item.scheduleSuccess)
                put("lastScheduledAt", item.lastScheduledAt)
                put("lastError", item.lastError)
                item.alarmReceivedAt?.let { put("alarmReceivedAt", it) }
                item.notificationPostedAt?.let { put("notificationPostedAt", it) }
                put("statusText", item.statusText)
            }
            jsonArray.put(obj)
        }
        getPrefs(context).edit().putString(KEY_RECORDS, jsonArray.toString()).apply()
    }

    fun setTwoMinuteTestTarget(context: Context, targetMillis: Long) {
        getPrefs(context).edit().putLong(KEY_TWO_MIN_TEST, targetMillis).apply()
    }

    fun getTwoMinuteTestTarget(context: Context): Long {
        return getPrefs(context).getLong(KEY_TWO_MIN_TEST, 0L)
    }

    fun clearDiagnostics(context: Context) {
        getPrefs(context).edit().remove(KEY_RECORDS).remove(KEY_TWO_MIN_TEST).apply()
    }
}
