package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LectureEntity
import com.example.ui.StudentViewModel
import kotlinx.coroutines.launch

@Composable
fun ScheduleScreen(viewModel: StudentViewModel) {
    val allLectures by viewModel.allLectures.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAddLectureDialog by remember { mutableStateOf(false) }
    var lectureToEdit by remember { mutableStateOf<LectureEntity?>(null) }

    val daysList = listOf(
        1 to "السبت",
        2 to "الأحد",
        3 to "الاثنين",
        4 to "الثلاثاء",
        5 to "الأربعاء",
        6 to "الخميس",
        7 to "الجمعة"
    )

    var selectedSectionFilter by remember { mutableStateOf("الكل") }

    val currentDayLectures = remember(allLectures, selectedDay, selectedSectionFilter) {
        allLectures.filter { lec ->
            if (lec.dayOfWeek != selectedDay) return@filter false
            if (selectedSectionFilter == "الكل") return@filter true
            // If the lecture is general or group A (not section specific) or matches selected section
            val isSec1 = lec.subject.contains("Sec 1", ignoreCase = true) || lec.department.contains("سكشن 1")
            val isSec2 = lec.subject.contains("Sec 2", ignoreCase = true) || lec.department.contains("سكشن 2")
            if (!isSec1 && !isSec2) true // general lecture for all
            else if (selectedSectionFilter == "سكشن 1") isSec1
            else isSec2
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddLectureDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_lecture_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة موعد جامعي")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Faculty Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "كلية الحاسبات والذكاء الاصطناعي - جامعة الأزهر",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "الفرقة الثانية • قسم الذكاء الاصطناعي • خريف 2026",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // Day selector tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                daysList.forEach { (dayId, dayName) ->
                    val isSelected = selectedDay == dayId
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { viewModel.setSelectedDay(dayId) }
                            .testTag("day_tab_$dayId")
                    ) {
                        Text(
                            text = dayName,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Section filter chips (الكل / سكشن 1 / سكشن 2)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("تصفية السكشن:", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                listOf("الكل", "سكشن 1", "سكشن 2").forEach { sec ->
                    val isSel = selectedSectionFilter == sec
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { selectedSectionFilter = sec }
                    ) {
                        Text(
                            text = sec,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Info bar with total lectures for day & reset option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مواعيد يوم ${StudentViewModel.getDayName(selectedDay)} (${currentDayLectures.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                TextButton(
                    onClick = {
                        viewModel.resetScheduleToDefault()
                        scope.launch {
                            snackbarHostState.showSnackbar("تم استعادة الجدول الجامعي الأكاديمي")
                        }
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("استعادة الافتراضي", fontSize = 12.sp)
                }
            }

            if (currentDayLectures.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "لا توجد محاضرات أو معامل مسجلة في هذا اليوم",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = { showAddLectureDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إضافة موعد جامعي لهذا اليوم")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentDayLectures, key = { it.id }) { lecture ->
                        LectureCard(
                            lecture = lecture,
                            onEdit = { lectureToEdit = lecture },
                            onDelete = {
                                viewModel.deleteLecture(lecture)
                                scope.launch {
                                    snackbarHostState.showSnackbar("تم حذف الموعد")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddLectureDialog) {
        LectureFormDialog(
            initialDay = selectedDay,
            onDismiss = { showAddLectureDialog = false },
            onConfirm = { newLec ->
                viewModel.addLecture(newLec)
                showAddLectureDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("تمت إضافة ${newLec.subject}")
                }
            }
        )
    }

    lectureToEdit?.let { lecture ->
        LectureFormDialog(
            initialDay = lecture.dayOfWeek,
            initialLecture = lecture,
            onDismiss = { lectureToEdit = null },
            onConfirm = { updatedLec ->
                viewModel.updateLecture(updatedLec)
                lectureToEdit = null
                scope.launch {
                    snackbarHostState.showSnackbar("تم تحديث الموعد")
                }
            }
        )
    }
}

@Composable
fun LectureCard(
    lecture: LectureEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lecture_card_${lecture.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (lecture.type) {
                        "معمل" -> MaterialTheme.colorScheme.tertiaryContainer
                        "سكشن" -> MaterialTheme.colorScheme.secondaryContainer
                        "تمرين" -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        text = lecture.type,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (lecture.type) {
                            "معمل" -> MaterialTheme.colorScheme.onTertiaryContainer
                            "سكشن" -> MaterialTheme.colorScheme.onSecondaryContainer
                            "تمرين" -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = lecture.subject,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${com.example.util.TimeFormatHelper.format12Hour(lecture.startTime, true)} إلى ${com.example.util.TimeFormatHelper.format12Hour(lecture.endTime, true)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "المكان: ${lecture.hall}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (lecture.instructor.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "المحاضر: ${lecture.instructor}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (lecture.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ملاحظات: ${lecture.notes}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun LectureFormDialog(
    initialDay: Int,
    initialLecture: LectureEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (LectureEntity) -> Unit
) {
    var subject by remember { mutableStateOf(initialLecture?.subject ?: "") }
    var type by remember { mutableStateOf(initialLecture?.type ?: "محاضرة") }
    var hall by remember { mutableStateOf(initialLecture?.hall ?: "") }
    var startTime by remember { mutableStateOf(initialLecture?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(initialLecture?.endTime ?: "11:00") }
    var instructor by remember { mutableStateOf(initialLecture?.instructor ?: "") }
    var department by remember { mutableStateOf(initialLecture?.department ?: "") }
    var notes by remember { mutableStateOf(initialLecture?.notes ?: "") }

    val types = listOf("محاضرة", "معمل", "سكشن", "تمرين")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialLecture == null) "إضافة موعد بالجدول" else "تعديل الموعد",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("اسم المادة / المقرر") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("نوع الموعد:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    types.forEach { t ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (type == t) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { type = t }
                        ) {
                            Text(
                                text = t,
                                fontSize = 12.sp,
                                color = if (type == t) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("البدء (HH:mm)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("الانتهاء (HH:mm)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = hall,
                    onValueChange = { hall = it },
                    label = { Text("القاعة / المدرج / المعمل") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = instructor,
                    onValueChange = { instructor = it },
                    label = { Text("المحاضر أو المعيد") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subject.isNotBlank() && hall.isNotBlank()) {
                        val lec = initialLecture?.copy(
                            subject = subject,
                            type = type,
                            hall = hall,
                            startTime = startTime,
                            endTime = endTime,
                            instructor = instructor,
                            department = department,
                            notes = notes
                        ) ?: LectureEntity(
                            subject = subject,
                            type = type,
                            dayOfWeek = initialDay,
                            startTime = startTime,
                            endTime = endTime,
                            hall = hall,
                            instructor = instructor,
                            department = department,
                            notes = notes
                        )
                        onConfirm(lec)
                    }
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
