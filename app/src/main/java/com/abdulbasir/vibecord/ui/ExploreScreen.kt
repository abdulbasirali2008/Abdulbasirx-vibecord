package com.abdulbasir.vibecord.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abdulbasir.vibecord.data.OnlineResult
import com.abdulbasir.vibecord.data.OnlineTrack

/** Online music: what's trending, and a search box that looks through all of it. */
@Composable
fun ExploreScreen(
    result: OnlineResult,
    query: String,
    currentMediaId: String?,
    bottomPad: Dp,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onPlay: (List<OnlineTrack>, Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        ScreenTitle("Explore")
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .glass(RoundedCornerShape(20.dp)),
        ) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search online music") },
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

        when (result) {
            OnlineResult.Loading -> MessageBox(
                title = if (query.isBlank()) "Loading trending songs" else "Searching",
                body = "One moment…",
            )
            OnlineResult.Error -> MessageBox(
                title = "Can't reach the music service",
                body = "Check your internet connection and try again.",
                action = { Button(onClick = onRetry) { Text("Try again") } },
            )
            is OnlineResult.Success -> {
                if (result.tracks.isEmpty()) {
                    MessageBox("No results", "Nothing found for \"${query.trim()}\".")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = bottomPad),
                    ) {
                        item { SectionTitle(if (query.isBlank()) "Trending" else "Results") }
                        itemsIndexed(result.tracks, key = { index, track -> "$index:${track.id}" }) { index, track ->
                            OnlineRow(
                                track = track,
                                isCurrent = currentMediaId == "audius:${track.id}",
                                onClick = { onPlay(result.tracks, index) },
                            )
                        }
                        item {
                            Text(
                                text = "Music from Audius",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 20.dp, top = 20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineRow(track: OnlineTrack, isCurrent: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtImage(
            uri = track.artworkUrl?.let { Uri.parse(it) },
            modifier = Modifier.size(56.dp),
            size = 160,
            shape = RoundedCornerShape(13.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = track.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (track.durationMs > 0) "${track.artist} · ${formatTime(track.durationMs)}" else track.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
