package top.nihoru.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anime_list")
data class AnimeEntry(
    @PrimaryKey val malId: Int,
    val title: String,
    val titleEnglish: String?,
    val type: String?,
    val format: String?,
    val status: String?,
    val episodes: Int?,
    val chapters: Int?,
    val volumes: Int?,
    val year: Int?,
    val imageUrl: String?,
    val siteUrl: String?,
    val userStatus: String = "Plan to Watch",
    val userScore: Int = 0,
    val originalQuery: String = "",
    val mode: String = "anime" // "anime" or "manga"
)
