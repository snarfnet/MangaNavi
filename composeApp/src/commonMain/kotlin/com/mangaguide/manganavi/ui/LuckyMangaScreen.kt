package com.mangaguide.manganavi.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mangaguide.manganavi.data.AniListApi
import com.mangaguide.manganavi.model.Manga
import com.mangaguide.manganavi.model.MangaGenre
import com.mangaguide.manganavi.theme.*
import com.mangaguide.manganavi.ui.components.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*

data class LuckyResult(
    val manga: Manga,
    val fortune: String,
    val luckyGenre: String,
    val compatibility: Int,
    val message: String
)

@Composable
fun LuckyMangaScreen(
    onMangaClick: (Manga) -> Unit,
    onBack: () -> Unit
) {
    val api = remember { AniListApi() }
    var luckyResult by remember { mutableStateOf<LuckyResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    val fortunes = listOf(
        "\u5927\u5409" to "\u4ECA\u65E5\u306F\u6700\u9AD8\u306E\u6F2B\u753B\u65E5\u548C\uFF01\u65B0\u3057\u3044\u4F5C\u54C1\u3068\u306E\u51FA\u4F1A\u3044\u304C\u3042\u308A\u305D\u3046\uFF01",
        "\u4E2D\u5409" to "\u304A\u6C17\u306B\u5165\u308A\u306E\u30B8\u30E3\u30F3\u30EB\u3092\u8AAD\u3080\u3068\u826F\u3044\u3053\u3068\u304C\u3042\u308B\u304B\u3082\uFF01",
        "\u5C0F\u5409" to "\u77ED\u3044\u4F5C\u54C1\u3092\u8AAD\u3080\u306E\u304C\u304A\u3059\u3059\u3081\uFF01\u610F\u5916\u306A\u767A\u898B\u304C\u3042\u308B\u304B\u3082\uFF01",
        "\u5409" to "\u53CB\u9054\u306B\u6F2B\u753B\u3092\u304A\u3059\u3059\u3081\u3057\u3066\u307F\u3066\uFF01\u826F\u3044\u53CD\u5FDC\u304C\u3082\u3089\u3048\u308B\u304B\u3082\uFF01",
        "\u672B\u5409" to "\u65B0\u3057\u3044\u30B8\u30E3\u30F3\u30EB\u306B\u6311\u6226\u3059\u308B\u65E5\uFF01\u601D\u308F\u306C\u540D\u4F5C\u306B\u51FA\u4F1A\u3048\u308B\u304B\u3082\uFF01"
    )

    fun generateLucky() {
        isLoading = true
        revealed = false
        scope.launch {
            val seed = today.dayOfYear + today.year
            val allManga = api.getRandomManga(seed, perPage = 50)
            if (allManga.isNotEmpty()) {
                val index = seed % allManga.size
                val manga = allManga[index]
                val fortuneIndex = seed % fortunes.size
                val genreIndex = seed % MangaGenre.entries.size
                val compatibility = 60 + (seed % 41)

                luckyResult = LuckyResult(
                    manga = manga,
                    fortune = fortunes[fortuneIndex].first,
                    luckyGenre = MangaGenre.entries[genreIndex].displayName,
                    compatibility = compatibility,
                    message = fortunes[fortuneIndex].second
                )
            }
            isLoading = false
            revealed = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF7B1FA2),
                        Color(0xFF4A148C),
                        Color(0xFF1A237E)
                    )
                )
            )
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("\u2190 \u623B\u308B", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Title
        Text(
            text = "\uD83D\uDD2E",
            fontSize = 60.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "\u4ECA\u65E5\u306E\u30E9\u30C3\u30AD\u30FC\u6F2B\u753B",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "${today.year}\u5E74${today.monthNumber}\u6708${today.dayOfMonth}\u65E5",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (!revealed && !isLoading) {
            // Fortune button
            Button(
                onClick = { generateLucky() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PopYellow
                )
            ) {
                Text(
                    text = "\u2728 \u904B\u52E2\u3092\u5360\u3046 \u2728",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A148C)
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                val rotation by rememberInfiniteTransition().animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing))
                )
                Text(
                    text = "\uD83D\uDD2E",
                    fontSize = 48.sp,
                    modifier = Modifier.rotate(rotation)
                )
            }
        }

        luckyResult?.let { result ->
            if (revealed) {
                // Fortune result
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Fortune
                        Text(
                            text = result.fortune,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (result.fortune) {
                                "\u5927\u5409" -> Color(0xFFFF6F00)
                                "\u4E2D\u5409" -> PopPink
                                else -> PopPurple
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = result.message,
                            fontSize = 14.sp,
                            color = PopGrayDark,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = PopGrayLight)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Lucky manga
                        Text(
                            text = "\u4ECA\u65E5\u306E\u304A\u3059\u3059\u3081\u6F2B\u753B",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PopGrayDark
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        AsyncImage(
                            model = result.manga.coverUrl,
                            contentDescription = result.manga.titleJa,
                            modifier = Modifier
                                .width(140.dp)
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = result.manga.titleJa,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PopOnSurface,
                            textAlign = TextAlign.Center
                        )
                        if (result.manga.score > 0) {
                            Text(
                                text = "\u2B50 ${result.manga.score}%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = StarYellow
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Lucky info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            LuckyInfoItem(
                                label = "\u30E9\u30C3\u30AD\u30FC\u30B8\u30E3\u30F3\u30EB",
                                value = result.luckyGenre
                            )
                            LuckyInfoItem(
                                label = "\u76F8\u6027\u5EA6",
                                value = "${result.compatibility}%"
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { onMangaClick(result.manga) },
                            colors = ButtonDefaults.buttonColors(containerColor = PopPink),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "\u8A73\u3057\u304F\u898B\u308B",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun LuckyInfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = PopGrayDark
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = PopPurple
        )
    }
}
