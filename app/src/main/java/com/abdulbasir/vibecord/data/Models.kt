package com.abdulbasir.vibecord.data

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: Uri,
    val dateAdded: Long,
)

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val songs: List<Song>,
)

data class Artist(
    val name: String,
    val songs: List<Song>,
)

fun List<Song>.toAlbums(): List<Album> =
    groupBy { it.albumId }
        .map { (id, list) ->
            Album(
                id = id,
                name = list.first().album,
                artist = list.first().artist,
                songs = list.sortedBy { it.title.lowercase() },
            )
        }
        .sortedBy { it.name.lowercase() }

fun List<Song>.toArtists(): List<Artist> =
    groupBy { it.artist }
        .map { (name, list) -> Artist(name, list.sortedBy { it.title.lowercase() }) }
        .sortedBy { it.name.lowercase() }
