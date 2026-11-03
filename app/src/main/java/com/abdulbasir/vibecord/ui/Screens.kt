package com.abdulbasir.vibecord.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    bottomPad: Dp,
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
        contentPadding = PaddingValues(top = statusBarTop(), bottom = bottomPad),
    ) {
        item { ScreenTitle("Home") }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlassButton(
                    text = "Play",
                    icon = Icons.Rounded.PlayArrow,
                    onClick = { onPlay(songs, 0) },
                    modifier = Modifier.weight(1f),
                    filled = true,
                )
                GlassButton(
                    text = "Shuffle",
                    icon = Icons.Rounded.Shuffle,
                    onClick = { onShuffle(songs) },
                    modifier = Modifier.weight(1f),
                )
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
                modifier = Modifier.padding(start = 20.dp, top = 24.dp),
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
                    .width(148.dp)
                    .clickable { onClick(index) },
            ) {
                ArtImage(
                    uri = song.uri,
                    modifier = Modifier
                        .size(148.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(22.dp)),
                    size = 384,
                    shape = RoundedCornerShape(22.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = song.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
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
    bottomPad: Dp,
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

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        ScreenTitle("Search")
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .glass(RoundedCornerShape(20.dp)),
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Songs, artists, albums") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(8.dp))
        when {
            query.isBlank() -> MessageBox("Search your music", "Type a song, artist or album name.")
            results.isEmpty() -> MessageBox("No results", "Nothing matches \"${query.trim()}\".")
            else -> SongList(results, currentId, bottomPad, onPlay)
        }
    }
}

// ---------------------------------------------------------------- Library

private val LibraryLabels = listOf("Songs", "Albums", "Artists", "Favorites")

@Composable
fun LibraryScreen(
    songs: List<Song>,
    favorites: Set<Long>,
    currentId: Long?,
    bottomPad: Dp,
    onPlay: (List<Song>, Int) -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onOpenArtist: (Artist) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val albums = remember(songs) { songs.toAlbums() }
    val artists = remember(songs) { songs.toArtists() }
    val favoriteSongs = remember(songs, favorites) { songs.filter { it.id in favorites } }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        ScreenTitle("Library")
        GlassSegmented(
            options = LibraryLabels,
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))

        when (tab) {
            0 -> SongList(songs, currentId, bottomPad, onPlay)
            3 -> {
                if (favoriteSongs.isEmpty()) {
                    MessageBox("No favorites yet", "Tap the heart on the Now Playing screen to add songs here.")
                } else {
                    SongList(favoriteSongs, currentId, bottomPad, onPlay)
                }
            }
            1 -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPad),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(albums, key = { it.id }) { album ->
                    Column(Modifier.clickable { onOpenAlbum(album) }) {
                        ArtImage(
                            uri = album.songs.first().uri,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(22.dp)),
                            size = 384,
                            shape = RoundedCornerShape(22.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = album.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
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
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomPad),
            ) {
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
                            modifier = Modifier.size(56.dp),
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
                                fontWeight = FontWeight.SemiBold,
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
    bottomPad: Dp,
    onPlay: (List<Song>, Int) -> Unit,
) {
    if (list.isEmpty()) {
        MessageBox("Nothing here", "No songs to show.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPad),
    ) {
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
    bottomPad: Dp,
    onBack: () -> Unit,
    onPlay: (List<Song>, Int) -> Unit,
    onShuffle: (List<Song>) -> Unit,
) {
    BackHandler(onBack = onBack)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = statusBarTop() + 8.dp, bottom = bottomPad),
    ) {
        item {
            GlassIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                description = "Back",
                onClick = onBack,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ArtImage(
                    uri = artUri,
                    modifier = Modifier
                        .size(230.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(28.dp)),
                    size = 640,
                    shape = RoundedCornerShape(28.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassButton(
                        text = "Play",
                        icon = Icons.Rounded.PlayArrow,
                        onClick = { onPlay(songs, 0) },
                        modifier = Modifier.weight(1f),
                        filled = true,
                    )
                    GlassButton(
                        text = "Shuffle",
                        icon = Icons.Rounded.Shuffle,
                        onClick = { onShuffle(songs) },
                        modifier = Modifier.weight(1f),
                    )
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

private val ThemeKeys = listOf("system", "light", "dark")
private val ThemeLabels = listOf("System", "Light", "Dark")

@Composable
fun SettingsScreen(
    themeMode: String,
    accentIndex: Int,
    songCount: Int,
    canRescan: Boolean,
    bottomPad: Dp,
    onTheme: (String) -> Unit,
    onAccent: (Int) -> Unit,
    onRescan: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomPad),
    ) {
        ScreenTitle("Settings")

        SettingsHeader("Appearance")
        GlassCard(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Theme", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                GlassSegmented(
                    options = ThemeLabels,
                    selected = ThemeKeys.indexOf(themeMode).coerceAtLeast(0),
                    onSelect = { onTheme(ThemeKeys[it]) },
                )
                Spacer(Modifier.height(20.dp))
                Text("Accent color", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    AccentColors.forEachIndexed { index, color ->
                        val selected = index == accentIndex
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    if (selected) 3.dp else 1.dp,
                                    if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.5f),
                                    CircleShape,
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
            }
        }

        SettingsHeader("Library")
        GlassCard(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("$songCount songs on this device", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onRescan, enabled = canRescan) {
                    Text("Rescan music")
                }
            }
        }

        SettingsHeader("About")
        GlassCard(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Vibecord 0.3.0", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Made by ABDUL BASIR",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Lyrics from LRCLIB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsHeader(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 8.dp),
    )
}
