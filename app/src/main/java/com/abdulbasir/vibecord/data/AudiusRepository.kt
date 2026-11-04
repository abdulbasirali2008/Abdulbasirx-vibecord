package com.abdulbasir.vibecord.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A song that streams from the internet. */
data class OnlineTrack(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val durationMs: Long,
    val streamUrl: String,
)

sealed interface OnlineResult {
    data object Loading : OnlineResult
    data object Error : OnlineResult
    data class Success(val tracks: List<OnlineTrack>) : OnlineResult
}

/**
 * Online music from Audius (https://audius.co), an open streaming service with
 * songs from independent artists. Its API is free and needs no account.
 */
object AudiusRepository {

    private const val HOST = "https://api.audius.co"
    private const val APP_NAME = "Vibecord"

    suspend fun trending(limit: Int = 30): List<OnlineTrack> = withContext(Dispatchers.IO) {
        parse(get("$HOST/v1/tracks/trending?limit=$limit&app_name=$APP_NAME"))
    }

    suspend fun search(query: String, limit: Int = 30): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query, "UTF-8")
        parse(get("$HOST/v1/tracks/search?query=$q&limit=$limit&app_name=$APP_NAME"))
    }

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10000
            connection.readTimeout = 12000
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("Audius returned $code")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.str(name: String): String =
        if (isNull(name)) "" else optString(name, "")

    private fun parse(body: String): List<OnlineTrack> {
        val data = JSONObject(body).optJSONArray("data") ?: return emptyList()
        val tracks = ArrayList<OnlineTrack>()
        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i) ?: continue
            val id = item.str("id")
            if (id.isEmpty()) continue
            if (item.has("is_streamable") && !item.isNull("is_streamable") && !item.optBoolean("is_streamable", true)) continue

            val artist = item.optJSONObject("user")?.str("name").orEmpty()
            tracks += OnlineTrack(
                id = id,
                title = item.str("title").ifBlank { "Unknown title" },
                artist = artist.ifBlank { "Unknown artist" },
                artworkUrl = artwork(item),
                durationMs = item.optLong("duration", 0L) * 1000L,
                streamUrl = "$HOST/v1/tracks/$id/stream?app_name=$APP_NAME",
            )
        }
        return tracks
    }

    /** Artwork comes as a set of sizes; take a medium one, or whatever URL is there. */
    private fun artwork(item: JSONObject): String? {
        val art = item.opt("artwork")
        if (art is String) return art.ifBlank { null }
        if (art is JSONObject) {
            for (size in listOf("480x480", "1000x1000", "150x150")) {
                val url = art.str(size)
                if (url.isNotBlank()) return url
            }
            val keys = art.keys()
            while (keys.hasNext()) {
                val url = art.str(keys.next())
                if (url.startsWith("http")) return url
            }
        }
        return null
    }
}
