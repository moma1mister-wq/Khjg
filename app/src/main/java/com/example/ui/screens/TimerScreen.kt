package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudentWorkout
import com.example.data.model.StudentWorkoutData
import com.example.service.ActiveTimerState
import com.example.ui.StudentViewModel
import com.example.ui.components.EmeraldBorder
import com.example.ui.components.toEasternArabic
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun TimerScreen(viewModel: StudentViewModel) {
    val screenTab by viewModel.timerScreenTab.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("timer_screen")
    ) {
        TabRow(
            selectedTabIndex = screenTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = screenTab == 0,
                onClick = { viewModel.setTimerScreenTab(0) },
                icon = { Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(20.dp)) },
                text = { Text("مؤقت المذاكرة", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = screenTab == 1,
                onClick = { viewModel.setTimerScreenTab(1) },
                icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(20.dp)) },
                text = { Text("التمارين الرياضية", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = screenTab == 2,
                onClick = { viewModel.setTimerScreenTab(2) },
                icon = { Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(20.dp)) },
                text = { Text("تقارير الإنجاز", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }

        when (screenTab) {
            0 -> StudyTimerTabContent(viewModel = viewModel)
            1 -> WorkoutTabContent()
            2 -> AnalyticsTabContent(viewModel = viewModel)
        }
    }
}

@Composable
fun StudyTimerTabContent(viewModel: StudentViewModel) {
    val timerState by viewModel.timerState.collectAsState()
    val todayMinutes by viewModel.todayStudyMinutes.collectAsState()
    val totalMinutes by viewModel.totalStudyMinutes.collectAsState()
    val lectures by viewModel.allLectures.collectAsState()

    var customSubject by remember { mutableStateOf("مذاكرة عامة") }
    var selectedSessionType by remember { mutableStateOf("مؤقت المذاكرة الحرة") }
    var selectedPomodoroMinutes by remember { mutableIntStateOf(25) }
    var customMinutesInput by remember { mutableStateOf("") }
    var isPomodoroMode by remember { mutableStateOf(true) }

    val sessionTypes = listOf(
        "مؤقت PDF",
        "مؤقت الفيديو",
        "مؤقت المذاكرة الحرة",
        "مؤقت حل التمارين"
    )

    val presetSubjects = remember(lectures) {
        val fromLectures = lectures.map { it.subject.split("(")[0].split("[")[0].trim() }.distinct()
        if (fromLectures.isNotEmpty()) fromLectures.take(5) else listOf("Algorithm Analysis", "Computer Networks", "Data Structures", "Linear Algebra", "مذاكرة عامة")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dial & Active Status
        item {
            TimerDialCard(
                timerState = timerState,
                isPomodoro = isPomodoroMode,
                targetMinutes = selectedPomodoroMinutes
            )
        }

        // Action Controls
        item {
            TimerControls(
                timerState = timerState,
                onStart = {
                    val finalMinutes = customMinutesInput.toIntOrNull() ?: selectedPomodoroMinutes
                    viewModel.startTimer(
                        subject = customSubject,
                        sessionType = selectedSessionType,
                        targetMinutes = finalMinutes,
                        isPomodoro = isPomodoroMode
                    )
                },
                onPause = { viewModel.pauseTimer() },
                onResume = { viewModel.resumeTimer() },
                onStop = { viewModel.stopTimer() }
            )
        }

        // Configuration Card (only editable when stopped)
        if (!timerState.isRunning) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "نوع جلسة المذاكرة:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sessionTypes.forEach { type ->
                                FilterChip(
                                    selected = selectedSessionType == type,
                                    onClick = { selectedSessionType = type },
                                    label = { Text(type) }
                                )
                            }
                        }

                        // Mode toggle: Pomodoro vs Open
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isPomodoroMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isPomodoroMode = true }
                            ) {
                                Text(
                                    text = "مؤقت محدد (بومودورو)",
                                    fontSize = 13.sp,
                                    fontWeight = if (isPomodoroMode) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isPomodoroMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (!isPomodoroMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isPomodoroMode = false }
                            ) {
                                Text(
                                    text = "مؤقت تصاعدي حر",
                                    fontSize = 13.sp,
                                    fontWeight = if (!isPomodoroMode) FontWeight.Bold else FontWeight.Normal,
                                    color = if (!isPomodoroMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }

                        if (isPomodoroMode) {
                            Text("المدة المحددة للجلسة:", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(15, 25, 45, 60).forEach { mins ->
                                    val isSelected = selectedPomodoroMinutes == mins && customMinutesInput.isEmpty()
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                selectedPomodoroMinutes = mins
                                                customMinutesInput = ""
                                            }
                                    ) {
                                        Text(
                                            text = "$mins د",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = customMinutesInput,
                                onValueChange = {
                                    customMinutesInput = it
                                    it.toIntOrNull()?.let { v -> selectedPomodoroMinutes = v }
                                },
                                label = { Text("أو حدد مدة مخصصة (بالدقائق)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        Text("المادة أو المقرر الدراسي:", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presetSubjects.forEach { sub ->
                                FilterChip(
                                    selected = customSubject == sub,
                                    onClick = { customSubject = sub },
                                    label = { Text(sub, maxLines = 1) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = customSubject,
                            onValueChange = { customSubject = it },
                            label = { Text("أو اكتب اسم المادة يدويًا") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // Summary metric strip
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$todayMinutes دقيقة",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "إنجاز اليوم",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$totalMinutes دقيقة",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "إجمالي الوقت المسجل",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimerDialCard(
    timerState: ActiveTimerState,
    isPomodoro: Boolean,
    targetMinutes: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val progress = remember(timerState, isPomodoro, targetMinutes) {
                if (!isPomodoro || timerState.targetSeconds == 0L) {
                    0f
                } else {
                    (timerState.elapsedSeconds.toFloat() / timerState.targetSeconds.toFloat()).coerceIn(0f, 1f)
                }
            }

            val animatedProgress by animateFloatAsState(targetValue = progress, label = "timerProgress")

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 12.dp
                )

                CircularProgressIndicator(
                    progress = { if (isPomodoro) animatedProgress else 0.75f },
                    modifier = Modifier.fillMaxSize(),
                    color = if (timerState.isPaused) Color(0xFFE65100) else EmeraldBorder,
                    strokeWidth = 12.dp
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val displaySeconds = if (isPomodoro) {
                        (timerState.targetSeconds - timerState.elapsedSeconds).coerceAtLeast(0L)
                    } else {
                        timerState.elapsedSeconds
                    }

                    val mins = displaySeconds / 60
                    val secs = displaySeconds % 60
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", mins, secs),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timerState.subject,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Text(
                        text = timerState.sessionType,
                        fontSize = 11.sp,
                        color = EmeraldBorder,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val statusText = when {
                !timerState.isRunning -> "جاهز للبدء"
                timerState.isPaused -> "موقوف مؤقتًا (لا يُحسب وقت التوقف)"
                else -> "الجلسة نشطة ومستمرة بالخلفية والشاشة مغلقة"
            }
            Text(
                text = statusText,
                fontSize = 12.sp,
                color = if (timerState.isPaused) Color(0xFFE65100) else EmeraldBorder,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun TimerControls(
    timerState: ActiveTimerState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!timerState.isRunning) {
            Button(
                onClick = onStart,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldBorder),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(52.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("بدء جلسة المذاكرة", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            if (timerState.isPaused) {
                Button(
                    onClick = onResume,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldBorder),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("استئناف")
                }
            } else {
                Button(
                    onClick = onPause,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إيقاف مؤقت")
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = onStop,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("إنهاء وحفظ")
            }
        }
    }
}

@Composable
fun WorkoutTabContent() {
    val workouts = StudentWorkoutData.workouts
    var activeWorkoutForTimer by remember { mutableStateOf<StudentWorkout?>(null) }
    var selectedCategory by remember { mutableStateOf("الكل") }

    val categories = listOf(
        "الكل",
        "تمارين الظهر",
        "تمارين البطن",
        "تمارين القدم والأرجل",
        "تخفيف إجهاد المكتب",
        "تركيز وتنفس"
    )

    val filteredWorkouts = remember(selectedCategory) {
        if (selectedCategory == "الكل") workouts else workouts.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تمارين النشاط البدني والاستراحة بين المذاكرة",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "تمارين موجهة لتقوية الظهر، شد عضلات البطن، وتنشيط الدورة الدموية في القدمين والأرجل، مصحوبة برسوم بيانية لشكل الأداء الصحيح.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Horizontal Category Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        items(filteredWorkouts, key = { it.id }) { workout ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Exercise illustration image showing proper execution form
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Image(
                            painter = painterResource(id = workout.imageResId),
                            contentDescription = "شكل أداء ${workout.title}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.7f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "شكل وطريقة الأداء الصحيح",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = workout.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = workout.category,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "العضلات المستهدفة: ${workout.muscleGroup}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "الفوائد: ${workout.benefits}",
                        fontSize = 12.sp,
                        color = EmeraldBorder
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "الخطة: ${workout.durationDescription} • راحة بين الجولات: ${workout.restSeconds} ثانية",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "خطوات الأداء السليم:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        workout.instructions.forEachIndexed { idx, ins ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "${idx + 1}.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(20.dp)
                                )
                                Text(
                                    text = ins,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { activeWorkoutForTimer = workout },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("بدء المؤقت التفاعلي للجولات والراحة")
                    }
                }
            }
        }
    }

    activeWorkoutForTimer?.let { workout ->
        WorkoutInteractiveDialog(
            workout = workout,
            onDismiss = { activeWorkoutForTimer = null }
        )
    }
}

@Composable
fun WorkoutInteractiveDialog(
    workout: StudentWorkout,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var currentSet by remember { mutableIntStateOf(1) }
    var isResting by remember { mutableStateOf(false) }
    var restSecondsRemaining by remember { mutableIntStateOf(workout.restSeconds) }
    var isWorkoutComplete by remember { mutableStateOf(false) }

    LaunchedEffect(isResting, restSecondsRemaining) {
        if (isResting && restSecondsRemaining > 0) {
            delay(1000L)
            restSecondsRemaining -= 1
            if (restSecondsRemaining == 0) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isResting = false
                if (currentSet < workout.sets) {
                    currentSet += 1
                } else {
                    isWorkoutComplete = true
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(workout.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Exercise illustration inside dialog for quick reference
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Image(
                        painter = painterResource(id = workout.imageResId),
                        contentDescription = workout.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                if (isWorkoutComplete) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldBorder, modifier = Modifier.size(54.dp))
                    Text(
                        text = "عظيم! أكملت جميع الجولات بنجاح",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldBorder
                    )
                    Text("تم تجديد نشاطك البدني، يمكنك العودة الآن لجلسة المذاكرة بتركيز وصفاء ذهني.")
                } else {
                    Text(
                        text = "الجولة $currentSet من ${workout.sets}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "الهدف: ${workout.targetRepsOrSeconds}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    if (isResting) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE65100).copy(alpha = 0.15f),
                            modifier = Modifier.size(90.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${restSecondsRemaining}ث",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                        Text("فترة راحة وتنفس هادئ...")
                    } else {
                        Button(
                            onClick = {
                                if (currentSet < workout.sets) {
                                    isResting = true
                                    restSecondsRemaining = workout.restSeconds
                                } else {
                                    isWorkoutComplete = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("أنهيت الجولة $currentSet • بدء الراحة")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(if (isWorkoutComplete) "إغلاق" else "إنهاء التمرين")
            }
        }
    )
}

@Composable
fun AnalyticsTabContent(viewModel: StudentViewModel) {
    val todayMinutes by viewModel.todayStudyMinutes.collectAsState()
    val totalMinutes by viewModel.totalStudyMinutes.collectAsState()
    val subjectDurations by viewModel.subjectDurations.collectAsState()
    val typeDurations by viewModel.typeDurations.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ملخص الوقت الفعلي المنجز",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBox(label = "اليوم", value = "$todayMinutes د")
                        MetricBox(label = "الإجمالي المسجل", value = "$totalMinutes د")
                        MetricBox(label = "المتوسط بالساعات", value = String.format(Locale.US, "%.1f س", totalMinutes / 60.0))
                    }
                }
            }
        }

        // Breakdown by Session Type
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "توزيع وقت المذاكرة حسب نوع الجلسة:",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (typeDurations.isEmpty()) {
                        Text("لم تسجل جلسات مذاكرة بعد", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                    } else {
                        typeDurations.forEach { td ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(td.sessionType, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("${td.totalMinutes} دقيقة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldBorder)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val ratio = if (totalMinutes > 0) td.totalMinutes.toFloat() / totalMinutes.toFloat() else 0f
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = EmeraldBorder
                                )
                            }
                        }
                    }
                }
            }
        }

        // Breakdown by Subject
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "توزيع وقت الاستذكار حسب المادة الأكاديمية:",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (subjectDurations.isEmpty()) {
                        Text("لم تسجل جلسات بعد", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                    } else {
                        subjectDurations.forEach { sd ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(sd.subject, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("${sd.totalMinutes} دقيقة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val ratio = if (totalMinutes > 0) sd.totalMinutes.toFloat() / totalMinutes.toFloat() else 0f
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricBox(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EmeraldBorder)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}
