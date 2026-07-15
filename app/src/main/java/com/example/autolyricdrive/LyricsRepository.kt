package com.example.autolyricdrive

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import android.util.Log

@Serializable
data class LrcResponse(
    val trackName: String? = null,
    val artistName: String? = null,
    val syncedLyrics: String? = null,
    val plainLyrics: String? = null
)

class LyricsRepository {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun fetchLyrics(trackName: String, artistName: String): String? {
        return try {
            Log.d("LyricsRepository", "Searching lyrics for $trackName - $artistName")
            val results: List<LrcResponse> = client.get("https://lrclib.net/api/search") {
                parameter("track_name", trackName)
                parameter("artist_name", artistName)
            }.body()

            // 最初に見つかった syncedLyrics を返す
            results.firstOrNull { !it.syncedLyrics.isNullOrBlank() }?.syncedLyrics
        } catch (e: Exception) {
            Log.e("LyricsRepository", "Error fetching lyrics", e)
            null
        }
    }
}
