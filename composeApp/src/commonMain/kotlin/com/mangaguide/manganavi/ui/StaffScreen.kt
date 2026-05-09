package com.mangaguide.manganavi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mangaguide.manganavi.data.AniListApi
import com.mangaguide.manganavi.model.Manga
import com.mangaguide.manganavi.model.StaffInfo
import com.mangaguide.manganavi.theme.*
import com.mangaguide.manganavi.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun StaffScreen(
    staffId: Int,
    onMangaClick: (Manga) -> Unit,
    onBack: () -> Unit
) {
    val api = remember { AniListApi() }
    var staffInfo by remember { mutableStateOf<StaffInfo?>(null) }
    var mangaList by remember { mutableStateOf<List<Manga>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(staffId) {
        scope.launch {
            try {
                val (info, works) = api.getStaffWorks(staffId)
                staffInfo = info
                mangaList = works
            } catch (_: Exception) {}
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
            color = Color.Transparent,
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(PopOrange, PopPink)
                        )
                    )
                    .padding(top = 48.dp, bottom = 20.dp)
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

                    if (staffInfo != null) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar circle with initial
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        color = Color.White.copy(alpha = 0.25f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "\u270D\uFE0F",
                                    fontSize = 24.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                if (staffInfo!!.nameNative.isNotBlank()) {
                                    Text(
                                        text = staffInfo!!.nameNative,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                if (staffInfo!!.nameEn.isNotBlank() && staffInfo!!.nameEn != staffInfo!!.nameNative) {
                                    Text(
                                        text = staffInfo!!.nameEn,
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "\uD83D\uDCDA \u4F5C\u54C1\u4E00\u89A7 (${mangaList.size}\u4F5C)",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else {
                        Text(
                            text = "\uD83D\uDC64 \u4F5C\u8005\u60C5\u5831",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }

        if (isLoading) {
            LoadingIndicator()
        } else if (mangaList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "\uD83D\uDCDA", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "\u4F5C\u54C1\u304C\u898B\u3064\u304B\u308A\u307E\u305B\u3093\u3067\u3057\u305F",
                        fontSize = 16.sp,
                        color = PopGrayDark,
                        textAlign = TextAlign.Center
                    )
                }
            }
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
