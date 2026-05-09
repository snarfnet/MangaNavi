package com.mangaguide.manganavi.model

import kotlinx.serialization.Serializable

@Serializable
data class AniListResponse(
    val data: AniListData? = null
)

@Serializable
data class AniListData(
    val Page: PageData? = null
)

@Serializable
data class PageData(
    val media: List<AniListManga> = emptyList(),
    val pageInfo: PageInfo? = null
)

@Serializable
data class PageInfo(
    val total: Int = 0,
    val currentPage: Int = 1,
    val hasNextPage: Boolean = false
)

@Serializable
data class AniListManga(
    val id: Int = 0,
    val title: MangaTitle? = null,
    val coverImage: CoverImage? = null,
    val bannerImage: String? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val averageScore: Int? = null,
    val popularity: Int? = null,
    val trending: Int? = null,
    val chapters: Int? = null,
    val volumes: Int? = null,
    val status: String? = null,
    val startDate: FuzzyDate? = null,
    val meanScore: Int? = null,
    val format: String? = null,
    val isAdult: Boolean = false,
    val externalLinks: List<ExternalLink> = emptyList()
)

@Serializable
data class ExternalLink(
    val url: String? = null,
    val site: String? = null,
    val type: String? = null
)

@Serializable
data class MangaTitle(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null
)

@Serializable
data class CoverImage(
    val large: String? = null,
    val medium: String? = null,
    val extraLarge: String? = null
)

@Serializable
data class FuzzyDate(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null
)

data class ReadingLink(
    val siteName: String,
    val url: String,
    val icon: String
)

data class Manga(
    val id: Int,
    val titleJa: String,
    val titleEn: String,
    val coverUrl: String,
    val description: String,
    val genres: List<String>,
    val score: Int,
    val popularity: Int,
    val chapters: Int?,
    val volumes: Int?,
    val status: String,
    val year: Int?,
    val readingLinks: List<ReadingLink> = emptyList()
) {
    val weightedScore: Double get() {
        val hasJapaneseTitle = titleJa.isNotBlank() && titleJa != titleEn && titleJa != "不明"
        val scoreBase = score.toDouble()
        val popularityBoost = (popularity.toDouble() / 120000.0).coerceAtMost(8.0)
        val japaneseTitleBoost = if (hasJapaneseTitle) 3.0 else 0.0
        return scoreBase + popularityBoost + japaneseTitleBoost
    }
}

private val readingSiteIcons = mapOf(
    "少年ジャンプ" to "📕",
    "ジャンプ" to "📕",
    "マガポケ" to "📘",
    "マンガワン" to "📙",
    "コミックシーモア" to "📚",
    "pixiv" to "🎨",
    "BookWalker" to "📖",
    "Amazon" to "🛒",
    "Kindle" to "🛒",
    "ebookjapan" to "📱",
    "めちゃコミック" to "📱",
    "LINEマンガ" to "💬",
    "ニコニコ" to "★",
    "ComicWalker" to "📖",
    "ガンガンONLINE" to "📕"
)

private fun getIconForSite(siteName: String): String {
    for ((key, icon) in readingSiteIcons) {
        if (siteName.contains(key, ignoreCase = true)) return icon
    }
    return "↗"
}

fun AniListManga.toManga(): Manga = Manga(
    id = id,
    titleJa = title?.native ?: title?.romaji ?: "不明",
    titleEn = title?.english ?: title?.romaji ?: "Unknown",
    coverUrl = coverImage?.extraLarge ?: coverImage?.large ?: coverImage?.medium ?: "",
    description = description?.replace(Regex("<[^>]*>"), "") ?: "",
    genres = genres,
    score = averageScore ?: meanScore ?: 0,
    popularity = popularity ?: 0,
    chapters = chapters,
    volumes = volumes,
    status = when (status) {
        "FINISHED" -> "完結"
        "RELEASING" -> "連載中"
        "NOT_YET_RELEASED" -> "未発売"
        "CANCELLED" -> "中止"
        "HIATUS" -> "休載中"
        else -> status ?: "不明"
    },
    year = startDate?.year,
    readingLinks = externalLinks
        .filter { it.url != null && it.site != null }
        .mapNotNull { link ->
            val url = link.url ?: return@mapNotNull null
            val site = link.site ?: return@mapNotNull null
            val isRelevantSite = jpSiteKeywords.any { site.contains(it, ignoreCase = true) }
                || url.contains(".jp", ignoreCase = true)
                || url.contains("amazon", ignoreCase = true)
            if (!isRelevantSite) return@mapNotNull null
            ReadingLink(
                siteName = site,
                url = url,
                icon = getIconForSite(site)
            )
        }
        .distinctBy { it.siteName }
)

private val jpSiteKeywords = listOf(
    "ジャンプ", "マガポケ", "マンガワン", "コミックシーモア", "ebookjapan",
    "めちゃコミック", "LINE", "ニコニコ", "ガンガン", "BookWalker", "pixiv",
    "ComicWalker", "Shonen Jump", "Sunday", "Magazine", "piccoma",
    "Renta", "DLsite", "DMM", "Booklive", "honto", "Rakuten",
    "BOOK WALKER", "Amazon", "Kindle", "Pixiv", "Nico Nico",
    "Comic Days", "Manga Plus", "Manga One", "AlphaPolis",
    "Comico", "GANMA", "Palcy", "マンガUP", "マンガPark"
)

data class StaffInfo(
    val id: Int,
    val nameNative: String,
    val nameEn: String
)

data class StaffEdge(
    val staffId: Int,
    val nameNative: String,
    val nameEn: String,
    val role: String
)

@Serializable
data class AniListStaffName(
    val full: String? = null,
    val native: String? = null
)

@Serializable
data class AniListStaffNode(
    val id: Int = 0,
    val name: AniListStaffName? = null
)

@Serializable
data class AniListStaffEdge(
    val node: AniListStaffNode? = null,
    val role: String? = null
)

@Serializable
data class AniListStaffConnection(
    val edges: List<AniListStaffEdge> = emptyList()
)

@Serializable
data class AniListStaffMediaNodes(
    val nodes: List<AniListManga> = emptyList()
)

@Serializable
data class AniListStaffDetail(
    val id: Int = 0,
    val name: AniListStaffName? = null,
    val staffMedia: AniListStaffMediaNodes? = null
)

@Serializable
data class AniListRecommendationNode(
    val mediaRecommendation: AniListManga? = null,
    val rating: Int? = null
)

@Serializable
data class AniListRecommendationConnection(
    val nodes: List<AniListRecommendationNode> = emptyList()
)

@Serializable
data class AniListMediaWithRecommendations(
    val id: Int = 0,
    val recommendations: AniListRecommendationConnection? = null
)

@Serializable
data class AniListMediaWithStaff(
    val id: Int = 0,
    val staff: AniListStaffConnection? = null
)

@Serializable
data class AniListDataExtended(
    val Page: PageData? = null,
    val Media: AniListMediaWithRecommendations? = null,
    val Staff: AniListStaffDetail? = null
)

@Serializable
data class AniListResponseExtended(
    val data: AniListDataExtended? = null
)

@Serializable
data class AniListDataWithStaff(
    val Media: AniListMediaWithStaff? = null
)

@Serializable
data class AniListResponseWithStaff(
    val data: AniListDataWithStaff? = null
)

enum class MangaGenre(val displayName: String, val aniListName: String) {
    ACTION("アクション", "Action"),
    ADVENTURE("冒険", "Adventure"),
    COMEDY("コメディ", "Comedy"),
    DRAMA("ドラマ", "Drama"),
    FANTASY("ファンタジー", "Fantasy"),
    HORROR("ホラー", "Horror"),
    MYSTERY("ミステリー", "Mystery"),
    ROMANCE("恋愛", "Romance"),
    SCI_FI("SF", "Sci-Fi"),
    SLICE_OF_LIFE("日常", "Slice of Life"),
    SPORTS("スポーツ", "Sports"),
    THRILLER("サスペンス", "Thriller"),
    SUPERNATURAL("超自然", "Supernatural"),
    PSYCHOLOGICAL("心理", "Psychological")
}
