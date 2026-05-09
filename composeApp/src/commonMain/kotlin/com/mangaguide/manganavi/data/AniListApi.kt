package com.mangaguide.manganavi.data

import com.mangaguide.manganavi.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class AniListApi {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private val endpoint = "https://graphql.anilist.co"

    private val mangaFields = """
        id
        title { romaji english native }
        coverImage { large extraLarge }
        description(asHtml: false)
        genres
        averageScore
        popularity
        trending
        chapters
        volumes
        status
        startDate { year month day }
        meanScore
        format
        isAdult
        externalLinks { url site type }
    """.trimIndent()

    // 高評価（独自スコアでソート）
    suspend fun getTopManga(page: Int = 1, perPage: Int = 20): List<Manga> {
        val query = """
            query {
                Page(page: $page, perPage: $perPage) {
                    media(type: MANGA, sort: SCORE_DESC, isAdult: false, countryOfOrigin: "JP", minimumTagRank: 10) {
                        $mangaFields
                    }
                }
            }
        """.trimIndent()
        return executeQuery(query).sortedByDescending { it.weightedScore }
    }

    // 人気順（独自スコア）
    suspend fun getPopularManga(page: Int = 1, perPage: Int = 20): List<Manga> {
        val query = """
            query {
                Page(page: $page, perPage: $perPage) {
                    media(type: MANGA, sort: POPULARITY_DESC, isAdult: false, countryOfOrigin: "JP") {
                        $mangaFields
                    }
                }
            }
        """.trimIndent()
        return executeQuery(query).sortedByDescending { it.weightedScore }
    }

    // トレンド
    suspend fun getTrendingManga(page: Int = 1, perPage: Int = 20): List<Manga> {
        val query = """
            query {
                Page(page: $page, perPage: $perPage) {
                    media(type: MANGA, sort: TRENDING_DESC, isAdult: false, countryOfOrigin: "JP") {
                        $mangaFields
                    }
                }
            }
        """.trimIndent()
        return executeQuery(query)
    }

    // 殿堂入り名作（高スコア＋高人気＋完結or長期連載）
    suspend fun getHallOfFame(perPage: Int = 15): List<Manga> {
        val query = """
            query {
                Page(page: 1, perPage: $perPage) {
                    media(type: MANGA, sort: FAVOURITES_DESC, isAdult: false, countryOfOrigin: "JP") {
                        $mangaFields
                    }
                }
            }
        """.trimIndent()
        return executeQuery(query)
    }

    // 完結済みおすすめ
    suspend fun getCompletedGems(page: Int = 1, perPage: Int = 20): List<Manga> {
        val query = """
            query {
                Page(page: $page, perPage: $perPage) {
                    media(type: MANGA, sort: SCORE_DESC, status: FINISHED, isAdult: false, countryOfOrigin: "JP") {
                        $mangaFields
                    }
                }
            }
        """.trimIndent()
        return executeQuery(query).sortedByDescending { it.weightedScore }
    }

    // 新作注目（直近2年）
    suspend fun getNewNotable(perPage: Int = 20): List<Manga> {
        val query = """
            query {
                Page(page: 1, perPage: $perPage) {
                    media(type: MANGA, sort: POPULARITY_DESC, isAdult: false, countryOfOrigin: "JP", startDate_greater: 20240101) {
                        $mangaFields
                    }
                }
            }
        """.trimIndent()
        return executeQuery(query).sortedByDescending { it.weightedScore }
    }

    // ジャンル別
    suspend fun getMangaByGenre(genre: String, page: Int = 1, perPage: Int = 20): List<Manga> {
        val query = """
            query {
                Page(page: $page, perPage: $perPage) {
                    media(type: MANGA, sort: SCORE_DESC, genre: "$genre", isAdult: false, countryOfOrigin: "JP") {
                        $mangaFields
                    }
                }
            }
        """.trimIndent()
        return executeQuery(query).sortedByDescending { it.weightedScore }
    }

    // ランダム（ラッキー漫画用）
    suspend fun getRandomManga(seed: Int, perPage: Int = 50): List<Manga> {
        val page = (seed % 10) + 1
        val query = """
            query {
                Page(page: $page, perPage: $perPage) {
                    media(type: MANGA, sort: POPULARITY_DESC, isAdult: false, countryOfOrigin: "JP") {
                        $mangaFields
                    }
                }
            }
        """.trimIndent()
        return executeQuery(query)
    }

    // 類似作品レコメンド
    suspend fun getRecommendations(mangaId: Int): List<Manga> {
        val query = """
            query {
                Media(id: $mangaId, type: MANGA) {
                    recommendations(sort: RATING_DESC, perPage: 10) {
                        nodes {
                            mediaRecommendation {
                                $mangaFields
                            }
                            rating
                        }
                    }
                }
            }
        """.trimIndent()
        return try {
            val response = client.post(endpoint) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("query" to query))
            }
            val result: AniListResponseExtended = response.body()
            result.data?.Media?.recommendations?.nodes
                ?.mapNotNull { it.mediaRecommendation }
                ?.filter { !it.isAdult }
                ?.map { it.toManga() }
                ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 隠れた名作（高スコア＋低人気）
    suspend fun getHiddenGems(page: Int = 1, perPage: Int = 20): List<Manga> {
        val query = """
            query {
                Page(page: $page, perPage: $perPage) {
                    media(type: MANGA, sort: SCORE_DESC, isAdult: false, countryOfOrigin: "JP", popularity_lesser: 30000, averageScore_greater: 75) {
                        $mangaFields
                    }
                }
            }
        """.trimIndent()
        return executeQuery(query).sortedByDescending { it.weightedScore }
    }

    // 作者・スタッフの全作品
    suspend fun getStaffWorks(staffId: Int): Pair<StaffInfo, List<Manga>> {
        val query = """
            query {
                Staff(id: $staffId) {
                    name { full native }
                    staffMedia(type: MANGA, sort: POPULARITY_DESC, perPage: 20) {
                        nodes {
                            $mangaFields
                        }
                    }
                }
            }
        """.trimIndent()
        return try {
            val response = client.post(endpoint) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("query" to query))
            }
            val result: AniListResponseExtended = response.body()
            val staff = result.data?.Staff
            val staffInfo = StaffInfo(
                id = staff?.id ?: staffId,
                nameNative = staff?.name?.native ?: staff?.name?.full ?: "",
                nameEn = staff?.name?.full ?: ""
            )
            val manga = staff?.staffMedia?.nodes
                ?.filter { !it.isAdult }
                ?.map { it.toManga() }
                ?: emptyList()
            Pair(staffInfo, manga)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(StaffInfo(id = staffId, nameNative = "", nameEn = ""), emptyList())
        }
    }

    // 漫画のスタッフ情報取得（作者ページへのリンク用）
    suspend fun getMangaWithStaff(mangaId: Int): List<StaffEdge> {
        val query = """
            query {
                Media(id: $mangaId, type: MANGA) {
                    staff(sort: RELEVANCE) {
                        edges {
                            node { id name { full native } }
                            role
                        }
                    }
                }
            }
        """.trimIndent()
        return try {
            val response = client.post(endpoint) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("query" to query))
            }
            val result: AniListResponseWithStaff = response.body()
            result.data?.Media?.staff?.edges
                ?.mapNotNull { edge ->
                    val node = edge.node ?: return@mapNotNull null
                    StaffEdge(
                        staffId = node.id,
                        nameNative = node.name?.native ?: node.name?.full ?: "",
                        nameEn = node.name?.full ?: "",
                        role = edge.role ?: ""
                    )
                }
                ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getMangaById(id: Int): Manga? {
        val query = """
            query {
                Page(page: 1, perPage: 1) {
                    media(id: $id, type: MANGA) {
                        $mangaFields
                    }
                }
            }
        """.trimIndent()
        return executeQuery(query).firstOrNull()
    }

    private suspend fun executeQuery(query: String): List<Manga> {
        return try {
            val response = client.post(endpoint) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("query" to query))
            }
            val result: AniListResponse = response.body()
            result.data?.Page?.media
                ?.filter { !it.isAdult }
                ?.map { it.toManga() }
                ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
