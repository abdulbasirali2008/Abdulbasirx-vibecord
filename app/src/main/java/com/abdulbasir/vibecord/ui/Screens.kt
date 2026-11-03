package com.abdulbasir.vibecord.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abdulbasir.vibecord.data.Album
import com.abdulbasir.vibecord.data.Artist
import com.abdulbasir.vibecord.data.Song
import com.abdulbasir.vibecord.data.toAlbums
import com.abdulbasir.vibecord.data.toArtists

// ---------------------------------------------------------------- Home

@Composable
fun HomeScreen(
    songs: List<Song>,
    favorites: Set<Long>,
    currentId: Long?,
    loading: Boolean,
    onPlay: (List<Song>, Int) -> Unit,
    onShuffle: (List<Song>) -> Unit,
) {
    if (songs.isEmpty()) {
        MessageBox(
            title = if (loading) "Loading your music" else "No music found",
            body = "Songs stored on this phone will show up here.",
        )
        return
    }

    val recent = remember(songs) { songs.sortedByDescending { it.dateAdded }.take(20) }
    val favoriteSongs = remember(songs, favorites) { songs.filter { it.id in favorites } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item { ScreenTitle("Home") }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = { onPlay(songs, 0) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Play")
                }
                FilledTonalButton(onClick = { onShuffle(songs) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Shuffle")
                }
            }
        }
        item { SectionTitle("Recently added") }
        item { SongCardRow(recent, currentId) { index -> onPlay(recent, index) } }
        if (favoriteSongs.isNotEmpty()) {
            item { SectionTitle("Favorites") }
            item { SongCardRow(favoriteSongs, currentId) { index -> onPlay(favoriteSongs, index) } }
        }
        item {
            Text(
                text = "${songs.size} songs in your library",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp),
            )
        }
    }
}

@Composable
private fun SongCardRow(list: List<Song>, currentId: Long?, onClick: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        itemsIndexed(list, key = { _, song -> song.id }) { index, song ->
            Column(
                modifier = Modifier
                    .width(140.dp)
                    .clickable { onClick(index) },
            ) {
                ArtImage(
                    uri = song.uri,
                    modifier = Modifier.size(140.dp),
                    size = 384,
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = song.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (song.id == currentId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = song.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Search

@Composable
fun SearchScreen(
    songs: List<Song>,
    currentId: Long?,
    onPlay: (List<Song>, Int) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(songs, query) {
        val q = query.trim()
        if (q.isEmpty()) {
            emptyList()
        } else {
            songs.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.artist.contains(q, ignoreCase = true) ||
                    it.album.contains(q, ignoreCase = true)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Search")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Songs, artists, albums") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))
        when {
            query.isBlank() -> MessageBox("Search your music", "Type a song, artist or album name.")
            results.isEmpty() -> MessageBox("No results", "Nothing matches \"${query.trim()}\".")
            else -> SongList(results, currentId, onPlay)
        }
    }
}

// ---------------------------------------------------------------- Library

private enum class LibraryTab(val label: String) {
    Songs("Songs"),
    Albums("Albums"),
    Artists("Artists"),
    Favorites("Favorites"),
}

@Composable
fun LibraryScreen(
    songs: List<Song>,
    favorites: Set<Long>,
    currentId: Long?,
    onPlay: (List<Song>, Int) -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onOpenArtist: (Artist) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(LibraryTab.Songs) }
    val albums = remember(songs) { songs.toAlbums() }
    val artists = remember(songs) { songs.toArtists() }
    val favoriteSongs = remember(songs, favorites) { songs.filter { it.id in favorites } }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Library")
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LibraryTab.entries.forEach { entry ->
                FilterChip(
                    selected = tab == entry,
                    onClick = { tab = entry },
                    label = { Text(entry.label) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        when (tab) {
            LibraryTab.Songs -> SongList(songs, currentId, onPlay)
            LibraryTab.Favorites -> {
                if (favoriteSongs.isEmpty()) {
                    MessageBox("No favorites yet", "Tap the heart on the Now Playing screen to add songs here.")
                } else {
                    SongList(favoriteSongs, currentId, onPlay)
                }
            }
            LibraryTab.Albums -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(albums, key = { it.id }) { album ->
                    Column(Modifier.clickable { onOpenAlbum(album) }) {
                        ArtImage(
                            uri = album.songs.first().uri,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            size = 384,
                            shape = RoundedCornerShape(14.dp),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = album.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = album.artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            LibraryTab.Artists -> LazyColumn(Modifier.fillMaxSize()) {
                items(artists, key = { it.name }) { artist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenArtist(artist) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ArtImage(
                            uri = artist.songs.first().uri,
                            modifier = Modifier.size(52.dp),
                            size = 128,
                            shape = CircleShape,
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = artist.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "${artist.songs.size} songs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongList(
    list: List<Song>,
    currentId: Long?,
    onPlay: (List<Song>, Int) -> Unit,
) {
    if (list.isEmpty()) {
        MessageBox("Nothing here", "No songs to show.")
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(list, key = { _, song -> song.id }) { index, song ->
            SongRow(song = song, isCurrent = song.id == currentId, onClick = { onPlay(list, index) })
        }
    }
}

// ---------------------------------------------------------------- Album / artist page

@Composable
fun DetailScreen(
    title: String,
    subtitle: String,
    artUri: Uri?,
    songs: List<Song>,
    currentId: Long?,
    onBack: () -> Unit,
    onPlay: (List<Song>, Int) -> Unit,
    onShuffle: (List<Song>) -> Unit,
) {
    BackHandler(onBack = onBack)

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            IconButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ArtImage(
                    uri = artUri,
                    modifier = Modifier.size(220.dp),
                    size = 640,
                    shape = RoundedCornerShape(18.dp),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { onPlay(songs, 0) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Play")
                    }
                    FilledTonalButton(onClick = { onShuffle(songs) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Shuffle")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            SongRow(song = song, isCurrent = song.id == currentId, onClick = { onPlay(songs, index) })
        }
    }
}

// ---------------------------------------------------------------- Settings

@Composable
fun SettingsScreen(
    themeMode: String,
    accentIndex: Int,
    songCount: Int,
    canRescan: Boolean,
    onTheme: (String) -> Unit,
    onAccent: (Int) -> Unit,
    onRescan: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenTitle("Settings")

        SectionTitle("Appearance")
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (key, label) ->
                FilterChip(
                    selected = themeMode == key,
                    onClick = { onTheme(key) },
                    label = { Text(label) },
                )
            }
        }

        Text(
            text = "Accent color",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 10.dp),
        )
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AccentColors.forEachIndexed { index, color ->
                val selected = index == accentIndex
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (selected) {
                                Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            } else {
                                Modifier
                            },
                        )
                        .clickable { onAccent(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(Icons.Rounded.Check, contentDescription = AccentNames[index], tint = Color.White)
                    }
                }
            }
        }

        SectionTitle("Library")
        Text(
            text = "$songCount songs on this device",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onRescan,
            enabled = canRescan,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Text("Rescan music")
        }

        SectionTitle("About")
        Text(
            text = "Vibecord 0.1.0",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = "Made by ABDUL BASIR",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(24.dp))
    }
}
