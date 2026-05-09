package com.mangaguide.manganavi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mangaguide.manganavi.data.MangaReadingEntry
import com.mangaguide.manganavi.data.ReadingStatus
import com.mangaguide.manganavi.data.ReadingStatusStore
import com.mangaguide.manganavi.model.Manga
import com.mangaguide.manganavi.theme.*

@Composable
fun ReadingListScreen(
    onMangaClick: (Int) -> Unit,
    onBack: () -> Unit
) {
    val allEntries by ReadingStatusStore.entries.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = ReadingStatus.entries.toList()

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
                            listOf(PopPink, PopOrange)
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
                        text = "\uD83D\uDCDA \u30DE\u30A4\u30EA\u30B9\u30C8",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Text(
                        text = "\u5408\u8A08 ${allEntries.size} \u4F5C\u54C1",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Tab row
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = PopSurface,
            contentColor = PopPink
        ) {
            tabs.forEachIndexed { index, status ->
                val count = allEntries.values.count { it.status == status.name }
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = "${status.emoji} ${status.displayName} ($count)",
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    },
                    selectedContentColor = PopPink,
                    unselectedContentColor = PopGrayDark
                )
            }
        }

        val currentStatus = tabs[selectedTabIndex]
        val currentEntries = remember(allEntries, selectedTabIndex) {
            ReadingStatusStore.getByStatus(currentStatus)
        }

        if (currentEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = currentStatus.emoji, fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when (currentStatus) {
                            ReadingStatus.WANT_TO_READ -> "\u8AAD\u307F\u305F\u3044\u4F5C\u54C1\u304C\u307E\u3060\u3042\u308A\u307E\u305B\u3093\n\u8A73\u7D30\u753B\u9762\u304B\u3089\u8FFD\u52A0\u3067\u304D\u307E\u3059"
                            ReadingStatus.READING -> "\u8AAD\u66F8\u4E2D\u306E\u4F5C\u54C1\u304C\u307E\u3060\u3042\u308A\u307E\u305B\u3093\n\u8A73\u7D30\u753B\u9762\u304B\u3089\u8FFD\u52A0\u3067\u304D\u307E\u3059"
                            ReadingStatus.COMPLETED -> "\u8AAD\u4E86\u306E\u4F5C\u54C1\u304C\u307E\u3060\u3042\u308A\u307E\u305B\u3093\n\u8A73\u7D30\u753B\u9762\u304B\u3089\u8FFD\u52A0\u3067\u304D\u307E\u3059"
                        },
                        fontSize = 15.sp,
                        color = PopGrayDark,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(currentEntries, key = { it.mangaId }) { entry ->
                    ReadingListEntryCard(
                        entry = entry,
                        currentStatus = currentStatus,
                        onCardClick = { onMangaClick(entry.mangaId) },
                        onStatusChange = { newStatus ->
                            ReadingStatusStore.setStatus(
                                mangaId = entry.mangaId,
                                status = newStatus,
                                titleJa = entry.titleJa,
                                coverUrl = entry.coverUrl,
                                score = entry.score
                            )
                        },
                        onDelete = { ReadingStatusStore.removeStatus(entry.mangaId) }
                    )
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun ReadingListEntryCard(
    entry: MangaReadingEntry,
    currentStatus: ReadingStatus,
    onCardClick: () -> Unit,
    onStatusChange: (ReadingStatus) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { showDeleteConfirm = true },
                    onTap = { onCardClick() }
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PopSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover
            AsyncImage(
                model = entry.coverUrl,
                contentDescription = entry.titleJa,
                modifier = Modifier
                    .width(56.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.titleJa,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = PopOnSurface
                )
                if (entry.score > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "\u2B50", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${entry.score}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StarYellow
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Status change buttons (show the other two statuses)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReadingStatus.entries.filter { it != currentStatus }.forEach { status ->
                        Surface(
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(onTap = { onStatusChange(status) })
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = PopPink.copy(alpha = 0.10f)
                        ) {
                            Text(
                                text = "${status.emoji} ${status.displayName}",
                                fontSize = 11.sp,
                                color = PopPink,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Delete button
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.size(36.dp)
            ) {
                Text(text = "\uD83D\uDDD1\uFE0F", fontSize = 16.sp)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "\u524A\u9664\u3057\u307E\u3059\u304B\uFF1F",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "「${entry.titleJa}」\u3092\u30EA\u30B9\u30C8\u304B\u3089\u524A\u9664\u3057\u307E\u3059\u3002",
                    fontSize = 14.sp,
                    color = PopGrayDark
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text(
                        text = "\u524A\u9664",
                        color = PopPink,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(text = "\u30AD\u30E3\u30F3\u30BB\u30EB", color = PopGrayDark)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = PopSurface
        )
    }
}
