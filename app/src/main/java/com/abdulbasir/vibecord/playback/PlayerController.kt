package com.abdulbasir.vibecord.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.abdulbasir.vibecord.data.Song
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerState(
    val hasMedia: Boolean = false,
    val mediaId: String? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artUri: Uri? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
)

/** The app's side of the player: talks to [PlaybackService] and exposes its state. */
class PlayerController(private val context: Context) {

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var future: ListenableFuture<MediaController>? = null
    private var pendingAction: ((MediaController) -> Unit)? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
        }
    }

    init {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val connecting = MediaController.Builder(context, token).buildAsync()
        future = connecting
        connecting.addListener(
            {
                val connected = try {
                    connecting.get()
                } catch (e: Exception) {
                    null
                }
                if (connected != null) {
                    controller = connected
                    connected.addListener(listener)
                    publish(connected)
                    pendingAction?.invoke(connected)
                    pendingAction = null
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    private fun publish(player: Player) {
        val item = player.currentMediaItem
        _state.value = PlayerState(
            hasMedia = item != null,
            mediaId = item?.mediaId,
            title = item?.mediaMetadata?.title?.toString().orEmpty(),
            artist = item?.mediaMetadata?.artist?.toString().orEmpty(),
            album = item?.mediaMetadata?.albumTitle?.toString().orEmpty(),
            artUri = item?.localConfiguration?.uri ?: item?.requestMetadata?.mediaUri,
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L,
            shuffle = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
        )
    }

    /** Called by the screen a couple of times a second while music is playing. */
    fun refreshPosition() {
        controller?.let { publish(it) }
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else pendingAction = block
    }

    fun play(songs: List<Song>, startIndex: Int, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        val items = songs.map { it.toMediaItem() }
        withController { c ->
            c.shuffleModeEnabled = shuffle
            c.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
            c.prepare()
            c.play()
        }
    }

    fun togglePlay() = withController { c -> if (c.isPlaying) c.pause() else c.play() }

    fun next() = withController { c -> c.seekToNext() }

    fun previous() = withController { c -> c.seekToPrevious() }

    fun seekTo(positionMs: Long) = withController { c -> c.seekTo(positionMs) }

    fun toggleShuffle() = withController { c -> c.shuffleModeEnabled = !c.shuffleModeEnabled }

    fun cycleRepeat() = withController { c ->
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun release() {
        future?.let { MediaController.releaseFuture(it) }
        controller = null
    }
}

private fun Song.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .build(),
        )
        .build()
