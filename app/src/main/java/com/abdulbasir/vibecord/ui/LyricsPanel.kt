package com.abdulbasir.vibecord.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abdulbasir.vibecord.data.Lyrics
import com.abdulbasir.vibecord.data.LyricsResult

/** A glass panel that shows the lyrics, with the current line lit while the song plays. */
@Composable
fun LyricsPanel(
    result: LyricsResult,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.glass(RoundedCornerShape(30.dp), dark = true),
        contentAlignment = Alignment.Center,
    ) {
        when (result) {
            LyricsResult.Loading -> PanelMessage("Looking for lyrics…")
            LyricsResult.NotFound -> PanelMessage("No lyrics found for this song.")
            LyricsResult.Failed -> PanelMessage("Couldn't load lyrics. Check your internet connection.")
            is LyricsResult.Found -> LyricsList(result.lyrics, positionMs, onSeek)
        }
    }
}

@Composable
private fun PanelMessage(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.7f),
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(24.dp),
    )
}

@Composable
private fun LyricsList(lyrics: Lyrics, positionMs: Long, onSeek: (Long) -> Unit) {
    val listState = rememberLazyListState()
    val current = if (lyrics.synced) {
        lyrics.lines.indexOfLast { it.timeMs <= positionMs + 150 }
    } else {
        -1
    }

    // Keep the current line near the top as the song moves along.
    LaunchedEffect(current) {
        if (current >= 0) listState.animateScrollToItem(current)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 60.dp),
    ) {
        itemsIndexed(lyrics.lines) { index, line ->
            val isCurrent = index == current
            val color = when {
                !lyrics.synced -> Color.White.copy(alpha = 0.9f)
                isCurrent -> Color.White
                else -> Color.White.copy(alpha = 0.40f)
            }
            Text(
                text = line.text.ifEmpty { "♪" },
                color = color,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (lyrics.synced) Modifier.clickable { onSeek(line.timeMs) } else Modifier)
                    .padding(vertical = 8.dp),
            )
        }
    }
}
