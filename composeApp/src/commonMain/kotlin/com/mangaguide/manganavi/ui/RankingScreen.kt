package com.mangaguide.manganavi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangaguide.manganavi.data.AniListApi
import com.mangaguide.manganavi.model.Manga
import com.mangaguide.manganavi.theme.*
import com.mangaguide.manganavi.ui.components.*
import kotlinx.coroutines.launch

enum class RankingType(val label: String, val headline: String) {
    SCORE("高評価", "読者評価が強い作品"),
    POPULAR("人気", "読まれている作品"),
    TRENDING("急上昇", "いま動いている作品")
}

@Composable
fun RankingScreen(
    onMangaClick: (Manga) -> Unit,
    onBack: () -> Unit
) {
    val api = remember { AniListApi() }
    var selectedType by remember { mutableStateOf(RankingType.SCORE) }
    var mangaList by remember { mutableStateOf<List<Manga>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var page by remember { mutableStateOf(1) }
    var isLoadingMore by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun loadData(reset: Boolean) {
        if (reset) {
            page = 1
            isLoading = true
        } else {
            isLoadingMore = true
        }
        scope.launch {
            val targetPage = if (reset) 1 else page + 1
            val result = when (selectedType) {
                RankingType.SCORE -> api.getTopManga(page = targetPage, perPage = 30)
                RankingType.POPULAR -> api.getPopularManga(page = targetPage, perPage = 30)
                RankingType.TRENDING -> api.getTrendingManga(page = targetPage, perPage = 30)
            }
            mangaList = if (reset) result else (mangaList + result).distinctBy { it.id }
            page = targetPage
            isLoading = false
            isLoadingMore = false
        }
    }

    LaunchedEffect(selectedType) {
        loadData(reset = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PopBackground)
    ) {
        RankingHeader(
            selectedType = selectedType,
            count = mangaList.size,
            onBack = onBack
        )

        RankingTabs(
            selectedType = selectedType,
            onSelect = { selectedType = it }
        )

        if (isLoading) {
            LoadingIndicator()
        } else {
            LazyColumn {
                item {
                    RankingDigest(selectedType = selectedType, mangaList = mangaList)
                }
                itemsIndexed(mangaList) { index, manga ->
                    MangaRankCard(
                        rank = index + 1,
                        manga = manga,
                        onClick = { onMangaClick(manga) }
                    )
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingMore) {
                            CircularProgressIndicator(color = HeroRed)
                        } else {
                            Button(
                                onClick = { loadData(reset = false) },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Ink)
                            ) {
                                Text("さらにランキングを見る", color = Paper, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun RankingHeader(
    selectedType: RankingType,
    count: Int,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(Ink, HeroRedDark, HeroRed)
                )
            )
            .padding(top = 48.dp, bottom = 18.dp, start = 16.dp, end = 16.dp)
    ) {
        Column {
            TextButton(onClick = onBack) {
                Text("← 戻る", color = Paper, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "MANGA RANKING",
                color = Gold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp
            )
            Text(
                text = selectedType.headline,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 31.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderPill("JP Manga")
                HeaderPill("${count.coerceAtLeast(30)} titles")
                HeaderPill("AniList signal")
            }
        }
    }
}

@Composable
private fun HeaderPill(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.16f)
    ) {
        Text(
            text = text,
            color = Paper,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun RankingTabs(
    selectedType: RankingType,
    onSelect: (RankingType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RankingType.entries.forEach { type ->
            val selected = selectedType == type
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                color = if (selected) Ink else Panel,
                shadowElevation = if (selected) 6.dp else 0.dp,
                onClick = { onSelect(type) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = type.label,
                        color = if (selected) Gold else Ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RankingDigest(selectedType: RankingType, mangaList: List<Manga>) {
    val top = mangaList.firstOrNull()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(22.dp),
        color = Ink
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "現在の1位",
                    color = Gold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = top?.titleJa ?: selectedType.headline,
                    color = Paper,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2
                )
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HeroRed
            ) {
                Text(
                    text = top?.score?.let { "★ $it" } ?: "集計中",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}
