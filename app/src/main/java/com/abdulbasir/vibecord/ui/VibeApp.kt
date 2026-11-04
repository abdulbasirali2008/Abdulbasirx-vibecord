package com.abdulbasir.vibecord.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abdulbasir.vibecord.MainViewModel
import com.abdulbasir.vibecord.data.Album
import com.abdulbasir.vibecord.data.Artist
import kotlinx.coroutines.delay

private enum class NavTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Rounded.Home),
    Search("Search", Icons.Rounded.Search),
    Library("Library", Icons.Rounded.LibraryMusic),
    Settings("Settings", Icons.Rounded.Settings),
}

private sealed interface Detail {
    data class AlbumPage(val album: Album) : Detail
    data class ArtistPage(val artist: Artist) : Detail
}

@Composable
fun VibeApp(vm: MainViewModel) {
    val context = LocalContext.current

    val songs by vm.songs.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val recents by vm.recents.collectAsStateWithLifecycle()
    val player by vm.player.state.collectAsStateWithLifecycle()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val accentIndex by vm.accentIndex.collectAsStateWithLifecycle()

    val audioPermission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            vm.loadSongs()
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Keep the seek bar moving while a song plays.
    val isPlaying = player.isPlaying
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            vm.player.refreshPosition()
            delay(250)
        }
    }

    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf<Detail?>(null) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }

    val currentId = player.mediaId?.toLongOrNull()
    val currentTab = NavTab.entries[tabIndex]

    // Room the floating mini player and tab bar take at the bottom, so lists
    // can scroll clear of them.
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPad = (if (player.hasMedia) 168.dp else 100.dp) + navBottom

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()

        Box(Modifier.fillMaxSize()) {
            val page = detail
            if (!hasPermission && currentTab != NavTab.Settings) {
                MessageBox(
                    title = "Allow access to your music",
                    body = "Vibecord needs permission to read the songs on your phone.",
                    action = {
                        Button(onClick = { permissionLauncher.launch(audioPermission) }) {
                            Text("Allow access")
                        }
                    },
                )
            } else if (page != null) {
                when (page) {
                    is Detail.AlbumPage -> DetailScreen(
                        title = page.album.name,
                        subtitle = "${page.album.artist} · ${page.album.songs.size} songs",
                        artUri = page.album.songs.first().uri,
                        songs = page.album.songs,
                        currentId = currentId,
                        bottomPad = bottomPad,
                        onBack = { detail = null },
                        onPlay = { list, index -> vm.player.play(list, index) },
                        onShuffle = vm::shuffleAll,
                    )
                    is Detail.ArtistPage -> DetailScreen(
                        title = page.artist.name,
                        subtitle = "${page.artist.songs.size} songs",
                        artUri = page.artist.songs.first().uri,
                        songs = page.artist.songs,
                        currentId = currentId,
                        bottomPad = bottomPad,
                        onBack = { detail = null },
                        onPlay = { list, index -> vm.player.play(list, index) },
                        onShuffle = vm::shuffleAll,
                    )
                }
            } else {
                when (currentTab) {
                    NavTab.Home -> HomeScreen(
                        songs = songs,
                        favorites = favorites,
                        recentIds = recents,
                        currentId = currentId,
                        loading = loading,
                        bottomPad = bottomPad,
                        onPlay = { list, index -> vm.player.play(list, index) },
                        onShuffle = vm::shuffleAll,
                        onToggleFavorite = vm::toggleFavorite,
                    )
                    NavTab.Search -> SearchScreen(
                        songs = songs,
                        currentId = currentId,
                        bottomPad = bottomPad,
                        onPlay = { list, index -> vm.player.play(list, index) },
                    )
                    NavTab.Library -> LibraryScreen(
                        songs = songs,
                        favorites = favorites,
                        currentId = currentId,
                        bottomPad = bottomPad,
                        onPlay = { list, index -> vm.player.play(list, index) },
                        onOpenAlbum = { detail = Detail.AlbumPage(it) },
                        onOpenArtist = { detail = Detail.ArtistPage(it) },
                    )
                    NavTab.Settings -> SettingsScreen(
                        themeMode = themeMode,
                        accentIndex = accentIndex,
                        songCount = songs.size,
                        canRescan = hasPermission && !loading,
                        bottomPad = bottomPad,
                        onTheme = vm::setThemeMode,
                        onAccent = vm::setAccent,
                        onRescan = vm::loadSongs,
                    )
                }
            }
        }

        // Lists fade out behind the floating bars instead of running under them.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(bottomPad)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (player.hasMedia) {
                Box(Modifier.padding(horizontal = 12.dp)) {
                    MiniPlayer(
                        state = player,
                        onToggle = vm.player::togglePlay,
                        onNext = vm.player::next,
                        onOpen = { showNowPlaying = true },
                    )
                }
            }
            FloatingNavBar(
                tabs = NavTab.entries.take(3).map { it.label to it.icon },
                selected = if (tabIndex < 3) tabIndex else -1,
                onSelect = {
                    tabIndex = it
                    detail = null
                },
                sideIcon = NavTab.Settings.icon,
                sideLabel = NavTab.Settings.label,
                sideSelected = currentTab == NavTab.Settings,
                onSide = {
                    tabIndex = NavTab.Settings.ordinal
                    detail = null
                },
            )
        }

        AnimatedVisibility(
            visible = showNowPlaying && player.hasMedia,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            NowPlayingScreen(
                state = player,
                isFavorite = currentId != null && currentId in favorites,
                onClose = { showNowPlaying = false },
                onToggle = vm.player::togglePlay,
                onNext = vm.player::next,
                onPrevious = vm.player::previous,
                onSeek = vm.player::seekTo,
                onShuffle = vm.player::toggleShuffle,
                onRepeat = vm.player::cycleRepeat,
                onFavorite = { currentId?.let(vm::toggleFavorite) },
            )
        }
    }
}
