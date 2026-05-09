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

@Composable
fun HiddenGemsScreen(
    onMangaClick: (Manga) -> Unit,
    onBack: () -> Unit
) {
    val api = remember { AniListApi() }
    var mangaList by remember { mutableStateOf<List<Manga>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentPage by remember { mutableStateOf(1) }
    var isLoadingMore by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun loadPage(page: Int, append: Boolean = false) {
        if (append) isLoadingMore = true else isLoading = true
        scope.launch {
            val result = try {
                api.getHiddenGems(page = page, perPage = 20)
            } catch (_: Exception) {
                emptyList()
            }
            if (append) {
                mangaList = mangaList + result
                isLoadingMore = false
            } else {
                mangaList = result
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadPage(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PopBackground)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent,
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(PopPurple, PopPink)
                        )
                    )
                    .padding(top = 48.dp, bottom = 16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onBack) {
                            Text("\u2190 \u623B\u308B", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Text(
                        text = "\uD83D\uDC8E \u96A0\u308C\u305F\u540D\u4F5C",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Text(
                        text = "\u9AD8\u30B9\u30B3\u30A2\u306A\u306E\u306B\u8A18\u4E8B\u5C11\u3081\u306E\u7A74\u5834\u4F5C\u54C1",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }

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

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingMore) {
                            CircularProgressIndicator(color = PopPurple)
                        } else {
                            OutlinedButton(
                                onClick = {
                                    currentPage++
                                    loadPage(currentPage, append = true)
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PopPurple)
                            ) {
                                Text(
                                    text = "\u3082\u3063\u3068\u898B\u308B",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}
