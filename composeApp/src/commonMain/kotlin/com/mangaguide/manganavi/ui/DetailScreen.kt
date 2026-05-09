package com.mangaguide.manganavi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mangaguide.manganavi.data.AniListApi
import com.mangaguide.manganavi.data.ReadingStatus
import com.mangaguide.manganavi.data.ReadingStatusStore
import com.mangaguide.manganavi.model.StaffEdge
import com.mangaguide.manganavi.model.Manga
import com.mangaguide.manganavi.data.openUrl
import com.mangaguide.manganavi.theme.*
import com.mangaguide.manganavi.ui.components.GenreChip

@Composable
fun DetailScreen(
    manga: Manga,
    onBack: () -> Unit,
    onMangaClick: (Manga) -> Unit,
    onStaffClick: (Int) -> Unit
) {
    val api = remember { AniListApi() }

    var currentStatus by remember(manga.id) {
        mutableStateOf(ReadingStatusStore.getStatus(manga.id))
    }
    var staffList by remember { mutableStateOf<List<StaffEdge>>(emptyList()) }
    var recommendations by remember { mutableStateOf<List<Manga>>(emptyList()) }

    LaunchedEffect(manga.id) {
        staffList = api.getMangaWithStaff(manga.id)
    }
    LaunchedEffect(manga.id) {
        recommendations = api.getRecommendations(manga.id)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PopBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // Header with cover
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(PopPink, PopOrange, PopBackground)
                        )
                    )
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 8.dp)
                ) {
                    TextButton(onClick = onBack) {
                        Text("\u2190 \u623B\u308B", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                AsyncImage(
                    model = manga.coverUrl,
                    contentDescription = manga.titleJa,
                    modifier = Modifier
                        .width(150.dp)
                        .height(215.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Title
        Text(
            text = manga.titleJa,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = PopOnSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        if (manga.titleEn != manga.titleJa) {
            Text(
                text = manga.titleEn,
                fontSize = 14.sp,
                color = PopGrayDark,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Score bar
        if (manga.score > 0) {
            ScoreSection(manga.score)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status & basic info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadge(manga.status)
            if (manga.year != null) {
                Spacer(modifier = Modifier.width(8.dp))
                InfoBadge(label = "${manga.year}\u5E74\u958B\u59CB", color = PopBlue)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Info cards grid
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PopSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "\uD83D\uDCCA \u4F5C\u54C1\u60C5\u5831",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = PopOnSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DetailInfoItem(
                        emoji = "\uD83D\uDCD6",
                        label = "\u5DFB\u6570",
                        value = manga.volumes?.let { "${it}\u5DFB" } ?: "\u2015"
                    )
                    DetailInfoItem(
                        emoji = "\uD83D\uDCC4",
                        label = "\u8A71\u6570",
                        value = manga.chapters?.let { "${it}\u8A71" } ?: "\u2015"
                    )
                    DetailInfoItem(
                        emoji = "\u2764\uFE0F",
                        label = "\u4EBA\u6C17\u5EA6",
                        value = formatPopularity(manga.popularity)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Popularity meter
                Text(
                    text = "\u4EBA\u6C17\u30E1\u30FC\u30BF\u30FC",
                    fontSize = 13.sp,
                    color = PopGrayDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                PopularityBar(manga.popularity)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Genres section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PopSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "\uD83C\uDFAD \u30B8\u30E3\u30F3\u30EB",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = PopOnSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Genre chips in flow layout
                val rows = manga.genres.chunked(3)
                rows.forEach { rowGenres ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        rowGenres.forEachIndexed { index, genre ->
                            val colorIndex = (manga.genres.indexOf(genre)) % genreColors.size
                            GenreChip(genre = genre, color = genreColors[colorIndex])
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Score comparison
        if (manga.score > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PopSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "\uD83C\uDFC5 \u8A55\u4FA1\u5206\u6790",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = PopOnSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val rating = when {
                        manga.score >= 90 -> "\u795E\u4F5C" to "\u2728 \u4F1D\u8AAC\u7D1A\u306E\u540D\u4F5C\uFF01\u8AAD\u307E\u306A\u3044\u3068\u640D\uFF01"
                        manga.score >= 80 -> "\u795E\u4F5C" to "\u2B50 \u975E\u5E38\u306B\u9AD8\u3044\u8A55\u4FA1\uFF01\u304A\u3059\u3059\u3081\uFF01"
                        manga.score >= 70 -> "\u826F\u4F5C" to "\uD83D\uDC4D \u5B89\u5B9A\u3057\u3066\u697D\u3057\u3081\u308B\u4F5C\u54C1\uFF01"
                        manga.score >= 60 -> "\u4F73\u4F5C" to "\uD83D\uDE0A \u597D\u307F\u304C\u5206\u304B\u308C\u308B\u304C\u30CF\u30DE\u308B\u4EBA\u306F\u30CF\u30DE\u308B\uFF01"
                        else -> "\u6CE8\u76EE\u4F5C" to "\uD83D\uDD0D \u72EC\u7279\u306A\u4F5C\u98A8\u3067\u6CE8\u76EE\uFF01"
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        listOf(PopYellow, PopOrange)
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${manga.score}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = rating.first,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PopOrange
                            )
                            Text(
                                text = rating.second,
                                fontSize = 13.sp,
                                color = PopGrayDark
                            )
                        }
                    }
                }
            }
        }

        // Where to read section
        if (manga.readingLinks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PopSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "\uD83D\uDCD6 \u3069\u3053\u3067\u8AAD\u3081\u308B\uFF1F",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = PopOnSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    manga.readingLinks.forEach { link ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = PopPink.copy(alpha = 0.08f),
                            onClick = { openUrl(link.url) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = link.icon, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = link.siteName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PopOnSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "\u203A",
                                    fontSize = 20.sp,
                                    color = PopPink,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Amazon purchase button
        Button(
            onClick = {
                val searchQuery = manga.titleJa.replace(" ", "+")
                val url = "https://www.amazon.co.jp/s?k=${searchQuery}+\u6F2B\u753B&tag=kixyouhueizou-22"
                openUrl(url)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9900)
            )
        ) {
            Text(
                text = "\uD83D\uDED2 Amazon\u3067\u8CFC\u5165\u3059\u308B",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Amazon.co.jp\u306E\u691C\u7D22\u7D50\u679C\u306B\u79FB\u52D5\u3057\u307E\u3059",
            fontSize = 11.sp,
            color = PopGrayMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Reading status buttons
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PopSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "\uD83D\uDCDA \u8AAD\u307F\u30EA\u30B9\u30C8",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = PopOnSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReadingStatus.entries.forEach { status ->
                        val isSelected = currentStatus == status
                        val color = when (status) {
                            ReadingStatus.WANT_TO_READ -> PopBlue
                            ReadingStatus.READING -> PopOrange
                            ReadingStatus.COMPLETED -> PopGreen
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (isSelected) {
                                        ReadingStatusStore.removeStatus(manga.id)
                                        currentStatus = null
                                    } else {
                                        ReadingStatusStore.setStatus(
                                            mangaId = manga.id,
                                            status = status,
                                            titleJa = manga.titleJa,
                                            coverUrl = manga.coverUrl,
                                            score = manga.score
                                        )
                                        currentStatus = status
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) color.copy(alpha = 0.2f) else PopGrayLight
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = status.emoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = status.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) color else PopGrayDark
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Staff section
        if (staffList.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PopSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "\u270F\uFE0F \u4F5C\u8005\u30FB\u30B9\u30BF\u30C3\u30D5",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = PopOnSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    staffList.forEach { staff ->
                        val displayName = staff.nameNative.ifBlank { staff.nameEn }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = PopPurple.copy(alpha = 0.08f),
                            onClick = { onStaffClick(staff.staffId) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PopOnSurface
                                    )
                                    Text(
                                        text = staff.role,
                                        fontSize = 12.sp,
                                        color = PopGrayDark
                                    )
                                }
                                Text(
                                    text = "\u203A",
                                    fontSize = 20.sp,
                                    color = PopPurple,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Similar manga section
        if (recommendations.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PopSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "\uD83D\uDD0D \u4F3C\u305F\u4F5C\u54C1",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = PopOnSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(recommendations) { rec ->
                            Column(
                                modifier = Modifier
                                    .width(100.dp)
                                    .clickable { onMangaClick(rec) },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = rec.coverUrl,
                                    contentDescription = rec.titleJa,
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = rec.titleJa,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PopOnSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun ScoreSection(score: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = StarYellow.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Stars
            val fullStars = (score / 20)
            val halfStar = (score % 20) >= 10
            repeat(5) { i ->
                Text(
                    text = when {
                        i < fullStars -> "\u2B50"
                        i == fullStars && halfStar -> "\u2B50"
                        else -> "\u2606"
                    },
                    fontSize = 24.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${score}%",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = StarYellow
            )
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val color = when (status) {
        "\u9023\u8F09\u4E2D" -> PopGreen
        "\u5B8C\u7D50" -> PopBlue
        "\u4F11\u8F09\u4E2D" -> PopOrange
        else -> PopGrayMedium
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = color
        )
    }
}

@Composable
private fun InfoBadge(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = color
        )
    }
}

@Composable
private fun DetailInfoItem(emoji: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = PopOnSurface
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = PopGrayDark
        )
    }
}

@Composable
private fun PopularityBar(popularity: Int) {
    val maxPop = 500000f
    val ratio = (popularity.toFloat() / maxPop).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(PopGrayLight)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(ratio)
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(PopPink, PopOrange, PopYellow)
                    ),
                    shape = RoundedCornerShape(6.dp)
                )
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "${formatPopularity(popularity)} \u30E6\u30FC\u30B6\u30FC\u304C\u30D5\u30A9\u30ED\u30FC\u4E2D",
        fontSize = 12.sp,
        color = PopGrayDark
    )
}

private fun formatPopularity(pop: Int): String {
    return when {
        pop >= 1000000 -> "${pop / 1000000}M"
        pop >= 1000 -> "${pop / 1000}K"
        else -> "$pop"
    }
}
