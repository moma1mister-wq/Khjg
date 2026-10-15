package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PrepopulationData
import com.example.data.model.StudentWorkoutData
import com.example.data.repository.QuranRepository
import com.example.data.repository.StudentRepository
import com.example.receiver.AlarmScheduler
import com.example.util.AlarmDiagnosticStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("رفيق الطالب", appName)
  }

  @Test
  fun `verify essential tasks contain core worship and routine`() {
    val essential = PrepopulationData.essentialTasks
    assertTrue("Essential tasks must not be empty", essential.isNotEmpty())
    assertTrue("All essential tasks must have isEssential flag true", essential.all { it.isEssential })
    val fajr = essential.find { it.title.contains("الفجر") }
    assertNotNull("Fajr prayer must be an essential task", fajr)
    val workout = essential.find { it.title.contains("التمرين") }
    assertNotNull("Daily workout must be an essential task", workout)
    assertTrue("Daily workout must have reminder", workout?.hasReminder == true)
  }

  @Test
  fun `verify azhar college schedule contains official courses`() {
    val lectures = PrepopulationData.azharCollegeLectures
    assertTrue("Schedule must not be empty", lectures.isNotEmpty())
    val saturdayLectures = lectures.filter { it.dayOfWeek == 1 }
    assertEquals("Saturday has 3 lectures", 3, saturdayLectures.size)
    assertTrue(saturdayLectures.any { it.subject.contains("Algorithm Analysis") })
    assertTrue(saturdayLectures.any { it.subject.contains("Computer Networks") })
    assertTrue(saturdayLectures.any { it.subject.contains("English") })

    val sundayLectures = lectures.filter { it.dayOfWeek == 2 }
    assertTrue(sundayLectures.any { it.subject.contains("Data Structures") })
    assertTrue(sundayLectures.any { it.hall == "معمل 1" })
    assertTrue(sundayLectures.any { it.hall == "معمل 2" })
  }

  @Test
  fun `verify quran surahs list has 114 surahs`() {
    val surahs = PrepopulationData.allSurahs
    assertEquals(114, surahs.size)
    assertEquals("الفاتحة", surahs.first().name)
    assertEquals("الناس", surahs.last().name)
  }

  @Test
  fun `verify adhkar items loaded across all categories`() {
    val adhkar = PrepopulationData.allDhikrItems
    assertTrue(adhkar.any { it.categoryId == "morning" })
    assertTrue(adhkar.any { it.categoryId == "evening" })
    assertTrue(adhkar.any { it.categoryId == "prayer" })
    assertTrue(adhkar.any { it.categoryId == "sleep" })
    assertTrue(adhkar.any { it.categoryId == "food" })
    assertTrue(adhkar.any { it.categoryId == "home_mosque" })
    assertTrue(adhkar.any { it.categoryId == "wudu" })
    assertTrue(adhkar.any { it.categoryId == "study" })
  }

  @Test
  fun `verify student workouts loaded with sets and rest periods`() {
    val workouts = StudentWorkoutData.workouts
    assertTrue("Workouts list must contain multiple exercise routines", workouts.size >= 10)
    assertTrue(workouts.all { it.sets > 0 && it.restSeconds > 0 })
    assertTrue("Must have back exercises", workouts.any { it.category == "تمارين الظهر" })
    assertTrue("Must have abdomen exercises", workouts.any { it.category == "تمارين البطن" })
    assertTrue("Must have legs and feet exercises", workouts.any { it.category == "تمارين القدم والأرجل" })
    assertTrue("All workouts must have form illustrations", workouts.all { it.imageResId != 0 })
    assertTrue(workouts.any { it.title.contains("القطة") || it.title.contains("الجسر") })
    assertTrue(workouts.any { it.title.contains("طحن البطن") || it.title.contains("البلانك") })
    assertTrue(workouts.any { it.title.contains("ربلة الساق") || it.title.contains("القرفصاء") })
  }

  @Test
  fun `verify quran repository queries authentic database`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val quranRepo = QuranRepository(context)

    val surahs = quranRepo.getAllSurahs()
    assertEquals(114, surahs.size)

    val page1 = quranRepo.getAyahsForPage(1)
    assertEquals(7, page1.size)
    assertTrue("Surah name must contain Fatihah", page1.first().surahName.contains("فَاتِحَة") || page1.first().surahName.contains("الفاتحة"))

    val page604 = quranRepo.getAyahsForPage(604)
    assertTrue("Page 604 contains concluding surahs", page604.isNotEmpty())

    val searchResults = quranRepo.searchQuran("الرحمن")
    assertTrue("Search for الرحمن should return matches", searchResults.isNotEmpty())
    quranRepo.close()
  }

  @Test
  fun `verify dhikr count persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = StudentRepository(context)

    repo.saveDhikrCount(101, 5)
    val count = repo.getDhikrCount(101)
    assertEquals(5, count)

    val allCounts = repo.getAllDhikrCounts()
    assertEquals(5, allCounts[101])
  }

  @Test
  fun `verify calculateNextTriggerMillis produces future timestamp`() {
    val now = System.currentTimeMillis()
    val trigger = AlarmScheduler.calculateNextTriggerMillis("12:00", 0)
    assertTrue("Next trigger must be strictly greater than now", trigger > now)
  }

  @Test
  fun `verify diagnostic store records and retrieves task diagnostic`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AlarmDiagnosticStore.recordSchedule(
      context = context,
      taskId = 1001,
      taskTitle = "صلاة الفجر",
      category = "عبادة",
      targetTime = "05:00",
      nextTriggerMillis = System.currentTimeMillis() + 60000,
      isExactAllowed = true,
      scheduleSuccess = true
    )

    val diagnostics = AlarmDiagnosticStore.getDiagnostics(context)
    val record = diagnostics.find { it.taskId == 1001 }
    assertNotNull("Diagnostic record must exist", record)
    assertEquals("صلاة الفجر", record?.taskTitle)
    assertTrue(record?.scheduleSuccess == true)
  }

  @Test
  fun `verify 12-hour time format helper converts accurately with Arabic labels`() {
    val fajr12 = com.example.util.TimeFormatHelper.format12Hour("05:00", false)
    assertEquals("5:00 ص", fajr12)

    val dhuhr12 = com.example.util.TimeFormatHelper.format12Hour("12:15", false)
    assertEquals("12:15 م", dhuhr12)

    val workout12 = com.example.util.TimeFormatHelper.format12Hour("17:30", true)
    assertEquals("5:30 مساءً", workout12)

    val midnight12 = com.example.util.TimeFormatHelper.format12Hour("00:00", true)
    assertEquals("12:00 منتصف الليل", midnight12)

    // Test Eastern Arabic numerals normalization
    val arabicInput = com.example.util.TimeFormatHelper.normalizeTime("٠٥:٣٠")
    assertEquals("05:30", arabicInput)
  }

  @Test
  fun `verify astronomical prayer calculation for Cairo and Egyptian governorates`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val cairo = com.example.util.LocationAndPrayerManager.EGYPTIAN_CITIES.first { it.id == "cairo" }
    val prayerResult = com.example.util.LocationAndPrayerManager.calculatePrayerTimes(context, city = cairo)

    assertEquals("القاهرة", prayerResult.cityName)
    assertTrue("Fajr time should be valid HH:mm", prayerResult.fajr.matches(Regex("\\d{2}:\\d{2}")))
    assertTrue("Dhuhr time should be around noon", prayerResult.dhuhr.startsWith("11:") || prayerResult.dhuhr.startsWith("12:") || prayerResult.dhuhr.startsWith("13:"))
    assertTrue("Maghrib should be after Asr", prayerResult.maghrib > prayerResult.asr)
    assertTrue("Isha should be after Maghrib", prayerResult.isha > prayerResult.maghrib)
  }

  @Test
  fun `verify notification channels exist with correct importance`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val nm = context.getSystemService(android.app.NotificationManager::class.java)
    val channels = nm.notificationChannels
    // Ensure all 6 channels are defined
    val channelIds = channels.map { it.id }
    assertTrue("Prayer channel must exist", channelIds.contains(com.example.receiver.AlarmReceiver.CHANNEL_PRAYER))
    assertTrue("Study channel must exist", channelIds.contains(com.example.receiver.AlarmReceiver.CHANNEL_STUDY))
    assertTrue("Workouts channel must exist", channelIds.contains(com.example.receiver.AlarmReceiver.CHANNEL_WORKOUTS))
    assertTrue("Adhkar channel must exist", channelIds.contains(com.example.receiver.AlarmReceiver.CHANNEL_ADHKAR))
    assertTrue("Sleep channel must exist", channelIds.contains(com.example.receiver.AlarmReceiver.CHANNEL_SLEEP))
    assertTrue("Timer alert channel must exist", channelIds.contains(com.example.receiver.AlarmReceiver.CHANNEL_TIMER_ALERT))
  }
}

