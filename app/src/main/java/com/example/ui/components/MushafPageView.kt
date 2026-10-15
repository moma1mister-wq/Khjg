package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Ayah
import com.example.data.model.AyahDetail
import com.example.data.model.Surah

// Palette inspired by the illuminated Tajweed Mushaf page
val ParchmentGold = Color(0xFFD4AF37)
val ParchmentDarkGold = Color(0xFF996515)
val EmeraldBorder = Color(0xFF0F5132)
val DeepTeal = Color(0xFF004D40)
val PageBgCream = Color(0xFFFFFDF8)
val PageBorderIvory = Color(0xFFF7F1E1)

// Tajweed Color Tokens
val TajweedNormal = Color(0xFF1E1E1E)
val TajweedMadd = Color(0xFF0D47A1) // Blue for Prolongation (حروف المد)
val TajweedMaddLazem = Color(0xFFB71C1C) // Red for Obligatory 6-vowel Madd
val TajweedGhunnah = Color(0xFF1B5E20) // Green for Ghunnah & Ikhfa
val TajweedQalqalah = Color(0xFF00838F) // Cyan for Qalqalah (قطب جد)

@Composable
fun MushafFullPageView(
    pageNumber: Int,
    ayahs: List<AyahDetail>,
    allSurahs: List<Surah>,
    isBookmarked: Boolean,
    onSaveBookmark: (surahNumber: Int, surahName: String, ayahNumber: Int, pageNumber: Int) -> Unit,
    onNextPage: () -> Unit,
    onPrevPage: () -> Unit,
    onJumpToPage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var fontSizeMultiplier by remember { mutableFloatStateOf(1.0f) }
    var showTajweedLegend by remember { mutableStateOf(false) }
    var useTajweedColors by remember { mutableStateOf(true) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpPageInput by remember { mutableStateOf("") }

    val firstAyah = ayahs.firstOrNull()
    val juzNumber = firstAyah?.juz ?: 1
    val hizbQuarter = firstAyah?.hizbQuarter ?: 1
    val hizbNumber = ((hizbQuarter - 1) / 4) + 1

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("mushaf_page_view")
    ) {
        // Top Info & Controls Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (fontSizeMultiplier < 1.4f) fontSizeMultiplier += 0.1f
                    }
                ) {
                    Icon(
                        Icons.Default.FormatSize,
                        contentDescription = "تكبير الخط",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (fontSizeMultiplier > 0.8f) fontSizeMultiplier -= 0.1f
                    }
                ) {
                    Text("A-", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                IconButton(
                    onClick = { useTajweedColors = !useTajweedColors }
                ) {
                    Icon(
                        Icons.Default.ColorLens,
                        contentDescription = "تبديل ألوان التجويد",
                        tint = if (useTajweedColors) EmeraldBorder else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = { showTajweedLegend = !showTajweedLegend }
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "دليل أحكام التجويد",
                        tint = DeepTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { showJumpDialog = true }
                ) {
                    Icon(
                        Icons.Default.NearMe,
                        contentDescription = "الانتقال إلى صفحة",
                        tint = ParchmentDarkGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isBookmarked) EmeraldBorder else ParchmentGold.copy(alpha = 0.2f),
                modifier = Modifier.clickable {
                    val sNum = firstAyah?.surahNumber ?: 1
                    val sName = firstAyah?.surahName ?: "الفاتحة"
                    val aNum = firstAyah?.numberInSurah ?: 1
                    onSaveBookmark(sNum, sName, aNum, pageNumber)
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isBookmarked) Icons.Default.BookmarkAdded else Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = if (isBookmarked) Color.White else ParchmentDarkGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isBookmarked) "محفوظة" else "حفظ العلامة",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBookmarked) Color.White else ParchmentDarkGold
                    )
                }
            }
        }

        // Tajweed Legend
        AnimatedVisibility(visible = showTajweedLegend) {
            TajweedLegendCard()
        }

        // Top Page Header (Juz & Hizb)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "الجُزْءُ ${toArabicSpelledCount(juzNumber)} • الحِزْبُ $hizbNumber",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ParchmentDarkGold
            )
            val surahNamesOnPage = ayahs.map { it.surahName }.distinct().joinToString("، ")
            Text(
                text = surahNamesOnPage.ifEmpty { "المصحف الشريف" },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldBorder
            )
        }

        // Outer Illuminated Quran Page Frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(PageBgCream)
                .border(3.dp, ParchmentGold, RoundedCornerShape(16.dp))
                .border(6.dp, EmeraldBorder.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Group ayahs by surah
                val groupedBySurah = ayahs.groupBy { it.surahNumber }

                groupedBySurah.forEach { (surahNum, surahAyahs) ->
                    val surahMeta = allSurahs.find { it.number == surahNum }
                    val startsHere = surahAyahs.any { it.numberInSurah == 1 }

                    if (startsHere && surahMeta != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        SurahHeaderPlaque(surah = surahMeta)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Basmalah (except At-Tawbah=9 and Al-Fatihah=1 where ayah 1 is Basmalah)
                        if (surahNum != 9 && surahNum != 1) {
                            BasmalahPlaque(useTajweed = useTajweedColors, multiplier = fontSizeMultiplier)
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    // Render Ayahs block for this surah
                    PageAyahsBlock(
                        ayahs = surahAyahs,
                        useTajweed = useTajweedColors,
                        multiplier = fontSizeMultiplier
                    )

                    val isLastSurahOnPage = (surahNum == groupedBySurah.keys.last())
                    val reachesEnd = surahMeta != null && surahAyahs.any { it.numberInSurah == surahMeta.versesCount }
                    if (reachesEnd && surahMeta != null && !isLastSurahOnPage) {
                        Spacer(modifier = Modifier.height(8.dp))
                        SurahFooterPlaque(surah = surahMeta)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }

        // Bottom Page Flipping Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Note: In Arabic Quran layout, Next Page is to the left (decreasing page or reading direction)
            IconButton(
                onClick = onPrevPage,
                enabled = pageNumber > 1
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "الصفحة السابقة")
                    Text("السابقة", fontSize = 12.sp)
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PageBorderIvory,
                border = androidx.compose.foundation.BorderStroke(1.dp, ParchmentGold),
                modifier = Modifier.clickable { showJumpDialog = true }
            ) {
                Text(
                    text = "صفحة ${toEasternArabic(pageNumber)} من ٦٠٤",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ParchmentDarkGold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            IconButton(
                onClick = onNextPage,
                enabled = pageNumber < 604
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("التالية", fontSize = 12.sp)
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "الصفحة التالية")
                }
            }
        }
    }

    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("الانتقال إلى صفحة في المصحف") },
            text = {
                Column {
                    Text("أدخل رقم الصفحة من ١ إلى ٦٠٤:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jumpPageInput,
                        onValueChange = { jumpPageInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("مثال: 50") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = jumpPageInput.toIntOrNull()
                        if (p != null && p in 1..604) {
                            onJumpToPage(p)
                            showJumpDialog = false
                            jumpPageInput = ""
                        }
                    }
                ) {
                    Text("انتقال")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun PageAyahsBlock(
    ayahs: List<AyahDetail>,
    useTajweed: Boolean,
    multiplier: Float
) {
    val quranFlowText = buildAnnotatedString {
        ayahs.forEach { ayah ->
            val styledAyah = if (useTajweed) formatTajweedText(ayah.text) else AnnotatedString(ayah.text)
            append(styledAyah)
            append(" ")
            withStyle(
                SpanStyle(
                    color = ParchmentDarkGold,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(" ﴿${toEasternArabic(ayah.numberInSurah)}﴾ ")
            }
        }
    }

    Text(
        text = quranFlowText,
        fontSize = (19 * multiplier).sp,
        lineHeight = (36 * multiplier).sp,
        textAlign = TextAlign.Justify,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

// Backward compatibility overload for existing single-surah calls
@Composable
fun MushafPageView(
    surah: Surah,
    ayahs: List<Ayah>,
    isBookmarked: Boolean,
    onSaveBookmark: () -> Unit,
    onNextSurah: () -> Unit,
    onPrevSurah: () -> Unit,
    modifier: Modifier = Modifier
) {
    val details = ayahs.map {
        AyahDetail(
            number = it.numberInSurah,
            surahNumber = surah.number,
            surahName = surah.name,
            numberInSurah = it.numberInSurah,
            juz = 1,
            page = surah.startPage,
            hizbQuarter = 1,
            text = it.text
        )
    }
    MushafFullPageView(
        pageNumber = surah.startPage,
        ayahs = details,
        allSurahs = listOf(surah),
        isBookmarked = isBookmarked,
        onSaveBookmark = { _, _, _, _ -> onSaveBookmark() },
        onNextPage = onNextSurah,
        onPrevPage = onPrevSurah,
        onJumpToPage = {},
        modifier = modifier
    )
}

@Composable
fun SurahHeaderPlaque(surah: Surah) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        ParchmentDarkGold,
                        ParchmentGold,
                        EmeraldBorder,
                        ParchmentGold,
                        ParchmentDarkGold
                    )
                )
            )
            .border(2.dp, ParchmentGold, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = "سُورَةُ ${surah.name}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "${surah.revelationType} • آيَاتُهَا ${toEasternArabic(surah.versesCount)} • تَرْتِيبُهَا ${toEasternArabic(surah.number)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFFFF3CD)
            )
        }
    }
}

@Composable
fun BasmalahPlaque(useTajweed: Boolean, multiplier: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(10.dp))
            .background(PageBorderIvory)
            .border(1.5.dp, ParchmentGold, RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (useTajweed) {
            val basmalahAnnotated = buildAnnotatedString {
                append("بِسْمِ ")
                withStyle(SpanStyle(color = TajweedNormal, fontWeight = FontWeight.Bold)) {
                    append("ٱللَّهِ ")
                }
                withStyle(SpanStyle(color = TajweedMadd, fontWeight = FontWeight.Bold)) {
                    append("ٱلرَّحْمَٰنِ ")
                }
                withStyle(SpanStyle(color = TajweedMadd, fontWeight = FontWeight.Bold)) {
                    append("ٱلرَّحِيمِ")
                }
            }
            Text(
                text = basmalahAnnotated,
                fontSize = (20 * multiplier).sp,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                fontSize = (20 * multiplier).sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldBorder,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SurahFooterPlaque(surah: Surah) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.65f)
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PageBorderIvory)
            .border(1.5.dp, ParchmentGold, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "وَآيَاتُهَا ${toArabicSpelledCount(surah.versesCount)}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = EmeraldBorder
        )
    }
}

@Composable
fun TajweedLegendCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = PageBorderIvory),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = TajweedMadd, label = "المد الطبيعي والمتصل")
            LegendItem(color = TajweedMaddLazem, label = "المد اللازم المشبع")
            LegendItem(color = TajweedGhunnah, label = "الغنة والإخفاء")
            LegendItem(color = TajweedQalqalah, label = "القلقلة")
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = color, modifier = Modifier.size(10.dp)) {}
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

fun formatTajweedText(raw: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val words = raw.split(" ")
    words.forEachIndexed { idx, word ->
        when {
            word.contains("الضَّالِّينَ") || word.contains("الصَّاخَّةُ") || word.contains("الطَّامَّةُ") -> {
                builder.pushStyle(SpanStyle(color = TajweedMaddLazem, fontWeight = FontWeight.Bold))
                builder.append(word)
                builder.pop()
            }
            word.contains("الرَّحِيمِ") || word.contains("نَسْتَعِينُ") || word.contains("الْمُسْتَقِيمَ") || word.contains("الدِّينِ") || word.contains("الْعَالَمِينَ") -> {
                builder.pushStyle(SpanStyle(color = TajweedMadd, fontWeight = FontWeight.SemiBold))
                builder.append(word)
                builder.pop()
            }
            word.contains("أَنْعَمْتَ") || word.contains("مِن") || word.contains("عَن") || word.contains("إِنَّ") -> {
                builder.pushStyle(SpanStyle(color = TajweedGhunnah, fontWeight = FontWeight.SemiBold))
                builder.append(word)
                builder.pop()
            }
            word.contains("الْفَلَقِ") || word.contains("وَقَبَ") || word.contains("الْعُقَدِ") || word.contains("حَسَدَ") || word.contains("أَحَدٌ") || word.contains("الصَّمَدُ") || word.contains("يَلِدْ") || word.contains("يُولَدْ") -> {
                builder.pushStyle(SpanStyle(color = TajweedQalqalah, fontWeight = FontWeight.SemiBold))
                builder.append(word)
                builder.pop()
            }
            else -> {
                builder.pushStyle(SpanStyle(color = TajweedNormal))
                builder.append(word)
                builder.pop()
            }
        }
        if (idx < words.size - 1) {
            builder.append(" ")
        }
    }
    return builder.toAnnotatedString()
}

fun toEasternArabic(number: Int): String {
    val arabicDigits = arrayOf("٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩")
    return number.toString().map { arabicDigits[it - '0'] }.joinToString("")
}

fun toArabicSpelledCount(count: Int): String {
    return when (count) {
        1 -> "وَاحِدٌ"
        2 -> "اثْنَانِ"
        3 -> "ثَلَاثٌ"
        4 -> "أَرْبَعٌ"
        5 -> "خَمْسٌ"
        6 -> "سِتٌّ"
        7 -> "سَبْعٌ"
        8 -> "ثَمَانٍ"
        11 -> "إِحْدَى عَشْرَةَ"
        30 -> "ثَلَاثُونَ"
        83 -> "ثَلَاثٌ وَثَمَانُونَ"
        110 -> "عَشْرٌ وَمِائَةٌ"
        114 -> "أَرْبَعَ عَشْرَةَ وَمِائَةٌ"
        286 -> "مِائَتَانِ وَسِتٌّ وَثَمَانُونَ"
        else -> "$count"
    }
}
