package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.R
import com.example.ui.StudentViewModel
import com.example.util.PrayerAlertMode
import com.example.util.PrayerSoundManager
import com.example.util.SoundOption
import com.example.util.TimeFormatHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrayerNowSettingsSection(
    viewModel: StudentViewModel,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val prayerTimes by viewModel.prayerTimes.collectAsState()

    var masterEnabled by remember { mutableStateOf(viewModel.isMasterNotificationEnabled()) }
    var activePreviewResId by remember { mutableStateOf<Int?>(null) }
    var showSoundDialogForPrayer by remember { mutableStateOf<String?>(null) }
    var showSoundDialogForCategory by remember { mutableStateOf<String?>(null) }

    // State update trigger
    var changeCounter by remember { mutableIntStateOf(0) }

    // 1. Master Toggle Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (masterEnabled)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (masterEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (masterEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = if (masterEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "التنبيهات والإشعارات العامة",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (masterEnabled) "جميع تنبيهات الصلاة والأذكار والمذاكرة مفعلة" else "تم إيقاف كافة التنبيهات الصوتية والمجدولة",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Switch(
                checked = masterEnabled,
                onCheckedChange = { checked ->
                    masterEnabled = checked
                    viewModel.setMasterNotificationEnabled(checked)
                    onShowMessage(if (checked) "تم تفعيل التنبيهات العامة بنجاح 🔔" else "تم إيقاف التنبيهات العامة 🔕")
                },
                modifier = Modifier.testTag("master_notifications_switch")
            )
        }
    }

    // 2. Info Banner (Prayer Now Matching Architecture)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "نظام إشعارات Prayer Now الاحترافي 🕌",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "• تشغيل الأذان كاملاً عبر Foreground Service مع زر إيقاف فوري ⏹ على شاشة القفل.\n" +
                       "• تكبيرات ونغمات قصيرة عبر قنوات الإشعارات الأصلية دون تشغيل الصوت مرتين.\n" +
                       "• تذكير مسبق قبل الأذان بـ 5 إلى 20 دقيقة للاستعداد للوضوء.\n" +
                       "• تخصيص صوت مستقل لكل صلاة مع معاينة واستماع فوري.",
                fontSize = 11.5.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // 3. Five Prayers Individual Configuration
    Text(
        text = "تخصيص الصلوات الخمس والأذان:",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )

    val prayersList = listOf(
        Triple("Fajr", "صلاة الفجر", prayerTimes.fajr),
        Triple("Dhuhr", "صلاة الظهر", prayerTimes.dhuhr),
        Triple("Asr", "صلاة العصر", prayerTimes.asr),
        Triple("Maghrib", "صلاة المغرب", prayerTimes.maghrib),
        Triple("Isha", "صلاة العشاء", prayerTimes.isha)
    )

    for ((key, title, timeStr) in prayersList) {
        val currentMode = viewModel.getPrayerAlertMode(key)
        val currentSound = viewModel.getPrayerSound(key)
        val currentPreReminder = viewModel.getPreReminderMinutes(key)

        PrayerItemConfigCard(
            prayerKey = key,
            title = title,
            timeStr = timeStr,
            currentMode = currentMode,
            currentSound = currentSound,
            currentPreReminder = currentPreReminder,
            isPreviewPlaying = (activePreviewResId == currentSound.rawResId && viewModel.isPreviewPlaying()),
            onModeSelected = { newMode ->
                viewModel.setPrayerAlertMode(key, newMode)
                changeCounter++
                onShowMessage("تم ضبط $title على: ${newMode.title}")
            },
            onChangeSoundClick = {
                showSoundDialogForPrayer = key
            },
            onTogglePreview = {
                if (activePreviewResId == currentSound.rawResId && viewModel.isPreviewPlaying()) {
                    viewModel.stopAudioPreview()
                    activePreviewResId = null
                } else {
                    activePreviewResId = currentSound.rawResId
                    viewModel.playAudioPreview(currentSound.rawResId) {
                        activePreviewResId = null
                    }
                }
            },
            onPreReminderChange = { minutes ->
                viewModel.setPreReminderMinutes(key, minutes)
                changeCounter++
                viewModel.scheduleAllAlarmsNow()
                onShowMessage(
                    if (minutes > 0) "تم تفعيل تذكير اقتراب $title قبل $minutes دقائق 🕌"
                    else "تم إلغاء التذكير المسبق لـ $title"
                )
            },
            onTestAlert = {
                when (currentMode) {
                    PrayerAlertMode.FULL_ADHAN -> {
                        viewModel.testFullAdhan(title, currentSound.rawResId)
                        onShowMessage("بدأ تشغيل الأذان كاملاً في الخلفية مع زر إيقاف في الإشعارات 🕌")
                    }
                    PrayerAlertMode.SHORT_TONE -> {
                        viewModel.sendTestNotification("عبادة", "حيّ على الصلاة - حان الآن موعد $title 🕌")
                        onShowMessage("تم إرسال إشعار منبثق بنغمة التكبيرات 🔔")
                    }
                    PrayerAlertMode.SILENT -> {
                        viewModel.sendTestNotification("عبادة", "حان موعد $title (تنبيه صامت واهتزاز) 📳")
                        onShowMessage("تم إرسال إشعار منبثق مع اهتزاز صامت 📳")
                    }
                    PrayerAlertMode.DISABLED -> {
                        onShowMessage("التنبيه معطل لهذه الصلاة، يرجى تفعيله أولاً")
                    }
                }
            }
        )
    }

    // 4. Other Categories Section (Adhkar, Sleep, Study, Workouts)
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "أصوات وتنبيهات الأذكار والمذاكرة والتمارين:",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )

    val categoriesList = listOf(
        Triple("adhkar", "أذكار الصباح والمساء والقرآن", "أذكار الصباح والمساء"),
        Triple("sleep", "أذكار ومواعيد النوم والراحة", "أذكار النوم"),
        Triple("study", "المحاضرات والمذاكرة الجامعية", "موعد المحاضرة"),
        Triple("workout", "التمارين الرياضية وتنشيط البدن", "تمرين الظهر والبطن")
    )

    for ((catKey, catTitle, testTitle) in categoriesList) {
        val catMode = viewModel.getCategoryAlertMode(catKey)
        val catSound = viewModel.getCategorySound(catKey)

        CategoryItemConfigCard(
            categoryKey = catKey,
            title = catTitle,
            currentMode = catMode,
            currentSound = catSound,
            isPreviewPlaying = (activePreviewResId == catSound.rawResId && viewModel.isPreviewPlaying()),
            onModeSelected = { newMode ->
                viewModel.setCategoryAlertMode(catKey, newMode)
                changeCounter++
                onShowMessage("تم ضبط $catTitle على: ${newMode.title}")
            },
            onChangeSoundClick = {
                showSoundDialogForCategory = catKey
            },
            onTogglePreview = {
                if (activePreviewResId == catSound.rawResId && viewModel.isPreviewPlaying()) {
                    viewModel.stopAudioPreview()
                    activePreviewResId = null
                } else {
                    activePreviewResId = catSound.rawResId
                    viewModel.playAudioPreview(catSound.rawResId) {
                        activePreviewResId = null
                    }
                }
            },
            onTestAlert = {
                val catTag = when (catKey) {
                    "workout" -> "رياضة"
                    "study" -> "جامعة"
                    else -> "أذكار"
                }
                viewModel.sendTestNotification(catTag, "تنبيه $catTitle: حان الموعد المحدد")
                onShowMessage("تم إرسال إشعار تجريبي لـ $catTitle")
            }
        )
    }

    // 5. Sound Picker Dialog for Prayer
    showSoundDialogForPrayer?.let { prayerKey ->
        val currentSound = viewModel.getPrayerSound(prayerKey)
        SoundPickerDialog(
            title = "اختيار صوت الأذان لـ ${PrayerSoundManager.getPrayerArabicName(prayerKey)}",
            currentSoundId = currentSound.id,
            options = PrayerSoundManager.SOUND_OPTIONS,
            onSelect = { sound ->
                viewModel.setPrayerSound(prayerKey, sound.id)
                viewModel.stopAudioPreview()
                activePreviewResId = null
                showSoundDialogForPrayer = null
                onShowMessage("تم حفظ الصوت: ${sound.displayName}")
            },
            onPreview = { sound ->
                if (activePreviewResId == sound.rawResId && viewModel.isPreviewPlaying()) {
                    viewModel.stopAudioPreview()
                    activePreviewResId = null
                } else {
                    activePreviewResId = sound.rawResId
                    viewModel.playAudioPreview(sound.rawResId) {
                        activePreviewResId = null
                    }
                }
            },
            previewPlayingResId = activePreviewResId,
            onDismiss = {
                viewModel.stopAudioPreview()
                activePreviewResId = null
                showSoundDialogForPrayer = null
            }
        )
    }

    // 6. Sound Picker Dialog for Category
    showSoundDialogForCategory?.let { catKey ->
        val currentSound = viewModel.getCategorySound(catKey)
        val catName = categoriesList.find { it.first == catKey }?.second ?: catKey
        SoundPickerDialog(
            title = "اختيار نغمة $catName",
            currentSoundId = currentSound.id,
            options = PrayerSoundManager.SOUND_OPTIONS,
            onSelect = { sound ->
                viewModel.setCategorySound(catKey, sound.id)
                viewModel.stopAudioPreview()
                activePreviewResId = null
                showSoundDialogForCategory = null
                onShowMessage("تم حفظ النغمة: ${sound.displayName}")
            },
            onPreview = { sound ->
                if (activePreviewResId == sound.rawResId && viewModel.isPreviewPlaying()) {
                    viewModel.stopAudioPreview()
                    activePreviewResId = null
                } else {
                    activePreviewResId = sound.rawResId
                    viewModel.playAudioPreview(sound.rawResId) {
                        activePreviewResId = null
                    }
                }
            },
            previewPlayingResId = activePreviewResId,
            onDismiss = {
                viewModel.stopAudioPreview()
                activePreviewResId = null
                showSoundDialogForCategory = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrayerItemConfigCard(
    prayerKey: String,
    title: String,
    timeStr: String,
    currentMode: PrayerAlertMode,
    currentSound: SoundOption,
    currentPreReminder: Int,
    isPreviewPlaying: Boolean,
    onModeSelected: (PrayerAlertMode) -> Unit,
    onChangeSoundClick: () -> Unit,
    onTogglePreview: () -> Unit,
    onPreReminderChange: (Int) -> Unit,
    onTestAlert: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Name + Time + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = TimeFormatHelper.format12Hour(timeStr),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (currentMode) {
                        PrayerAlertMode.FULL_ADHAN -> Color(0xFF2E7D32).copy(alpha = 0.15f)
                        PrayerAlertMode.SHORT_TONE -> Color(0xFFF57F17).copy(alpha = 0.15f)
                        PrayerAlertMode.SILENT -> Color(0xFF6A1B9A).copy(alpha = 0.15f)
                        PrayerAlertMode.DISABLED -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = currentMode.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (currentMode) {
                            PrayerAlertMode.FULL_ADHAN -> Color(0xFF2E7D32)
                            PrayerAlertMode.SHORT_TONE -> Color(0xFFF57F17)
                            PrayerAlertMode.SILENT -> Color(0xFF6A1B9A)
                            PrayerAlertMode.DISABLED -> MaterialTheme.colorScheme.outline
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Alert Mode Selector Chips
            Text(text = "نمط التنبيه:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PrayerAlertMode.values().forEach { mode ->
                    val isSelected = (mode == currentMode)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onModeSelected(mode) },
                        label = { Text(mode.title, fontSize = 11.sp) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }

            // Sound Selector & Preview (visible if not disabled and not silent)
            if (currentMode != PrayerAlertMode.DISABLED && currentMode != PrayerAlertMode.SILENT) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onChangeSoundClick() }
                    ) {
                        Text(text = "ملف الصوت المحدد:", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = currentSound.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Preview Audio Button
                        IconButton(
                            onClick = onTogglePreview,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isPreviewPlaying) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isPreviewPlaying) "إيقاف" else "معاينة",
                                tint = if (isPreviewPlaying) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedButton(
                            onClick = onChangeSoundClick,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("تغيير", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Pre-prayer Reminder Selector
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "تذكير قبل الأذان:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0 to "بدون", 5 to "5 د", 10 to "10 د", 15 to "15 د").forEach { (mins, label) ->
                        val isSelected = (currentPreReminder == mins)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .clickable { onPreReminderChange(mins) }
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(6.dp)
                                )
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Test Alert Button
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onTestAlert,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "تجربة التنبيه عملياً لهذا الموعد (${currentMode.title})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryItemConfigCard(
    categoryKey: String,
    title: String,
    currentMode: PrayerAlertMode,
    currentSound: SoundOption,
    isPreviewPlaying: Boolean,
    onModeSelected: (PrayerAlertMode) -> Unit,
    onChangeSoundClick: () -> Unit,
    onTogglePreview: () -> Unit,
    onTestAlert: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = currentMode.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Modes: Short tone, Silent, Disabled
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(PrayerAlertMode.SHORT_TONE, PrayerAlertMode.SILENT, PrayerAlertMode.DISABLED).forEach { mode ->
                    val isSelected = (mode == currentMode)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onModeSelected(mode) },
                        label = { Text(mode.title, fontSize = 11.sp) }
                    )
                }
            }

            if (currentMode != PrayerAlertMode.DISABLED && currentMode != PrayerAlertMode.SILENT) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentSound.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onChangeSoundClick() }
                    )
                    IconButton(
                        onClick = onTogglePreview,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isPreviewPlaying) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isPreviewPlaying) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = onTestAlert,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("إرسال إشعار تجريبي 🧪", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SoundPickerDialog(
    title: String,
    currentSoundId: String,
    options: List<SoundOption>,
    onSelect: (SoundOption) -> Unit,
    onPreview: (SoundOption) -> Unit,
    previewPlayingResId: Int?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { option ->
                    val isSelected = (option.id == currentSoundId)
                    val isPlaying = (previewPlayingResId == option.rawResId)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .clickable { onSelect(option) }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (option.isFullAdhan) {
                                Text(text = "صوت أذان كامل مخصص للخدمة الخلفية", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        IconButton(
                            onClick = { onPreview(option) },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isPlaying) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "إيقاف" else "استماع",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
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
