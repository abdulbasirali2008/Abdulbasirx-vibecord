package com.abdulbasir.vibecord

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abdulbasir.vibecord.data.AudiusRepository
import com.abdulbasir.vibecord.data.MusicRepository
import com.abdulbasir.vibecord.data.OnlineResult
import com.abdulbasir.vibecord.data.OnlineTrack
import com.abdulbasir.vibecord.data.Song
import com.abdulbasir.vibecord.playback.PlayerController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("vibecord", Context.MODE_PRIVATE)

    val player = PlayerController(app)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _favorites = MutableStateFlow(
        (prefs.getStringSet("favorites", emptySet()) ?: emptySet())
            .mapNotNull { it.toLongOrNull() }
            .toSet(),
    )
    val favorites: StateFlow<Set<Long>> = _favorites.asStateFlow()

    private val _recents = MutableStateFlow(
        (prefs.getString("recents", "") ?: "").split(',').mapNotNull { it.toLongOrNull() },
    )
    /** Song ids, most recently played first. */
    val recents: StateFlow<List<Long>> = _recents.asStateFlow()

    init {
        viewModelScope.launch {
            player.state.map { it.mediaId }.distinctUntilChanged().collect { id ->
                val songId = id?.toLongOrNull() ?: return@collect
                val updated = (listOf(songId) + _recents.value.filter { it != songId }).take(30)
                _recents.value = updated
                prefs.edit().putString("recents", updated.joinToString(",")).apply()
            }
        }
    }

    private val _online = MutableStateFlow<OnlineResult>(OnlineResult.Loading)
    val online: StateFlow<OnlineResult> = _online.asStateFlow()

    private val _exploreQuery = MutableStateFlow("")
    val exploreQuery: StateFlow<String> = _exploreQuery.asStateFlow()

    private var onlineJob: Job? = null
    private var onlineLoaded = false

    /** Loads the trending list the first time the Explore tab opens. */
    fun loadExplore() {
        if (onlineLoaded) return
        fetchOnline(_exploreQuery.value, wait = 0L)
    }

    fun retryExplore() = fetchOnline(_exploreQuery.value, wait = 0L)

    /** Waits a moment after the last key press so we don't search on every letter. */
    fun setExploreQuery(query: String) {
        _exploreQuery.value = query
        fetchOnline(query, wait = 450L)
    }

    private fun fetchOnline(query: String, wait: Long) {
        onlineJob?.cancel()
        onlineJob = viewModelScope.launch {
            if (wait > 0) delay(wait)
            _online.value = OnlineResult.Loading
            _online.value = try {
                val q = query.trim()
                val tracks = if (q.isEmpty()) AudiusRepository.trending() else AudiusRepository.search(q)
                onlineLoaded = true
                OnlineResult.Success(tracks)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                OnlineResult.Error
            }
        }
    }

    fun playOnline(tracks: List<OnlineTrack>, index: Int) = player.playOnline(tracks, index)

    private val _themeMode = MutableStateFlow(prefs.getString("theme", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _accentIndex = MutableStateFlow(prefs.getInt("accent", 0))
    val accentIndex: StateFlow<Int> = _accentIndex.asStateFlow()

    fun loadSongs() {
        viewModelScope.launch {
            _loading.value = true
            _songs.value = MusicRepository.loadSongs(getApplication())
            _loading.value = false
        }
    }

    fun toggleFavorite(id: Long) {
        val updated = _favorites.value.toMutableSet()
        if (!updated.add(id)) updated.remove(id)
        _favorites.value = updated
        prefs.edit().putStringSet("favorites", updated.map { it.toString() }.toSet()).apply()
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme", mode).apply()
    }

    fun setAccent(index: Int) {
        _accentIndex.value = index
        prefs.edit().putInt("accent", index).apply()
    }

    fun shuffleAll(list: List<Song>) {
        if (list.isEmpty()) return
        player.play(list, list.indices.random(), shuffle = true)
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
