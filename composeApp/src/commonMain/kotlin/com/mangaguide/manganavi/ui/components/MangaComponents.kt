package com.mangaguide.manganavi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mangaguide.manganavi.model.Manga
import com.mangaguide.manganavi.theme.*

@Composable
fun MangaRankCard(
    rank: Int,
    manga: Manga,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rankBrush = when (rank) {
        1 -> Brush.linearGradient(listOf(Gold, Color(0xFFFF8A00)))
        2 -> Brush.linearGradient(listOf(Color(0xFFE9EDF5), Color(0xFF98A2B8)))
        3 -> Brush.linearGradient(listOf(Color(0xFFFFB06A), Color(0xFF9B4C18)))
        else -> Brush.linearGradient(listOf(Ink, InkSoft))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .shadow(10.dp, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(92.dp)
                    .background(rankBrush, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "#",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = rank.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = if (rank < 10) 24.sp else 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            AsyncImage(
                model = manga.coverUrl,
                contentDescription = manga.titleJa,
                modifier = Modifier
                    .width(66.dp)
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScorePill(score = manga.score)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = manga.status,
                        color = PopGrayDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = manga.titleJa,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Ink,
                    lineHeight = 19.sp
                )
                if (manga.titleEn != manga.titleJa && manga.titleEn != "Unknown") {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = manga.titleEn,
                        fontSize = 11.sp,
                        color = PopGrayDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    manga.genres.take(2).forEach { genre ->
                        GenreChip(genre = genre, small = true)
                    }
                }
            }
        }
    }
}

@Composable
fun MangaGridCard(
    manga: Manga,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(136.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = manga.coverUrl,
                    contentDescription = manga.titleJa,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(184.dp)
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    ScorePill(score = manga.score)
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = manga.titleJa,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Ink,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatPopularity(manga.popularity),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PopGrayDark
                )
            }
        }
    }
}

@Composable
fun GenreChip(
    genre: String,
    small: Boolean = false,
    color: Color = HeroRed,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = genre,
            color = color,
            fontSize = if (small) 10.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(
                horizontal = if (small) 7.dp else 12.dp,
                vertical = if (small) 3.dp else 6.dp
            )
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    emoji: String = "",
    onSeeAll: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = if (emoji.isBlank()) title else "$emoji $title",
                fontWeight = FontWeight.Black,
                fontSize = 21.sp,
                color = Ink
            )
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(54.dp)
                    .height(4.dp)
                    .background(Brush.horizontalGradient(listOf(HeroRed, Gold)), CircleShape)
            )
        }
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text(
                    text = "もっと見る",
                    color = HeroRed,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(36.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = HeroRed, strokeWidth = 4.dp)
    }
}

@Composable
private fun ScorePill(score: Int) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Ink
    ) {
        Text(
            text = if (score > 0) "★ $score" else "★ --",
            color = Gold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun formatPopularity(popularity: Int): String {
    return when {
        popularity >= 1000000 -> "${popularity / 1000000}M readers"
        popularity >= 1000 -> "${popularity / 1000}K readers"
        popularity > 0 -> "$popularity readers"
        else -> "new signal"
    }
}
