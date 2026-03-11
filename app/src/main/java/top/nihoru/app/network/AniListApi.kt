package top.nihoru.app.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import top.nihoru.app.data.model.*
import java.util.concurrent.TimeUnit

object AniListApi {

    private const val URL = "https://graphql.anilist.co"
    private val JSON = "application/json".toMediaType()
    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        })
        .build()

    private val SEARCH_QUERY = """
        query (${'$'}search: String, ${'$'}type: MediaType) {
          Page(perPage: 5) {
            media(search: ${'$'}search, type: ${'$'}type, sort: [SEARCH_MATCH, POPULARITY_DESC]) {
              id idMal title { romaji english }
              type format status episodes chapters volumes popularity
              startDate { year } coverImage { large } siteUrl
            }
          }
        }
    """.trimIndent()

    private val RELATIONS_QUERY = """
        query (${'$'}idMal: Int, ${'$'}type: MediaType) {
          Media(idMal: ${'$'}idMal, type: ${'$'}type) {
            relations {
              edges {
                relationType(version: 2)
                node {
                  id idMal title { romaji english }
                  type format status episodes chapters volumes popularity
                  startDate { year } coverImage { large } siteUrl
                }
              }
            }
          }
        }
    """.trimIndent()

    suspend fun searchMedia(title: String, type: String): List<MediaItem> {
        val variables = JsonObject().apply {
            addProperty("search", title)
            addProperty("type", type.uppercase())
        }
        return try {
            val result = executeQuery(SEARCH_QUERY, variables)
            val page = result?.getAsJsonObject("Page")
            val media = page?.getAsJsonArray("media")
            media?.map { gson.fromJson(it, MediaItem::class.java) } ?: emptyList()
        } catch (e: Exception) {
            Log.e("AniListApi", "Search error: ${e.message}")
            emptyList()
        }
    }

    suspend fun getRelations(malId: Int, type: String): List<RelationEdge> {
        val variables = JsonObject().apply {
            addProperty("idMal", malId)
            addProperty("type", type.uppercase())
        }
        return try {
            val result = executeQuery(RELATIONS_QUERY, variables)
            val media = result?.getAsJsonObject("Media")
            val relations = media?.getAsJsonObject("relations")
            val edges = relations?.getAsJsonArray("edges")
            edges?.map { gson.fromJson(it, RelationEdge::class.java) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun executeQuery(query: String, variables: JsonObject, retries: Int = 0): com.google.gson.JsonObject? {
        val body = JsonObject().apply {
            addProperty("query", query)
            add("variables", variables)
        }

        val request = Request.Builder()
            .url(URL)
            .post(body.toString().toRequestBody(JSON))
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()

        return try {
            val response = client.newCall(request).execute()

            if (response.code == 429) {
                val waitTime = (response.header("Retry-After")?.toLong() ?: 60L) * 1000L
                delay(waitTime + 2000)
                return executeQuery(query, variables, retries)
            }

            val json = gson.fromJson(response.body?.string(), JsonObject::class.java)
            json?.getAsJsonObject("data")
        } catch (e: Exception) {
            if (retries < 3) {
                delay(2000)
                executeQuery(query, variables, retries + 1)
            } else null
        }
    }

    fun findBestMatch(candidates: List<MediaItem>, query: String, mode: String): MediaItem? {
        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return candidates[0]

        val scored = candidates.map { item ->
            var score = 0
            val titleEng = (item.title.english ?: "").lowercase()
            val titleRom = (item.title.romaji ?: "").lowercase()
            val q = query.lowercase()

            if (titleEng == q || titleRom == q) score += 50
            else if (titleEng.contains(q) || titleRom.contains(q)) score += 20

            if (mode == "anime") {
                when (item.format) {
                    "TV" -> score += 30
                    "MOVIE" -> score += 25
                    "OVA" -> score += 15
                    "SPECIAL", "MUSIC" -> score -= 20
                }
            } else {
                when (item.format) {
                    "MANGA" -> score += 30
                    "ONE_SHOT" -> score -= 30
                }
            }

            val pop = item.popularity ?: 0
            if (pop > 100000) score += 10
            if (pop > 5000) score += 5

            Pair(item, score)
        }

        return scored.maxByOrNull { it.second }?.first
    }

    fun formatEntry(media: MediaItem, mode: String, defaultStatus: String): top.nihoru.app.data.model.AnimeEntry {
        val malStatusMap = mapOf(
            "FINISHED" to "Finished Airing",
            "RELEASING" to "Currently Airing",
            "NOT_YET_RELEASED" to "Not yet aired",
            "CANCELLED" to "Finished Airing",
            "HIATUS" to "Currently Airing"
        )

        var realType = "TV"
        if (media.type == "MANGA") {
            realType = "Manga"
        } else {
            val fmt = media.format ?: "TV"
            realType = when (fmt) {
                "MOVIE", "SPECIAL", "OVA", "ONA", "MUSIC" ->
                    fmt[0] + fmt.substring(1).lowercase()
                "TV_SHORT" -> "TV"
                else -> "TV"
            }
        }

        return top.nihoru.app.data.model.AnimeEntry(
            malId = media.idMal ?: 0,
            title = media.title.romaji ?: media.title.english ?: "",
            titleEnglish = media.title.english ?: media.title.romaji,
            type = realType,
            format = media.format,
            status = malStatusMap[media.status] ?: "Finished Airing",
            episodes = media.episodes,
            chapters = media.chapters,
            volumes = media.volumes,
            year = media.startDate?.year,
            imageUrl = media.coverImage?.large,
            siteUrl = media.siteUrl,
            userStatus = defaultStatus,
            userScore = 0,
            mode = mode
        )
    }
}
