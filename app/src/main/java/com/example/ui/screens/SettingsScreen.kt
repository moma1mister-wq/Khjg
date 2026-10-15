package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.receiver.AlarmReceiver
import com.example.ui.StudentViewModel
import com.example.util.AlarmDiagnosticStore
import com.example.util.LocationAndPrayerManager
import com.example.util.SoundHelper
import com.example.util.TaskAlarmDiagnostic
import com.example.util.TimeFormatHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: StudentViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val diagnostics by viewModel.diagnostics.collectAsState()
    val prayerTimes by viewModel.prayerTimes.collectAsState()

    var selectedSectionTab by remember { mutableIntStateOf(0) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var canScheduleExact by remember { mutableStateOf(viewModel.canScheduleExactAlarms()) }
    var isIgnoringBattery by remember { mutableStateOf(viewModel.isIgnoringBatteryOptimizations()) }

    var testTargetTime by remember { mutableLongStateOf(AlarmDiagnosticStore.getTwoMinuteTestTarget(context)) }
    var remainingSecondsToTest by remember { mutableLongStateOf(0L) }

    var showCityDialog by remember { mutableStateOf(false) }

    // Countdown timer for scheduled test
    LaunchedEffect(testTargetTime) {
        while (testTargetTime > System.currentTimeMillis()) {
            remainingSecondsToTest = ((testTargetTime - System.currentTimeMillis()) / 1000).coerceAtLeast(0L)
            delay(1000L)
        }
        remainingSecondsToTest = 0L
    }

    LaunchedEffect(Unit) {
        viewModel.refreshDiagnostics()
        viewModel.refreshPrayerTimes()
        canScheduleExact = viewModel.canScheduleExactAlarms()
        isIgnoringBattery = viewModel.isIgnoringBatteryOptimizations()
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            viewModel.sendTestNotification()
            scope.launch { snackbarHostState.showSnackbar("تم منح إذن الإشعارات وإرسال إشعار تجريبي بنجاح") }
        } else {
            scope.launch { snackbarHostState.showSnackbar("تم رفض إذن الإشعارات، يمكنك تفعيله من إعدادات النظام") }
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            viewModel.detectLocationAndSetCity { success, message ->
                scope.launch { snackbarHostState.showSnackbar(message) }
            }
        } else {
            scope.launch { snackbarHostState.showSnackbar("تم رفض إذن الموقع، سيتم الاستمرار في استخدام المدينة المحفوظة") }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = "الإعدادات وتشخيص التوقيت والموقع والإشعارات",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Text(
                    text = "فحص أذونات النظام، مواقيت الصلاة، المنبهات الدقيقة وتوافق ColorOS / OPPO",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )

                ScrollableTabRow(
                    selectedTabIndex = selectedSectionTab,
                    edgePadding = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedSectionTab == 0,
                        onClick = { selectedSectionTab = 0 },
                        text = { Text("تنبيهات Prayer Now 🕌", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedSectionTab == 1,
                        onClick = { selectedSectionTab = 1 },
                        text = { Text("الموقع والمواقيت 🌍", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedSectionTab == 2,
                        onClick = { selectedSectionTab = 2 },
                        text = { Text("الأذونات والبطارية 🛡️", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedSectionTab == 3,
                        onClick = { selectedSectionTab = 3 },
                        text = { Text("قنوات الإشعارات 🔔", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedSectionTab == 4,
                        onClick = { selectedSectionTab = 4 },
                        text = { Text("سجل التنبيهات ⏱️", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("settings_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedSectionTab) {
                // TAB 0: PRAYER NOW NOTIFICATIONS & AUDIO CUSTOMIZATION
                0 -> {
                    item {
                        PrayerNowSettingsSection(
                            viewModel = viewModel,
                            onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                        )
                    }
                }

                // TAB 1: LOCATION & PRAYER TIMES
                1 -> {
                    item {
                        LocationAndPrayerHeader(
                            prayerTimes = prayerTimes,
                            onDetectLocation = {
                                if (hasLocationPermission) {
                                    viewModel.detectLocationAndSetCity { _, msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                } else {
                                    locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                }
                            },
                            onChangeCity = { showCityDialog = true }
                        )
                    }

                    item {
                        TodayPrayerTimesCard(
                            prayerTimes = prayerTimes,
                            onSyncNow = {
                                viewModel.setManualCity(LocationAndPrayerManager.getSelectedCity(context).id)
                                scope.launch { snackbarHostState.showSnackbar("تمت إعادة حساب المواقيت وتحديث منبهات المهام بنجاح 🕌") }
                            }
                        )
                    }

                    item {
                        PrayerOffsetsAdjustmentCard(
                            prayerTimes = prayerTimes,
                            onOffsetChange = { prayer, offset ->
                                viewModel.setPrayerOffset(prayer, offset)
                                scope.launch { snackbarHostState.showSnackbar("تم تعديل فرق توقيت $prayer بمقدار $offset دقيقة") }
                            }
                        )
                    }
                }

                // TAB 2: PERMISSIONS & BATTERY OPTIMIZATIONS (ColorOS / OPPO Reno 8T)
                2 -> {
                    item {
                        PermissionsAuditHeader()
                    }

                    // 1. POST_NOTIFICATIONS
                    item {
                        PermissionCard(
                            title = "إذن الإشعارات (POST_NOTIFICATIONS)",
                            requiredVersion = "Android 13+ (API 33+)",
                            type = "وقت التشغيل (Runtime)",
                            isGranted = hasNotificationPermission,
                            description = "إظهار تنبيهات مواعيد الصلوات الخمس، أذكار الصباح والمساء، المحاضرات، التمارين الرياضية ومؤقت المذاكرة في شريط الإشعارات.",
                            onGrantClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            grantButtonText = "طلب إذن الإشعارات"
                        )
                    }

                    // 2. SCHEDULE_EXACT_ALARM & USE_EXACT_ALARM
                    item {
                        PermissionCard(
                            title = "إذن المنبهات الدقيقة (Exact Alarms)",
                            requiredVersion = "Android 12+ (API 31+)",
                            type = "تصريح خاص في إعدادات النظام",
                            isGranted = canScheduleExact,
                            description = "تشغيل صوت الأذان والتنبيهات المجدولة في الثانية المحددة بدقة كاملة عبر AlarmManager.setAlarmClock() حتى أثناء نوم الجهاز.",
                            onGrantClick = { viewModel.openExactAlarmSettings() },
                            grantButtonText = "فتح إعدادات المنبهات الدقيقة"
                        )
                    }

                    // 3. ACCESS_COARSE_LOCATION
                    item {
                        PermissionCard(
                            title = "إذن الموقع التقريبي (ACCESS_COARSE_LOCATION)",
                            requiredVersion = "جميع الإصدارات",
                            type = "وقت التشغيل عند الطلب فقط",
                            isGranted = hasLocationPermission,
                            description = "تحديد المدينة والمنطقة الزمنية لحساب مواقيت الصلاة بدقة. لا يتم استخدام الموقع في الخلفية أو إرساله لأي خادم.",
                            onGrantClick = {
                                locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            },
                            grantButtonText = "منح إذن الموقع لحساب المواقيت"
                        )
                    }

                    // 4. BATTERY OPTIMIZATION EXEMPTION
                    item {
                        PermissionCard(
                            title = "استثناء تحسين البطارية (Battery Optimization)",
                            requiredVersion = "Android 6+ / ColorOS / OPPO",
                            type = "إعدادات النظام",
                            isGranted = isIgnoringBattery,
                            description = "منع إيقاف النظام أو وضع Doze للتنبيهات المجدولة ومؤقت المذاكرة في الخلفية.",
                            onGrantClick = {
                                viewModel.requestIgnoreBatteryOptimizations()
                                isIgnoringBattery = viewModel.isIgnoringBatteryOptimizations()
                            },
                            grantButtonText = "طلب استثناء تحسين البطارية"
                        )
                    }

                    // 5. OPPO Reno 8T / ColorOS Aggressive Killing Guide
                    item {
                        ColorOsOptimizationCard(
                            onOpenColorOsSettings = {
                                val opened = viewModel.openColorOsAutoStartSettings()
                                if (!opened) {
                                    scope.launch { snackbarHostState.showSnackbar("تم فتح تفاصيل التطبيق في إعدادات النظام") }
                                }
                            }
                        )
                    }
                }

                // TAB 3: NOTIFICATION CHANNELS & SOUND AUDIT
                3 -> {
                    item {
                        NotificationChannelsHeader()
                    }

                    // Channel 1: Prayer
                    item {
                        ChannelAuditCard(
                            channelName = "1. مواقيت الصلاة والأذان",
                            channelId = AlarmReceiver.CHANNEL_PRAYER,
                            importance = "عالية جداً (HIGH / ALARM)",
                            soundName = "صوت الأذان والتكبيرات (adhan_tone.mp3)",
                            vibration = "اهتزاز قوي ممتد",
                            onPlaySound = {
                                SoundHelper.vibrate(context, isAlarm = true)
                                SoundHelper.playSound(context, R.raw.adhan_tone, true)
                            },
                            onTestNotification = {
                                SoundHelper.vibrate(context, isAlarm = true)
                                viewModel.sendTestNotification("عبادة", "حيّ على الصلاة - حان موعد صلاة الظهر")
                                scope.launch { snackbarHostState.showSnackbar("تم إرسال إشعار الصلاة بصوت الأذان والاهتزاز 🕌") }
                            }
                        )
                    }

                    // Channel 2: Study & Lectures
                    item {
                        ChannelAuditCard(
                            channelName = "2. المذاكرة والمحاضرات الجامعية",
                            channelId = AlarmReceiver.CHANNEL_STUDY,
                            importance = "عالية (HIGH)",
                            soundName = "نغمة الأكاديمية والمحاضرات (lecture_tone.mp3)",
                            vibration = "اهتزاز نبضي واضح",
                            onPlaySound = {
                                SoundHelper.vibrate(context, isAlarm = true)
                                SoundHelper.playSound(context, R.raw.lecture_tone, true)
                            },
                            onTestNotification = {
                                SoundHelper.vibrate(context, isAlarm = true)
                                viewModel.sendTestNotification("جامعة", "موعد محاضرة تحليل الخوارزميات - مدرج 1")
                                scope.launch { snackbarHostState.showSnackbar("تم إرسال إشعار المحاضرات الدراسية 📚") }
                            }
                        )
                    }

                    // Channel 3: Workouts
                    item {
                        ChannelAuditCard(
                            channelName = "3. التمارين الرياضية وتنشيط البدن",
                            channelId = AlarmReceiver.CHANNEL_WORKOUTS,
                            importance = "عالية (HIGH)",
                            soundName = "نغمة النشاط والحركة (lecture_tone.mp3)",
                            vibration = "اهتزاز منبه رياضي قوي",
                            onPlaySound = {
                                SoundHelper.vibrate(context, isAlarm = true)
                                SoundHelper.playSound(context, R.raw.lecture_tone, true)
                            },
                            onTestNotification = {
                                SoundHelper.vibrate(context, isAlarm = true)
                                viewModel.sendTestNotification("رياضة", "حان موعد التمرين اليومي وتنشيط الظهر والبطن والأرجل")
                                scope.launch { snackbarHostState.showSnackbar("تم إرسال إشعار التمارين الرياضية 💪") }
                            }
                        )
                    }

                    // Channel 4: Adhkar & Quran
                    item {
                        ChannelAuditCard(
                            channelName = "4. أذكار الصباح والمساء والقرآن",
                            channelId = AlarmReceiver.CHANNEL_ADHKAR,
                            importance = "عالية (HIGH)",
                            soundName = "نغمة أذكار هادئة ومطمئنة (adhkar_tone.mp3)",
                            vibration = "اهتزاز هادئ ومريح",
                            onPlaySound = {
                                SoundHelper.vibrate(context, isAlarm = false)
                                SoundHelper.playSound(context, R.raw.adhkar_tone, false)
                            },
                            onTestNotification = {
                                SoundHelper.vibrate(context, isAlarm = false)
                                viewModel.sendTestNotification("أذكار", "ألا بذكر الله تطمئن القلوب - أذكار المساء")
                                scope.launch { snackbarHostState.showSnackbar("تم إرسال إشعار الأذكار بالنغمة الهادئة 🌿") }
                            }
                        )
                    }

                    // Channel 5: Sleep
                    item {
                        ChannelAuditCard(
                            channelName = "5. أذكار ومواعيد النوم",
                            channelId = AlarmReceiver.CHANNEL_SLEEP,
                            importance = "عالية (HIGH)",
                            soundName = "نغمة النوم الهادئة (adhkar_tone.mp3)",
                            vibration = "اهتزاز ناعم",
                            onPlaySound = {
                                SoundHelper.vibrate(context, isAlarm = false)
                                SoundHelper.playSound(context, R.raw.adhkar_tone, false)
                            },
                            onTestNotification = {
                                SoundHelper.vibrate(context, isAlarm = false)
                                viewModel.sendTestNotification("أذكار", "باسمك ربي وضعت جنبي - أذكار النوم")
                                scope.launch { snackbarHostState.showSnackbar("تم إرسال إشعار النوم 🌙") }
                            }
                        )
                    }

                    // Channel 6: Timer Completed
                    item {
                        ChannelAuditCard(
                            channelName = "6. اكتمال مؤقت المذاكرة والتركيز",
                            channelId = AlarmReceiver.CHANNEL_TIMER_ALERT,
                            importance = "عالية جداً (HIGH / ALARM)",
                            soundName = "نغمة الإنجاز واكتمال الجلسة (study_tone.mp3)",
                            vibration = "اهتزاز مزدوج احتفالي",
                            onPlaySound = {
                                SoundHelper.vibrate(context, isAlarm = true)
                                SoundHelper.playSound(context, R.raw.study_tone, true)
                            },
                            onTestNotification = {
                                SoundHelper.vibrate(context, isAlarm = true)
                                viewModel.sendTestNotification("مذاكرة", "أحسنت! اكتملت جلسة المذاكرة والتركيز 🎯")
                                scope.launch { snackbarHostState.showSnackbar("تم إرسال إشعار اكتمال المذاكرة 🎯") }
                            }
                        )
                    }

                    item {
                        OutlinedButton(
                            onClick = { SoundHelper.stopSound() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إيقاف أي صوت أو اهتزاز يعمل حاليًا ⏹", fontSize = 13.sp)
                        }
                    }
                }

                // TAB 4: SCHEDULED ALARMS LOG & LIVE TESTS
                4 -> {
                    item {
                        PracticalAlarmTestsCard(
                            remainingSecondsToTest = remainingSecondsToTest,
                            onQuickTest = {
                                viewModel.scheduleQuickTest(15) { target ->
                                    testTargetTime = target
                                    scope.launch {
                                        snackbarHostState.showSnackbar("تمت جدولة تنبيه سريع بعد 15 ثانية! يمكنك الآن إطفاء الشاشة لاختبار الاستيقاظ ⏱️")
                                    }
                                }
                            },
                            onTwoMinuteTest = {
                                viewModel.scheduleTwoMinuteTest { target ->
                                    testTargetTime = target
                                    scope.launch {
                                        snackbarHostState.showSnackbar("تمت جدولة تنبيه بعد دقيقتين في AlarmManager ⏱️")
                                    }
                                }
                            },
                            onScheduleAllNow = {
                                viewModel.scheduleAllAlarmsNow()
                                canScheduleExact = viewModel.canScheduleExactAlarms()
                                scope.launch { snackbarHostState.showSnackbar("تمت إعادة جدولة وتثبيت كافة المهام والصلوات في AlarmManager") }
                            }
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "سجل التنبيهات المجدولة في النظام (${diagnostics.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { viewModel.refreshDiagnostics() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    if (diagnostics.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("لا توجد سجلات بعد. انقر على زر الجدولة للتثبيت في المنبه الأصلي:", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { viewModel.scheduleAllAlarmsNow() }) {
                                        Text("جدولة جميع المهام الآن")
                                    }
                                }
                            }
                        }
                    } else {
                        items(diagnostics, key = { it.taskId }) { diag ->
                            TaskDiagnosticCardEnhanced(diag)
                        }
                    }
                }
            }
        }
    }

    if (showCityDialog) {
        CitySelectionDialog(
            currentCityId = LocationAndPrayerManager.getSelectedCity(context).id,
            onSelectCity = { cityId ->
                viewModel.setManualCity(cityId)
                showCityDialog = false
                scope.launch { snackbarHostState.showSnackbar("تم اعتماد المدينة وتحديث مواقيت الصلاة بنجاح") }
            },
            onDismiss = { showCityDialog = false }
        )
    }
}

@Composable
fun PermissionsAuditHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تدقيق وفحص أذونات Android والتوافق",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "يراجع هذا القسم جاهزية هاتف الطالب (خاصة OPPO Reno 8T بنظام ColorOS) لاستقبال التنبيهات الدقيقة والمواقيت دون مقاطعة من نظام إدارة الطاقة.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    requiredVersion: String,
    type: String,
    isGranted: Boolean,
    description: String,
    onGrantClick: () -> Unit,
    grantButtonText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = "$requiredVersion • $type", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isGranted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = if (isGranted) "ممنوح ✓" else "غير متاح ✗",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGranted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onGrantClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(grantButtonText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ColorOsOptimizationCard(onOpenColorOsSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "دليل التوافق لهواتف OPPO Reno 8T وواجهة ColorOS",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "تقوم واجهات ColorOS بإيقاف التطبيقات وإلغاء المنبهات في الخلفية لتوفير الطاقة. لضمان تشغيل الأذان والمواعيد دون تأخير، يرجى تفعيل الخطوات التالية:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("1. بدء التشغيل التلقائي (Auto-launch): مسموح.", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("2. السماح بالنشاط في الخلفية (Allow background activity): مفعّل.", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("3. تحسين البطارية (Battery Optimization): إيقاف التحسين (Don't optimize).", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("4. قفل التطبيق في قائمة المهام الحديثة (Lock in Recent Apps).", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOpenColorOsSettings,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("فتح إعدادات البدء التلقائي والبطارية في OPPO", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun LocationAndPrayerHeader(
    prayerTimes: com.example.util.PrayerTimesResult,
    onDetectLocation: () -> Unit,
    onChangeCity: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "المدينة: ${prayerTimes.cityName}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (prayerTimes.isAuto) "تحديد تلقائي عبر GPS/الشبكة" else "اختيار يدوي",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = if (prayerTimes.isDstActive) "توقيت صيفي مفعل" else "توقيت قياسي",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "المنطقة الزمنية المعتمدة: ${prayerTimes.timeZoneDisplayName} (مأخوذة من نظام Android)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDetectLocation,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تحديد موقعي تلقائياً", fontSize = 11.sp)
                }

                Button(
                    onClick = onChangeCity,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("اختيار المدينة يدوياً", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TodayPrayerTimesCard(
    prayerTimes: com.example.util.PrayerTimesResult,
    onSyncNow: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مواقيت الصلاة لليوم بنظام 12 ساعة",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onSyncNow) {
                    Icon(Icons.Default.Refresh, contentDescription = "مزامنة", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val prayersList = listOf(
                Pair("الفجر", prayerTimes.fajr),
                Pair("الشروق", prayerTimes.sunrise),
                Pair("الظهر", prayerTimes.dhuhr),
                Pair("العصر", prayerTimes.asr),
                Pair("المغرب", prayerTimes.maghrib),
                Pair("العشاء", prayerTimes.isha)
            )

            for ((name, time24) in prayersList) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(
                        text = TimeFormatHelper.format12Hour(time24, true),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun PrayerOffsetsAdjustmentCard(
    prayerTimes: com.example.util.PrayerTimesResult,
    onOffsetChange: (String, Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ضبط دقيق لمواقيت الصلاة (تعديل بالدقائق)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "يمكنك تقديم أو تأخير أي فريضة بعدة دقائق لمطابقة المسجد المجاور بدقة:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(10.dp))

            val prayers = listOf("Fajr" to "صلاة الفجر", "Dhuhr" to "صلاة الظهر", "Asr" to "صلاة العصر", "Maghrib" to "صلاة المغرب", "Isha" to "صلاة العشاء")
            for ((key, label) in prayers) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = label, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { onOffsetChange(key, -1) },
                            modifier = Modifier.size(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedButton(
                            onClick = { onOffsetChange(key, 1) },
                            modifier = Modifier.size(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationChannelsHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "قنوات الإشعارات الست المنفصلة",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "تم إنشاء قنوات منفصلة ومستقلة لكل صنف (الصلاة، المذاكرة، التمارين، الأذكار، النوم، والمؤقتات) بأهمية قصوى واهتزاز ونغمة مخصصة تضمن عدم كونها صامتة.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun ChannelAuditCard(
    channelName: String,
    channelId: String,
    importance: String,
    soundName: String,
    vibration: String,
    onPlaySound: () -> Unit,
    onTestNotification: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = channelName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "مفعّلة ✓",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "• معرّف القناة: $channelId", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            Text(text = "• الأهمية: $importance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "• الصوت: $soundName", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "• الاهتزاز: $vibration", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPlaySound,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("استماع 🔊", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onTestNotification,
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تجربة إشعار فوري 🔔", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun PracticalAlarmTestsCard(
    remainingSecondsToTest: Long,
    onQuickTest: () -> Unit,
    onTwoMinuteTest: () -> Unit,
    onScheduleAllNow: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "اختبارات المنبه المجدول في الخلفية (AlarmManager)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "يبرمج هذا الاختبار منبهًا حقيقيًا في AlarmManager بنظام setAlarmClock() المخصص لإيقاظ هواتف OPPO / ColorOS وأحدث أنظمة Android عند إطفاء الشاشة.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                lineHeight = 18.sp
            )

            if (remainingSecondsToTest > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "التنبيه مجدول! متبقٍ على الرنين: $remainingSecondsToTest ثانية ⏱️",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "أغلق التطبيق وأطفئ الشاشة للتأكد من استيقاظ الهاتف والرنين.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onQuickTest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("اختبار سريع (15 ثانية)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onTwoMinuteTest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("اختبار دقيقتين", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onScheduleAllNow,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تحديث وجدولة جميع المهام والصلوات فوراً", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TaskDiagnosticCardEnhanced(diag: TaskAlarmDiagnostic) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = diag.taskTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (diag.scheduleSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = if (diag.scheduleSuccess) "مجدول بنجاح" else "فشل",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (diag.scheduleSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            val time12 = TimeFormatHelper.format12Hour(diag.targetTime, true)
            Text(text = "• الوقت المحفوظ (بنظام 12 ساعة): $time12 (${diag.category})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "• وقت الرنين القادم: ${diag.nextTriggerFormatted}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            Text(text = "• إذن الدقة (Exact Alarms): ${if (diag.isExactAllowed) "ممنوح ومفعّل ✓" else "تنبيه تقريبي"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

            if (diag.alarmReceivedAt != null) {
                val formattedRecv = TimeFormatHelper.formatTimestamp12Hour(diag.alarmReceivedAt)
                Text(text = "• استلام حدث التنبيه (Broadcast Received): نعم في $formattedRecv ✓", fontSize = 11.sp, color = Color(0xFF1B5E20), fontWeight = FontWeight.Medium)
            } else {
                Text(text = "• استلام حدث التنبيه: في انتظار حلول الموعد", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            }

            if (diag.lastError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "• ملاحظة: ${diag.lastError}", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun CitySelectionDialog(
    currentCityId: String,
    onSelectCity: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختر المدينة لحساب مواقيت الصلاة", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                item {
                    Text("محافظات ومدن جمهورية مصر العربية:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp))
                }
                items(LocationAndPrayerManager.EGYPTIAN_CITIES) { city ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectCity(city.id) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = city.arabicName, fontSize = 14.sp)
                        if (city.id == currentCityId) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("العواصم والمدن العربية والإسلامية:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(vertical = 4.dp))
                }
                items(LocationAndPrayerManager.ARAB_CAPITALS) { city ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectCity(city.id) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "${city.arabicName} (${city.country})", fontSize = 13.sp)
                        if (city.id == currentCityId) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
