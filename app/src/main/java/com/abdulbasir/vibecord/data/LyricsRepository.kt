package com.abdulbasir.vibecord.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

data class LyricLine(val timeMs: Long, val text: String)

/** [synced] lyrics carry a time for every line; plain lyrics have timeMs = -1. */
data class Lyrics(val lines: List<LyricLine>, val synced: Boolean)

sealed interface LyricsResult {
    data object Loading : LyricsResult
    data object NotFound : LyricsResult
    data object Failed : LyricsResult
    data class Found(val lyrics: Lyrics) : LyricsResult
}

/**
 * Finds lyrics for a song on LRCLIB (https://lrclib.net), a free community
 * lyrics service. Nothing is bundled in the app: lyrics are fetched when you
 * ask for them and kept in a small cache on the phone.
 */
object LyricsRepository {

    private const val USER_AGENT =
        "Vibecord/0.3.0 (https://github.com/abdulbasirali2008/abdulbasirx-vibecord)"
    private const val BASE = "https://lrclib.net/api"

    private val timeTag = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]""")

    suspend fun load(
        context: Context,
        key: String,
        title: String,
        artist: String,
        album: String,
        durationMs: Long,
    ): LyricsResult = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "lyrics").apply { mkdirs() }
        val file = File(dir, "$key.txt")
        if (file.exists()) {
            return@withContext parseStored(file.readText())
        }

        try {
            val found = fetch(title, artist, album, durationMs)
            if (found == null) {
                file.writeText("N\n")
                LyricsResult.NotFound
            } else {
                file.writeText("${found.first}\n${found.second}")
                parseStored("${found.first}\n${found.second}")
            }
        } catch (e: Exception) {
            // Offline or the service is down: don't remember "no lyrics", try again next time.
            LyricsResult.Failed
        }
    }

    private fun parseStored(text: String): LyricsResult {
        val kind = text.substringBefore('\n')
        val rest = text.substringAfter('\n', "")
        return when (kind) {
            "S" -> {
                val lines = parseLrc(rest)
                if (lines.isEmpty()) LyricsResult.NotFound else LyricsResult.Found(Lyrics(lines, synced = true))
            }
            "P" -> {
                val lines = rest.lines().map { LyricLine(-1L, it.trim()) }
                if (lines.all { it.text.isEmpty() }) LyricsResult.NotFound else LyricsResult.Found(Lyrics(lines, synced = false))
            }
            else -> LyricsResult.NotFound
        }
    }

    /** Returns ("S" or "P") to the lyrics text, null if the service has none, or throws if it can't be reached. */
    private fun fetch(title: String, artist: String, album: String, durationMs: Long): Pair<String, String>? {
        val seconds = durationMs / 1000
        val exact = "$BASE/get?track_name=${enc(title)}&artist_name=${enc(artist)}" +
            "&album_name=${enc(album)}&duration=$seconds"
        val (code, body) = httpGet(exact)
        if (code == 200) {
            pick(JSONObject(body))?.let { return it }
        } else if (code != 404) {
            throw IllegalStateException("LRCLIB returned $code")
        }

        // The exact match missed (often the album name differs): search by title and artist.
        val search = "$BASE/search?track_name=${enc(title)}&artist_name=${enc(artist)}"
        val (searchCode, searchBody) = httpGet(search)
        if (searchCode != 200) {
            if (searchCode == 404) return null
            throw IllegalStateException("LRCLIB returned $searchCode")
        }
        val results = JSONArray(searchBody)
        var best: Pair<String, String>? = null
        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val duration = item.optDouble("duration", 0.0)
            if (seconds > 0 && abs(duration - seconds) > 4.0) continue
            val candidate = pick(item) ?: continue
            if (candidate.first == "S") return candidate
            if (best == null) best = candidate
        }
        return best
    }

    private fun pick(item: JSONObject): Pair<String, String>? {
        val synced = item.str("syncedLyrics")
        if (synced.isNotBlank()) return "S" to synced
        val plain = item.str("plainLyrics")
        if (plain.isNotBlank()) return "P" to plain
        return null
    }

    private fun JSONObject.str(name: String): String =
        if (isNull(name)) "" else optString(name, "")

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun httpGet(url: String): Pair<Int, String> {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8000
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            return code to body
        } finally {
            connection.disconnect()
        }
    }

    /** Turns "[01:23.45] some words" lines into timed lines, in time order. */
    private fun parseLrc(text: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        for (raw in text.lines()) {
            val tags = timeTag.findAll(raw).toList()
            if (tags.isEmpty()) continue
            val words = raw.replace(timeTag, "").trim()
            for (tag in tags) {
                val minutes = tag.groupValues[1].toLong()
                val seconds = tag.groupValues[2].toLong()
                val fraction = tag.groupValues[3]
                val fractionMs = when (fraction.length) {
                    0 -> 0L
                    1 -> fraction.toLong() * 100
                    2 -> fraction.toLong() * 10
                    else -> fraction.take(3).toLong()
                }
                lines += LyricLine(minutes * 60_000 + seconds * 1000 + fractionMs, words)
            }
        }
        return lines.sortedBy { it.timeMs }
    }
}
