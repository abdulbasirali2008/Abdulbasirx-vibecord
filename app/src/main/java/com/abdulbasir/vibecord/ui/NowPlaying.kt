package com.abdulbasir.vibecord.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.abdulbasir.vibecord.data.LyricsRepository
import com.abdulbasir.vibecord.data.LyricsResult
import com.abdulbasir.vibecord.playback.PlayerState

@Composable
fun NowPlayingScreen(
    state: PlayerState,
    isFavorite: Boolean,
    onClose: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onFavorite: () -> Unit,
) {
    BackHandler(onBack = onClose)

    val accent = MaterialTheme.colorScheme.primary
    val white = Color.White
    val soft = Color.White.copy(alpha = 0.70f)
    val night = Color(0xFF0B0B0D)

    var showLyrics by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val hasDuration = state.durationMs > 0
    val lyrics by produceState<LyricsResult>(LyricsResult.Loading, state.mediaId, showLyrics, hasDuration) {
        val id = state.mediaId
        if (!showLyrics || id == null || !hasDuration) return@produceState
        value = LyricsResult.Loading
        value = LyricsRepository.load(
            context = context,
            key = id,
            title = state.title,
            artist = state.artist,
            album = state.album,
            durationMs = state.durationMs,
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(night),
    ) {
        // The cover, blown up and blurred, is the whole backdrop.
        if (Build.VERSION.SDK_INT >= 31) {
            ArtImage(
                uri = state.artUri,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(80.dp),
                size = 256,
                shape = RectangleShape,
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.65f), night))),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.20f), Color.Black.copy(alpha = 0.60f)))),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(
                    icon = Icons.Rounded.KeyboardArrowDown,
                    description = "Close",
                    onClick = onClose,
                    dark = true,
                    tint = white,
                )
                Text(
                    text = "Now Playing",
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = soft,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.width(44.dp))
            }

            Spacer(Modifier.height(22.dp))

            if (showLyrics) {
                LyricsPanel(
                    result = lyrics,
                    positionMs = state.positionMs,
                    onSeek = onSeek,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                )
            } else {
                ArtImage(
                    uri = state.artUri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .shadow(30.dp, RoundedCornerShape(30.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(30.dp)),
                    size = 1024,
                    shape = RoundedCornerShape(30.dp),
                )
            }

            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(32.dp), dark = true)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = state.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = white,
                        )
                        Text(
                            text = state.artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 17.sp,
                            color = soft,
                        )
                    }
                    IconButton(onClick = { showLyrics = !showLyrics }) {
                        Icon(
                            imageVector = Icons.Rounded.FormatQuote,
                            contentDescription = "Lyrics",
                            tint = if (showLyrics) accent else soft,
                        )
                    }
                    IconButton(onClick = onFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) accent else soft,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                var dragging by remember { mutableStateOf<Float?>(null) }
                val duration = state.durationMs.coerceAtLeast(1L)
                val progress = (state.positionMs.toFloat() / duration).coerceIn(0f, 1f)

                Slider(
                    value = dragging ?: progress,
                    onValueChange = { dragging = it },
                    onValueChangeFinished = {
                        dragging?.let { onSeek((it * duration).toLong()) }
                        dragging = null
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = white,
                        activeTrackColor = white,
                        inactiveTrackColor = Color.White.copy(alpha = 0.28f),
                    ),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val shownPosition = dragging?.let { (it * duration).toLong() } ?: state.positionMs
                    Text(formatTime(shownPosition), fontSize = 12.sp, color = soft)
                    Text(formatTime(state.durationMs), fontSize = 12.sp, color = soft)
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onShuffle) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (state.shuffle) accent else soft,
                        )
                    }
                    IconButton(onClick = onPrevious) {
                        Icon(
                            Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = white,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .glass(CircleShape, dark = true, fill = 0.30f)
                            .clickable(onClick = onToggle),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = white,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                    IconButton(onClick = onNext) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = white,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    IconButton(onClick = onRepeat) {
                        Icon(
                            imageVector = if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            tint = if (state.repeatMode == Player.REPEAT_MODE_OFF) soft else accent,
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}
