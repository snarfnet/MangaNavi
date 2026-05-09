package com.mangaguide.manganavi.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class ReadingStatus(val displayName: String, val emoji: String) {
    WANT_TO_READ("読みたい", "📌"),
    READING("読書中", "📖"),
    COMPLETED("読了", "✓")
}

@Serializable
data class MangaReadingEntry(
    val mangaId: Int,
    val status: String,
    val titleJa: String,
    val coverUrl: String,
    val score: Int = 0,
    val addedAt: Long = 0
)

object ReadingStatusStore {
    private val json = Json { ignoreUnknownKeys = true }
    private val _entries = MutableStateFlow<Map<Int, MangaReadingEntry>>(emptyMap())
    val entries: StateFlow<Map<Int, MangaReadingEntry>> = _entries

    fun setStatus(
        mangaId: Int,
        status: ReadingStatus,
        titleJa: String,
        coverUrl: String,
        score: Int
    ) {
        val current = _entries.value.toMutableMap()
        current[mangaId] = MangaReadingEntry(
            mangaId = mangaId,
            status = status.name,
            titleJa = titleJa,
            coverUrl = coverUrl,
            score = score,
            addedAt = currentTimeMillis()
        )
        _entries.value = current
    }

    fun removeStatus(mangaId: Int) {
        val current = _entries.value.toMutableMap()
        current.remove(mangaId)
        _entries.value = current
    }

    fun getStatus(mangaId: Int): ReadingStatus? {
        return _entries.value[mangaId]?.let {
            runCatching { ReadingStatus.valueOf(it.status) }.getOrNull()
        }
    }

    fun getByStatus(status: ReadingStatus): List<MangaReadingEntry> {
        return _entries.value.values
            .filter { it.status == status.name }
            .sortedByDescending { it.addedAt }
    }
}
