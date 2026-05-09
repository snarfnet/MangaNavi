package com.mangaguide.manganavi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangaguide.manganavi.data.AniListApi
import com.mangaguide.manganavi.model.Manga
import com.mangaguide.manganavi.model.MangaGenre
import com.mangaguide.manganavi.theme.*
import com.mangaguide.manganavi.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun GenreScreen(
    onMangaClick: (Manga) -> Unit,
    onBack: () -> Unit
) {
    val api = remember { AniListApi() }
    var selectedGenre by remember { mutableStateOf<MangaGenre?>(null) }
    var mangaList by remember { mutableStateOf<List<Manga>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedGenre) {
        selectedGenre?.let { genre ->
            isLoading = true
            mangaList = api.getMangaByGenre(genre.aniListName, perPage = 20)
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PopBackground)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PopGreen,
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(top = 48.dp, bottom = 16.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        if (selectedGenre != null) selectedGenre = null else onBack()
                    }) {
                        Text("\u2190 \u623B\u308B", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = "\uD83C\uDFAD ${selectedGenre?.displayName ?: "\u30B8\u30E3\u30F3\u30EB\u5225"}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        if (selectedGenre == null) {
            // Genre grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(MangaGenre.entries.toList()) { genre ->
                    val colorIndex = genre.ordinal % genreColors.size
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = genreColors[colorIndex].copy(alpha = 0.15f)
                        ),
                        onClick = { selectedGenre = genre }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = genre.displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = genreColors[colorIndex],
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            if (isLoading) {
                LoadingIndicator()
            } else {
                LazyColumn {
                    itemsIndexed(mangaList) { index, manga ->
                        MangaRankCard(
                            rank = index + 1,
                            manga = manga,
                            onClick = { onMangaClick(manga) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}
