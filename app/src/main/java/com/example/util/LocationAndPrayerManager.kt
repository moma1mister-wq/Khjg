package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import com.example.data.local.AppDatabase
import com.example.receiver.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

data class CityInfo(
    val id: String,
    val arabicName: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String = "Africa/Cairo"
)

enum class CalculationMethod(
    val id: String,
    val arabicName: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaMinutesAfterMaghrib: Int = 0
) {
    EGYPTIAN("egyptian", "الهيئة العامة للمساحة (مصر)", 19.5, 17.5),
    UMM_AL_QURA("umm_al_qura", "جامعة أم القرى (مكة المكرمة)", 18.5, 0.0, ishaMinutesAfterMaghrib = 90),
    MUSLIM_WORLD_LEAGUE("mwl", "رابطة العالم الإسلامي", 18.0, 17.0),
    ISNA("isna", "الجمعية الإسلامية لأمريكا الشمالية (ISNA)", 15.0, 15.0),
    KARACHI("karachi", "جامعة العلوم الإسلامية بكراتشي", 18.0, 18.0),
    GULF("gulf", "توقيت الخليج العربي", 19.5, 0.0, ishaMinutesAfterMaghrib = 90)
}

enum class JuristicSchool(
    val id: String,
    val arabicName: String,
    val shadowFactor: Double
) {
    STANDARD("standard", "جمهور العلماء (الشافعي، المالكي، الحنبلي)", 1.0),
    HANAFI("hanafi", "المذهب الحنفي", 2.0)
}

data class PrayerTimesResult(
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val cityName: String,
    val isAuto: Boolean,
    val isDstActive: Boolean,
    val timeZoneDisplayName: String,
    val calculationMethodName: String = CalculationMethod.EGYPTIAN.arabicName,
    val juristicSchoolName: String = JuristicSchool.STANDARD.arabicName
)

object LocationAndPrayerManager {

    private const val PREFS_NAME = "location_prayer_prefs"
    private const val KEY_CITY_ID = "selected_city_id"
    private const val KEY_IS_AUTO = "is_auto_location"
    private const val KEY_CUSTOM_LAT = "custom_lat"
    private const val KEY_CUSTOM_LNG = "custom_lng"
    private const val KEY_CUSTOM_NAME = "custom_name"
    private const val KEY_CALC_METHOD = "calc_method_id"
    private const val KEY_JURISTIC_SCHOOL = "juristic_school_id"

    // Minute offsets
    private const val KEY_OFFSET_FAJR = "offset_fajr"
    private const val KEY_OFFSET_DHUHR = "offset_dhuhr"
    private const val KEY_OFFSET_ASR = "offset_asr"
    private const val KEY_OFFSET_MAGHRIB = "offset_maghrib"
    private const val KEY_OFFSET_ISHA = "offset_isha"

    val EGYPTIAN_CITIES = listOf(
        CityInfo("cairo", "القاهرة", "مصر", 30.0444, 31.2357, "Africa/Cairo"),
        CityInfo("giza", "الجيزة", "مصر", 30.0131, 31.2089, "Africa/Cairo"),
        CityInfo("alexandria", "الإسكندرية", "مصر", 31.2001, 29.9187, "Africa/Cairo"),
        CityInfo("tanta", "طنطا (الغربية)", "مصر", 30.7865, 31.0004, "Africa/Cairo"),
        CityInfo("mahalla", "المحلة الكبرى (الغربية)", "مصر", 30.9706, 31.1669, "Africa/Cairo"),
        CityInfo("mansoura", "المنصورة (الدقهلية)", "مصر", 31.0409, 31.3785, "Africa/Cairo"),
        CityInfo("zagazig", "الزقازيق (الشرقية)", "مصر", 30.5765, 31.5041, "Africa/Cairo"),
        CityInfo("banha", "بنها (القليوبية)", "مصر", 30.4660, 31.1853, "Africa/Cairo"),
        CityInfo("shibin", "شبين الكوم (المنوفية)", "مصر", 30.5526, 31.0090, "Africa/Cairo"),
        CityInfo("damanhour", "دمنهور (البحيرة)", "مصر", 31.0404, 30.4700, "Africa/Cairo"),
        CityInfo("kafr_sheikh", "كفر الشيخ", "مصر", 31.1107, 30.9388, "Africa/Cairo"),
        CityInfo("damietta", "دمياط", "مصر", 31.4175, 31.8144, "Africa/Cairo"),
        CityInfo("port_said", "بورسعيد", "مصر", 31.2653, 32.3019, "Africa/Cairo"),
        CityInfo("ismailia", "الإسماعيلية", "مصر", 30.5965, 32.2715, "Africa/Cairo"),
        CityInfo("suez", "السويس", "مصر", 29.9668, 32.5498, "Africa/Cairo"),
        CityInfo("fayoum", "الفيوم", "مصر", 29.3084, 30.8428, "Africa/Cairo"),
        CityInfo("beni_suef", "بني سويف", "مصر", 29.0661, 31.0994, "Africa/Cairo"),
        CityInfo("minya", "المنيا", "مصر", 28.1099, 30.7503, "Africa/Cairo"),
        CityInfo("asyut", "أسيوط", "مصر", 27.1783, 31.1859, "Africa/Cairo"),
        CityInfo("sohag", "سوهاج", "مصر", 26.5590, 31.6957, "Africa/Cairo"),
        CityInfo("qena", "قنا", "مصر", 26.1551, 32.7160, "Africa/Cairo"),
        CityInfo("luxor", "الأقصر", "مصر", 25.6872, 32.6396, "Africa/Cairo"),
        CityInfo("aswan", "أسوان", "مصر", 24.0889, 32.8998, "Africa/Cairo"),
        CityInfo("hurghada", "الغردقة (البحر الأحمر)", "مصر", 27.2579, 33.8116, "Africa/Cairo"),
        CityInfo("sharm", "شرم الشيخ (جنوب سيناء)", "مصر", 27.9158, 34.3299, "Africa/Cairo"),
        CityInfo("tor", "طور سيناء", "مصر", 28.2393, 33.6247, "Africa/Cairo"),
        CityInfo("arish", "العريش (شمال سيناء)", "مصر", 31.1316, 33.7984, "Africa/Cairo"),
        CityInfo("matruh", "مرسى مطروح", "مصر", 31.3543, 27.2373, "Africa/Cairo"),
        CityInfo("kharga", "الخارجة (الوادي الجديد)", "مصر", 25.4402, 30.5469, "Africa/Cairo")
    )

    val ARAB_CAPITALS = listOf(
        CityInfo("makkah", "مكة المكرمة", "المملكة العربية السعودية", 21.4225, 39.8262, "Asia/Riyadh"),
        CityInfo("madinah", "المدينة المنورة", "المملكة العربية السعودية", 24.5247, 39.5692, "Asia/Riyadh"),
        CityInfo("riyadh", "الرياض", "المملكة العربية السعودية", 24.7136, 46.6753, "Asia/Riyadh"),
        CityInfo("jeddah", "جدة", "المملكة العربية السعودية", 21.4858, 39.1925, "Asia/Riyadh"),
        CityInfo("jerusalem", "القدس الشريف", "فلسطين", 31.7683, 35.2137, "Asia/Jerusalem"),
        CityInfo("amman", "عمان", "الأردن", 31.9454, 35.9284, "Asia/Amman"),
        CityInfo("damascus", "دمشق", "سوريا", 33.5138, 36.2765, "Asia/Damascus"),
        CityInfo("beirut", "بيروت", "لبنان", 33.8938, 35.5018, "Asia/Beirut"),
        CityInfo("baghdad", "بغداد", "العراق", 33.3152, 44.3661, "Asia/Baghdad"),
        CityInfo("kuwait", "مدينة الكويت", "الكويت", 29.3759, 47.9774, "Asia/Kuwait"),
        CityInfo("doha", "الدوحة", "قطر", 25.2854, 51.5310, "Asia/Qatar"),
        CityInfo("abudhabi", "أبوظبي", "الإمارات العربية المتحدة", 24.4539, 54.3773, "Asia/Dubai"),
        CityInfo("dubai", "دبي", "الإمارات العربية المتحدة", 25.2048, 55.2708, "Asia/Dubai"),
        CityInfo("manama", "المنامة", "البحرين", 26.2285, 50.5860, "Asia/Bahrain"),
        CityInfo("muscat", "مسقط", "عُمان", 23.5880, 58.3829, "Asia/Muscat"),
        CityInfo("sanaa", "صنعاء", "اليمن", 15.3694, 44.1910, "Asia/Aden"),
        CityInfo("khartoum", "الخرطوم", "السودان", 15.5007, 32.5599, "Africa/Khartoum"),
        CityInfo("tripoli", "طرابلس", "ليبيا", 32.8872, 13.1913, "Africa/Tripoli"),
        CityInfo("tunis", "تونس", "تونس", 36.8065, 10.1815, "Africa/Tunis"),
        CityInfo("algiers", "الجزائر", "الجزائر", 36.7538, 3.0588, "Africa/Algiers"),
        CityInfo("rabat", "الرباط", "المغرب", 34.0209, -6.8416, "Africa/Casablanca")
    )

    val ALL_CITIES = EGYPTIAN_CITIES + ARAB_CAPITALS

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isAutoLocation(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_AUTO, false)
    }

    fun setAutoLocation(context: Context, isAuto: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_AUTO, isAuto).apply()
    }

    fun getSelectedCity(context: Context): CityInfo {
        val prefs = getPrefs(context)
        val isAuto = prefs.getBoolean(KEY_IS_AUTO, false)
        if (isAuto && prefs.contains(KEY_CUSTOM_LAT)) {
            val name = prefs.getString(KEY_CUSTOM_NAME, "موقعي الحالي") ?: "موقعي الحالي"
            val lat = prefs.getFloat(KEY_CUSTOM_LAT, 30.0444f).toDouble()
            val lng = prefs.getFloat(KEY_CUSTOM_LNG, 31.2357f).toDouble()
            return CityInfo("custom_auto", name, "تحديد تلقائي", lat, lng, TimeZone.getDefault().id)
        }
        val cityId = prefs.getString(KEY_CITY_ID, "cairo") ?: "cairo"
        return ALL_CITIES.find { it.id == cityId } ?: EGYPTIAN_CITIES.first()
    }

    fun setSelectedCity(context: Context, cityId: String) {
        getPrefs(context).edit()
            .putString(KEY_CITY_ID, cityId)
            .putBoolean(KEY_IS_AUTO, false)
            .apply()
    }

    fun getCalculationMethod(context: Context): CalculationMethod {
        val raw = getPrefs(context).getString(KEY_CALC_METHOD, CalculationMethod.EGYPTIAN.id)
        return CalculationMethod.values().find { it.id == raw } ?: CalculationMethod.EGYPTIAN
    }

    fun setCalculationMethod(context: Context, method: CalculationMethod) {
        getPrefs(context).edit().putString(KEY_CALC_METHOD, method.id).apply()
    }

    fun getJuristicSchool(context: Context): JuristicSchool {
        val raw = getPrefs(context).getString(KEY_JURISTIC_SCHOOL, JuristicSchool.STANDARD.id)
        return JuristicSchool.values().find { it.id == raw } ?: JuristicSchool.STANDARD
    }

    fun setJuristicSchool(context: Context, school: JuristicSchool) {
        getPrefs(context).edit().putString(KEY_JURISTIC_SCHOOL, school.id).apply()
    }

    fun getOffsets(context: Context): Map<String, Int> {
        val prefs = getPrefs(context)
        return mapOf(
            "Fajr" to prefs.getInt(KEY_OFFSET_FAJR, 0),
            "Dhuhr" to prefs.getInt(KEY_OFFSET_DHUHR, 0),
            "Asr" to prefs.getInt(KEY_OFFSET_ASR, 0),
            "Maghrib" to prefs.getInt(KEY_OFFSET_MAGHRIB, 0),
            "Isha" to prefs.getInt(KEY_OFFSET_ISHA, 0)
        )
    }

    fun setOffset(context: Context, prayerKey: String, offsetMinutes: Int) {
        val key = when (prayerKey) {
            "Fajr" -> KEY_OFFSET_FAJR
            "Dhuhr" -> KEY_OFFSET_DHUHR
            "Asr" -> KEY_OFFSET_ASR
            "Maghrib" -> KEY_OFFSET_MAGHRIB
            "Isha" -> KEY_OFFSET_ISHA
            else -> return
        }
        getPrefs(context).edit().putInt(key, offsetMinutes).apply()
    }

    /**
     * Attempts to acquire current location using Android LocationManager (Network or GPS provider).
     */
    @SuppressLint("MissingPermission")
    suspend fun detectLocationAndSetCity(context: Context): Result<CityInfo> = withContext(Dispatchers.IO) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return@withContext Result.failure(Exception("خدمة الموقع غير متاحة في النظام"))

            var location: Location? = null
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
            if (location == null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
            if (location == null && locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            }

            if (location == null) {
                return@withContext Result.failure(Exception("تعذر استلام إحداثيات الموقع الحالية، يرجى تفعيل الموقع في الهاتف أو اختيار المدينة يدويًا"))
            }

            val lat = location.latitude
            val lng = location.longitude
            var resolvedName = "موقعي الحالي"

            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(context, Locale.forLanguageTag("ar"))
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                        val country = addr.countryName ?: ""
                        if (!locality.isNullOrBlank()) {
                            resolvedName = "$locality ($country)"
                        }
                    }
                }
            } catch (_: Exception) {}

            getPrefs(context).edit()
                .putBoolean(KEY_IS_AUTO, true)
                .putFloat(KEY_CUSTOM_LAT, lat.toFloat())
                .putFloat(KEY_CUSTOM_LNG, lng.toFloat())
                .putString(KEY_CUSTOM_NAME, resolvedName)
                .apply()

            val detectedCity = CityInfo("custom_auto", resolvedName, "مصر", lat, lng, TimeZone.getDefault().id)
            Result.success(detectedCity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calculates prayer times with support for selectable CalculationMethod and JuristicSchool
     */
    fun calculatePrayerTimes(
        context: Context,
        date: Date = Date(),
        city: CityInfo? = null
    ): PrayerTimesResult {
        val currentCity = city ?: getSelectedCity(context)
        val isAuto = isAutoLocation(context)
        val offsets = getOffsets(context)
        val method = getCalculationMethod(context)
        val school = getJuristicSchool(context)

        val cal = Calendar.getInstance().apply { time = date }
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val year = cal.get(Calendar.YEAR)

        val tz = if (isAuto) {
            TimeZone.getDefault()
        } else {
            try {
                TimeZone.getTimeZone(currentCity.timeZoneId)
            } catch (_: Exception) {
                TimeZone.getDefault()
            }
        }
        val rawOffsetHours = tz.getOffset(date.time) / (1000.0 * 3600.0)
        val isDst = tz.inDaylightTime(date)

        val latRad = Math.toRadians(currentCity.latitude)

        val b = 2.0 * Math.PI * (dayOfYear - 81) / 365.0
        val eot = 9.87 * sin(2.0 * b) - 7.53 * cos(b) - 1.5 * sin(b)
        val delta = Math.toRadians(23.45 * sin(Math.toRadians((360.0 / 365.0) * (dayOfYear - 81))))

        val solarNoonMinutes = 720.0 - (4.0 * currentCity.longitude) - eot + (rawOffsetHours * 60.0)
        val solarNoonHours = solarNoonMinutes / 60.0

        fun hourAngle(altitudeDeg: Double): Double {
            val altRad = Math.toRadians(altitudeDeg)
            val cosHA = (sin(altRad) - sin(latRad) * sin(delta)) / (cos(latRad) * cos(delta))
            val clamped = cosHA.coerceIn(-1.0, 1.0)
            return Math.toDegrees(acos(clamped)) / 15.0
        }

        // Fajr based on calculation method angle
        val haFajr = hourAngle(-method.fajrAngle)
        val fajrHours = solarNoonHours - haFajr

        // Sunrise
        val haSunrise = hourAngle(-0.833)
        val sunriseHours = solarNoonHours - haSunrise

        // Dhuhr: Solar noon + buffer
        val dhuhrHours = solarNoonHours + (2.0 / 60.0)

        // Asr based on juristic school shadow factor
        val asrAltRad = atan(1.0 / (school.shadowFactor + tan(Math.abs(latRad - delta))))
        val haAsr = hourAngle(Math.toDegrees(asrAltRad))
        val asrHours = solarNoonHours + haAsr

        // Maghrib: Sunset
        val maghribHours = solarNoonHours + haSunrise

        // Isha based on calculation method angle or fixed minutes
        val ishaHours = if (method.ishaMinutesAfterMaghrib > 0) {
            maghribHours + (method.ishaMinutesAfterMaghrib / 60.0)
        } else {
            val haIsha = hourAngle(-method.ishaAngle)
            solarNoonHours + haIsha
        }

        fun formatWithOffset(hours: Double, offsetMinutes: Int): String {
            var totalMin = (hours * 60.0).toInt() + offsetMinutes
            while (totalMin < 0) totalMin += 1440
            totalMin %= 1440
            val h = totalMin / 60
            val m = totalMin % 60
            return String.format(Locale.US, "%02d:%02d", h, m)
        }

        val fajrTime = formatWithOffset(fajrHours, offsets["Fajr"] ?: 0)
        val sunriseTime = formatWithOffset(sunriseHours, 0)
        val dhuhrTime = formatWithOffset(dhuhrHours, offsets["Dhuhr"] ?: 0)
        val asrTime = formatWithOffset(asrHours, offsets["Asr"] ?: 0)
        val maghribTime = formatWithOffset(maghribHours, offsets["Maghrib"] ?: 0)
        val ishaTime = formatWithOffset(ishaHours, offsets["Isha"] ?: 0)

        val tzName = if (isDst) "${tz.displayName} (توقيت صيفي مفعل)" else tz.displayName

        return PrayerTimesResult(
            fajr = fajrTime,
            sunrise = sunriseTime,
            dhuhr = dhuhrTime,
            asr = asrTime,
            maghrib = maghribTime,
            isha = ishaTime,
            cityName = currentCity.arabicName,
            isAuto = isAuto,
            isDstActive = isDst,
            timeZoneDisplayName = tzName,
            calculationMethodName = method.arabicName,
            juristicSchoolName = school.arabicName
        )
    }

    suspend fun syncPrayerTimesToTasks(context: Context) = withContext(Dispatchers.IO) {
        try {
            val prayers = calculatePrayerTimes(context)
            val db = AppDatabase.getInstance(context)
            val existingTasks = db.taskDao().getTasksWithRemindersSync()

            val prayerMappings = listOf(
                Pair("صلاة الفجر والورد الصباحي", prayers.fajr),
                Pair("صلاة الظهر", prayers.dhuhr),
                Pair("صلاة العصر", prayers.asr),
                Pair("صلاة المغرب", prayers.maghrib),
                Pair("صلاة العشاء وسنة الوتر", prayers.isha)
            )

            for ((title, newTime) in prayerMappings) {
                val task = existingTasks.find { it.title.startsWith(title.substring(0, 7)) }
                if (task != null) {
                    val updated = task.copy(targetTime = newTime)
                    db.taskDao().updateTask(updated)
                }
            }

            AlarmScheduler.scheduleAllPendingAlarms(context)
        } catch (_: Exception) {}
    }
}
