package com.mangaguide.manganavi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.mangaguide.manganavi.theme.*
import com.mangaguide.manganavi.ui.components.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import manganavi.composeapp.generated.resources.Res

@Serializable
data class AwardEntry(
    val year: Int,
    val category: String,
    val title: String,
    val aniListId: Int
)

@Serializable
data class AwardGroup(
    val awardName: String,
    val entries: List<AwardEntry>
)

@OptIn(ExperimentalResourceApi::class)
@Composable
fun AwardsScreen(
    onMangaClick: (Manga) -> Unit,
    onBack: () -> Unit
) {
    val api = remember { AniListApi() }
    val json = remember { Json { ignoreUnknownKeys = true } }
    var awardGroups by remember { mutableStateOf<List<AwardGroup>>(emptyList()) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var loadingMangaId by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val bytes = Res.readBytes("files/awards.json")
            val text = bytes.decodeToString()
            awardGroups = json.decodeFromString<List<AwardGroup>>(text)
        } catch (_: Exception) {}
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
                            listOf(Color(0xFFFFB300), Color(0xFFFF8A65))
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
                        text = "\uD83C\uDFC6 \u6F2B\u753B\u8CDE\u53D7\u8CDE\u4F5C",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Text(
                        text = "\u6A29\u5A01\u3042\u308B\u7523\u696D\u8CDE\u306E\u53D7\u8CDE\u4F5C\u54C1\u4E00\u89A7",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (awardGroups.isEmpty()) {
            LoadingIndicator()
        } else {
            // Tab row for award names
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = PopSurface,
                contentColor = StarYellow,
                edgePadding = 8.dp
            ) {
                awardGroups.forEachIndexed { index, group ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = group.awardName,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        },
                        selectedContentColor = StarYellow,
                        unselectedContentColor = PopGrayDark
                    )
                }
            }

            val currentGroup = awardGroups[selectedTabIndex]
            // Sort entries newest first
            val sortedEntries = remember(selectedTabIndex) {
                currentGroup.entries.sortedByDescending { it.year }
            }

            LazyColumn {
                // Group by year
                val byYear = sortedEntries.groupBy { it.year }
                byYear.forEach { (year, yearEntries) ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StarYellow.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${year}\u5E74",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StarYellow,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = PopGrayMedium.copy(alpha = 0.5f)
                            )
                        }
                    }

                    items(yearEntries) { entry ->
                        AwardEntryCard(
                            entry = entry,
                            isLoading = loadingMangaId == entry.aniListId,
                            onClick = {
                                if (loadingMangaId == null) {
                                    loadingMangaId = entry.aniListId
                                    scope.launch {
                                        try {
                                            val found = api.getMangaById(entry.aniListId)
                                            if (found != null) {
                                                onMangaClick(found)
                                            }
                                        } catch (_: Exception) {}
                                        loadingMangaId = null
                                    }
                                }
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun AwardEntryCard(
    entry: AwardEntry,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(enabled = !isLoading, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PopSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Trophy icon
            Text(text = "\uD83C\uDFC6", fontSize = 22.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = PopOnSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = entry.category,
                    fontSize = 12.sp,
                    color = PopGrayDark
                )
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = StarYellow,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = ">",
                    fontSize = 16.sp,
                    color = PopGrayMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
