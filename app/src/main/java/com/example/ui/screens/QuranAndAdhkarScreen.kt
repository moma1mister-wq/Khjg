package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PrepopulationData
import com.example.data.model.DhikrItem
import com.example.data.model.Surah
import com.example.ui.StudentViewModel
import com.example.ui.components.EmeraldBorder
import com.example.ui.components.MushafFullPageView
import com.example.ui.components.ParchmentDarkGold
import com.example.ui.components.ParchmentGold
import com.example.ui.components.toEasternArabic
import kotlinx.coroutines.launch

@Composable
fun QuranAndAdhkarScreen(viewModel: StudentViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val bookmark by viewModel.quranBookmark.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("quran_adhkar_screen")
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("المصحف الشريف (٦٠٤ صفحات)", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("حصن المسلم والأذكار", fontWeight = FontWeight.Bold) }
            )
        }

        if (selectedTab == 0) {
            QuranBrowserSection(
                viewModel = viewModel,
                bookmark = bookmark
            )
        } else {
            AdhkarSection(viewModel = viewModel)
        }
    }
}

@Composable
fun QuranBrowserSection(
    viewModel: StudentViewModel,
    bookmark: com.example.data.model.QuranBookmark?
) {
    var isReadingMode by remember { mutableStateOf(false) }
    var selectedIndexTab by remember { mutableIntStateOf(0) } // 0: Surahs, 1: Juz, 2: Bookmarks
    var searchQuery by remember { mutableStateOf("") }

    val allSurahs by viewModel.allSurahs.collectAsState()
    val currentPage by viewModel.currentMushafPage.collectAsState()
    val pageAyahs by viewModel.currentPageAyahs.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    if (isReadingMode) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { isReadingMode = false }) {
                    Text(
                        "← الفهرس والبحث",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "مصحف التجويد والتدبر برواية حفص",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ParchmentDarkGold
                )
            }

            MushafFullPageView(
                pageNumber = currentPage,
                ayahs = pageAyahs,
                allSurahs = allSurahs,
                isBookmarked = bookmark?.pageNumber == currentPage,
                onSaveBookmark = { sNum, sName, aNum, pNum ->
                    viewModel.saveBookmark(sNum, sName, aNum, pNum)
                },
                onNextPage = {
                    if (currentPage < 604) viewModel.loadMushafPage(currentPage + 1)
                },
                onPrevPage = {
                    if (currentPage > 1) viewModel.loadMushafPage(currentPage - 1)
                },
                onJumpToPage = { p ->
                    viewModel.loadMushafPage(p)
                }
            )
        }
    } else {
        // Index and Search View
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quick Resume Last Reading Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isReadingMode = true
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "متابعة القراءة والورد القرآني",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "آخر موضع: صفحة ${toEasternArabic(currentPage)} ${bookmark?.let { "• سورة ${it.surahName}" } ?: ""}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        Button(
                            onClick = { isReadingMode = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فتح المصحف", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Search Bar for Ayahs & Surahs
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.searchQuranText(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("ابحث في آيات القرآن الكريم (٦٢٣٦ آية) أو اسم السورة...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                viewModel.clearQuranSearch()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح البحث")
                            }
                        }
                    },
                    singleLine = true
                )
            }

            // If Search Query is Active: Show Matching Ayahs
            if (searchQuery.trim().length >= 2) {
                item {
                    Text(
                        text = "نتائج البحث في نصوص الآيات (${searchResults.size}):",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (searchResults.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "لم يتم العثور على آيات مطابقة للبحث",
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(searchResults) { result ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.loadMushafPage(result.page)
                                    isReadingMode = true
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "سورة ${result.surahName} • آية ${toEasternArabic(result.numberInSurah)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = EmeraldBorder
                                    )
                                    Text(
                                        text = "صفحة ${toEasternArabic(result.page)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ParchmentDarkGold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = result.text,
                                    fontSize = 15.sp,
                                    lineHeight = (24).sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            } else {
                // Category Tabs for Index
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedIndexTab == 0,
                            onClick = { selectedIndexTab = 0 },
                            label = { Text("فهرس السور (١١٤)") }
                        )
                        FilterChip(
                            selected = selectedIndexTab == 1,
                            onClick = { selectedIndexTab = 1 },
                            label = { Text("فهرس الأجزاء (٣٠)") }
                        )
                        FilterChip(
                            selected = selectedIndexTab == 2,
                            onClick = { selectedIndexTab = 2 },
                            label = { Text("العلامة المرجعية") }
                        )
                    }
                }

                // Surahs Index
                if (selectedIndexTab == 0) {
                    items(allSurahs) { surah ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.loadMushafPage(surah.startPage)
                                    isReadingMode = true
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${surah.number}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "سورة ${surah.name}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${surah.revelationType} • ${surah.versesCount} آية",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ParchmentGold.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Text(
                                        text = "ص ${toEasternArabic(surah.startPage)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ParchmentDarkGold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                } else if (selectedIndexTab == 1) {
                    // Juz Index (1..30)
                    items(juzIndexList) { juzItem ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.loadMushafPage(juzItem.startPage)
                                    isReadingMode = true
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "الجزء ${toEasternArabic(juzItem.number)}: ${juzItem.title}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "يبدأ من سورة ${juzItem.startSurah}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Text(
                                    text = "صفحة ${toEasternArabic(juzItem.startPage)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldBorder
                                )
                            }
                        }
                    }
                } else {
                    // Bookmark Card
                    item {
                        if (bookmark != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.loadMushafPage(bookmark.pageNumber)
                                        isReadingMode = true
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "العلامة المرجعية المحفوظة",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Icon(Icons.Default.BookmarkAdded, contentDescription = null, tint = EmeraldBorder)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "سورة ${bookmark.surahName} • صفحة ${toEasternArabic(bookmark.pageNumber)}",
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            viewModel.loadMushafPage(bookmark.pageNumber)
                                            isReadingMode = true
                                        }
                                    ) {
                                        Text("فتح موضع العلامة")
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "لا توجد علامة مرجعية محفوظة حتى الآن. اضغط زر 'حفظ العلامة' داخل المصحف لحفظ موضع قراءتك.",
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class JuzInfo(val number: Int, val title: String, val startSurah: String, val startPage: Int)

val juzIndexList = listOf(
    JuzInfo(1, "الم (آلم)", "الفاتحة", 1),
    JuzInfo(2, "سَيَقُولُ", "البقرة", 22),
    JuzInfo(3, "تِلْكَ الرُّسُلُ", "البقرة", 42),
    JuzInfo(4, "لَنْ تَنَالُوا", "آل عمران", 62),
    JuzInfo(5, "وَالْمُحْصَنَاتُ", "النساء", 82),
    JuzInfo(6, "لَا يُحِبُّ اللَّهُ", "النساء", 102),
    JuzInfo(7, "وَإِذَا سَمِعُوا", "المائدة", 121),
    JuzInfo(8, "وَلَوْ أَنَّنَا", "الأنعام", 142),
    JuzInfo(9, "قَالَ الْمَلَأُ", "الأعراف", 162),
    JuzInfo(10, "وَاعْلَمُوا", "الأنفال", 182),
    JuzInfo(11, "يَعْتَذِرُونَ", "التوبة", 201),
    JuzInfo(12, "وَمَا مِنْ دَابَّةٍ", "هود", 222),
    JuzInfo(13, "وَمَا أُبَرِّئُ", "يوسف", 242),
    JuzInfo(14, "رُبَمَا", "الحجر", 262),
    JuzInfo(15, "سُبْحَانَ الَّذِي", "الإسراء", 282),
    JuzInfo(16, "قَالَ أَلَمْ", "الكهف", 302),
    JuzInfo(17, "اقْتَرَبَ لِلنَّاسِ", "الأنبياء", 322),
    JuzInfo(18, "قَدْ أَفْلَحَ", "المؤمنون", 342),
    JuzInfo(19, "وَقَالَ الَّذِينَ", "الفرقان", 362),
    JuzInfo(20, "أَمَّنْ خَلَقَ", "النمل", 382),
    JuzInfo(21, "اتْلُ مَا أُوحِيَ", "العنكبوت", 402),
    JuzInfo(22, "وَمَنْ يَقْنُتْ", "الأحزاب", 422),
    JuzInfo(23, "وَمَا لِيَ", "يس", 442),
    JuzInfo(24, "فَمَنْ أَظْلَمُ", "الزمر", 462),
    JuzInfo(25, "إِلَيْهِ يُرَدُّ", "فصلت", 482),
    JuzInfo(26, "حم (الأحقاف)", "الأحقاف", 502),
    JuzInfo(27, "قَالَ فَمَا خَطْبُكُمْ", "الذاريات", 522),
    JuzInfo(28, "قَدْ سَمِعَ اللَّهُ", "المجادلة", 542),
    JuzInfo(29, "تَبَارَكَ الَّذِي", "الملك", 562),
    JuzInfo(30, "عَمَّ يَتَسَاءَلُونَ", "النبأ", 582)
)

@Composable
fun AdhkarSection(viewModel: StudentViewModel) {
    val haptic = LocalHapticFeedback.current
    val dhikrCounts by viewModel.dhikrCounts.collectAsState()
    val categories = PrepopulationData.dhikrCategories
    var selectedCategoryId by remember { mutableStateOf("morning") }

    val filteredItems = remember(selectedCategoryId) {
        PrepopulationData.allDhikrItems.filter { it.categoryId == selectedCategoryId }
    }

    val completedInCategory = remember(filteredItems, dhikrCounts) {
        filteredItems.count { (dhikrCounts[it.id] ?: 0) >= it.targetCount }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("adhkar_section")
    ) {
        // Horizontal Categories Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategoryId == cat.id,
                    onClick = { selectedCategoryId = cat.id },
                    label = { Text(cat.title) }
                )
            }
        }

        // Category Summary Header Card
        val activeCategory = categories.find { it.id == selectedCategoryId }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeCategory?.title ?: "الأذكار",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$completedInCategory من ${filteredItems.size} أذكار مكتملة",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldBorder
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = activeCategory?.description ?: "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(8.dp))
                val categoryProgress = if (filteredItems.isNotEmpty()) {
                    completedInCategory.toFloat() / filteredItems.size.toFloat()
                } else 0f
                LinearProgressIndicator(
                    progress = { categoryProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = EmeraldBorder,
                    trackColor = Color.LightGray.copy(alpha = 0.4f)
                )
            }
        }

        // Dhikr Items List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredItems, key = { it.id }) { dhikr ->
                val count = dhikrCounts[dhikr.id] ?: 0
                val isCompleted = count >= dhikr.targetCount

                DhikrCardItem(
                    dhikr = dhikr,
                    currentCount = count,
                    isCompleted = isCompleted,
                    onIncrement = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.incrementDhikr(dhikr)
                    },
                    onReset = {
                        viewModel.resetDhikr(dhikr.id)
                    }
                )
            }
        }
    }
}

@Composable
fun DhikrCardItem(
    dhikr: DhikrItem,
    currentCount: Int,
    isCompleted: Boolean,
    onIncrement: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) EmeraldBorder.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isCompleted) 1.5.dp else 1.dp,
            color = if (isCompleted) EmeraldBorder else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Dhikr Arabic Text
            Text(
                text = dhikr.text,
                fontSize = 17.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Justify
            )

            if (dhikr.virtue.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "فضل الذكر: ${dhikr.virtue}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Counter & Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Counter Button
                Button(
                    onClick = onIncrement,
                    enabled = !isCompleted,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) EmeraldBorder else MaterialTheme.colorScheme.primary,
                        disabledContainerColor = EmeraldBorder
                    )
                ) {
                    if (isCompleted) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "اكتمل (${dhikr.targetCount}/${dhikr.targetCount})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "$currentCount / ${dhikr.targetCount}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تسبيح")
                    }
                }

                if (currentCount > 0) {
                    IconButton(onClick = onReset) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "إعادة ضبط العداد",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
