package com.mangaguide.manganavi.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangaguide.manganavi.data.AniListApi
import com.mangaguide.manganavi.model.Manga
import com.mangaguide.manganavi.theme.*
import com.mangaguide.manganavi.ui.components.*
import kotlinx.coroutines.launch
import manganavi.composeapp.generated.resources.Res
import manganavi.composeapp.generated.resources.manga_hero
import org.jetbrains.compose.resources.painterResource

@Composable
fun HomeScreen(
    onMangaClick: (Manga) -> Unit,
    onRankingClick: () -> Unit,
    onGenreClick: () -> Unit,
    onLuckyClick: () -> Unit,
    onHiddenGemsClick: () -> Unit = {},
    onAwardsClick: () -> Unit = {},
    onReadingListClick: () -> Unit = {}
) {
    val api = remember { AniListApi() }
    var trending by remember { mutableStateOf<List<Manga>>(emptyList()) }
    var hallOfFame by remember { mutableStateOf<List<Manga>>(emptyList()) }
    var topRated by remember { mutableStateOf<List<Manga>>(emptyList()) }
    var completedGems by remember { mutableStateOf<List<Manga>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                trending = api.getTrendingManga(perPage = 15)
                val seenIds = trending.map { it.id }.toMutableSet()
                hallOfFame = api.getHallOfFame(perPage = 18).filter { it.id !in seenIds }.take(10)
                seenIds.addAll(hallOfFame.map { it.id })
                topRated = api.getTopManga(perPage = 15).filter { it.id !in seenIds }.take(5)
                seenIds.addAll(topRated.map { it.id })
                completedGems = api.getCompletedGems(perPage = 18).filter { it.id !in seenIds }.take(10)
            } catch (_: Exception) {
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PopBackground)
            .verticalScroll(rememberScrollState())
    ) {
        HeroSection(onRankingClick = onRankingClick)

        ActionGrid(
            onRankingClick = onRankingClick,
            onGenreClick = onGenreClick,
            onLuckyClick = onLuckyClick,
            onHiddenGemsClick = onHiddenGemsClick,
            onAwardsClick = onAwardsClick,
            onReadingListClick = onReadingListClick
        )

        if (isLoading) {
            LoadingIndicator()
        } else {
            SectionHeader(title = "今動いている漫画", emoji = "🔥")
            MangaRail(manga = trending, onMangaClick = onMangaClick)

            Spacer(modifier = Modifier.height(22.dp))

            SectionHeader(title = "殿堂入り名作", emoji = "🏆")
            MangaRail(manga = hallOfFame, onMangaClick = onMangaClick)

            Spacer(modifier = Modifier.height(22.dp))

            SectionHeader(title = "スコア上位", emoji = "⚡", onSeeAll = onRankingClick)
            topRated.forEachIndexed { index, manga ->
                MangaRankCard(
                    rank = index + 1,
                    manga = manga,
                    onClick = { onMangaClick(manga) }
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            SectionHeader(title = "完結済みの強作", emoji = "✓")
            MangaRail(manga = completedGems, onMangaClick = onMangaClick)

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun HeroSection(onRankingClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(430.dp)
            .background(Ink)
    ) {
        Image(
            painter = painterResource(Res.drawable.manga_hero),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.18f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.86f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Gold
            ) {
                Text(
                    text = "AI KEY VISUAL",
                    color = Ink,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Text(
                text = "MangaNavi",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 42.sp
            )
            Text(
                text = "読むべき一冊を、ランキングで一瞬で見つける。",
                color = Paper,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp
            )
            Button(
                onClick = onRankingClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HeroRed),
                modifier = Modifier.height(54.dp)
            ) {
                Text(
                    text = "ランキングを見る",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun ActionGrid(
    onRankingClick: () -> Unit,
    onGenreClick: () -> Unit,
    onLuckyClick: () -> Unit,
    onHiddenGemsClick: () -> Unit,
    onAwardsClick: () -> Unit,
    onReadingListClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile("🏆", "総合ランキング", "スコア・人気・勢い", HeroRed, onRankingClick, Modifier.weight(1f))
            ActionTile("🎭", "ジャンル", "気分で探す", Emerald, onGenreClick, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile("💎", "隠れた名作", "穴場を掘る", Violet, onHiddenGemsClick, Modifier.weight(1f))
            ActionTile("📚", "マイリスト", "読みたいを管理", Cyan, onReadingListClick, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile("🔮", "今日の一冊", "運命で選ぶ", Flame, onLuckyClick, Modifier.weight(1f))
            ActionTile("🏅", "受賞作", "信頼の名作", Gold, onAwardsClick, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ActionTile(
    mark: String,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(86.dp),
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.13f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(text = mark, fontSize = 22.sp, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = PopGrayDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MangaRail(manga: List<Manga>, onMangaClick: (Manga) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(manga) { item ->
            MangaGridCard(manga = item, onClick = { onMangaClick(item) })
        }
    }
}
