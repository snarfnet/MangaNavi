package com.mangaguide.manganavi

import androidx.compose.runtime.*
import com.mangaguide.manganavi.data.AniListApi
import com.mangaguide.manganavi.model.Manga
import com.mangaguide.manganavi.theme.PopTheme
import com.mangaguide.manganavi.ui.*
import kotlinx.coroutines.launch

enum class Screen {
    HOME, RANKING, GENRE, LUCKY, DETAIL, STAFF, HIDDEN_GEMS, AWARDS, READING_LIST
}

@Composable
fun App() {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var selectedManga by remember { mutableStateOf<Manga?>(null) }
    var selectedStaffId by remember { mutableStateOf<Int?>(null) }
    val api = remember { AniListApi() }
    val scope = rememberCoroutineScope()

    PopTheme {
        when (currentScreen) {
            Screen.HOME -> HomeScreen(
                onMangaClick = {
                    selectedManga = it
                    currentScreen = Screen.DETAIL
                },
                onRankingClick = { currentScreen = Screen.RANKING },
                onGenreClick = { currentScreen = Screen.GENRE },
                onLuckyClick = { currentScreen = Screen.LUCKY },
                onHiddenGemsClick = { currentScreen = Screen.HIDDEN_GEMS },
                onAwardsClick = { currentScreen = Screen.AWARDS },
                onReadingListClick = { currentScreen = Screen.READING_LIST }
            )

            Screen.RANKING -> RankingScreen(
                onMangaClick = {
                    selectedManga = it
                    currentScreen = Screen.DETAIL
                },
                onBack = { currentScreen = Screen.HOME }
            )

            Screen.GENRE -> GenreScreen(
                onMangaClick = {
                    selectedManga = it
                    currentScreen = Screen.DETAIL
                },
                onBack = { currentScreen = Screen.HOME }
            )

            Screen.LUCKY -> LuckyMangaScreen(
                onMangaClick = {
                    selectedManga = it
                    currentScreen = Screen.DETAIL
                },
                onBack = { currentScreen = Screen.HOME }
            )

            Screen.DETAIL -> selectedManga?.let { manga ->
                DetailScreen(
                    manga = manga,
                    onBack = { currentScreen = Screen.HOME },
                    onMangaClick = {
                        selectedManga = it
                        currentScreen = Screen.DETAIL
                    },
                    onStaffClick = { staffId ->
                        selectedStaffId = staffId
                        currentScreen = Screen.STAFF
                    }
                )
            }

            Screen.STAFF -> selectedStaffId?.let { staffId ->
                StaffScreen(
                    staffId = staffId,
                    onMangaClick = {
                        selectedManga = it
                        currentScreen = Screen.DETAIL
                    },
                    onBack = { currentScreen = Screen.DETAIL }
                )
            }

            Screen.HIDDEN_GEMS -> HiddenGemsScreen(
                onMangaClick = {
                    selectedManga = it
                    currentScreen = Screen.DETAIL
                },
                onBack = { currentScreen = Screen.HOME }
            )

            Screen.AWARDS -> AwardsScreen(
                onMangaClick = {
                    selectedManga = it
                    currentScreen = Screen.DETAIL
                },
                onBack = { currentScreen = Screen.HOME }
            )

            Screen.READING_LIST -> ReadingListScreen(
                onMangaClick = { mangaId ->
                    scope.launch {
                        val manga = api.getMangaById(mangaId)
                        if (manga != null) {
                            selectedManga = manga
                            currentScreen = Screen.DETAIL
                        }
                    }
                },
                onBack = { currentScreen = Screen.HOME }
            )
        }
    }
}
