package com.example.hitsterapp.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hitsterapp.data.db.entity.TrackEntity
import com.example.hitsterapp.data.repository.PlaylistRepository
import com.example.hitsterapp.data.repository.SpotifyRemoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrackUiModel(
    val id: String,
    val title: String,
    val artists: String,
    val releaseYear: Int
)

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isRevealed: Boolean = false,
    val currentTrack: TrackUiModel? = null,
    val isComplete: Boolean = false,
    val playedCount: Int = 0,
    val totalCount: Int = 0
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val remoteRepository: SpotifyRemoteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val playlistId: String = checkNotNull(savedStateHandle["playlistId"])

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val connected = remoteRepository.connect()
            if (connected) loadAndPlayNextTrack()
        }
    }

    private suspend fun loadAndPlayNextTrack() {
        val track = repository.getRandomUnplayedTrack(playlistId)
        if (track == null) {
            val total = repository.getTrackCount(playlistId)
            _uiState.update { it.copy(isComplete = true, totalCount = total, playedCount = total) }
            return
        }
        val played = repository.getPlayedCount(playlistId)
        val total = repository.getTrackCount(playlistId)
        _uiState.update {
            it.copy(
                currentTrack = track.toUiModel(),
                isRevealed = false,
                isPlaying = true,
                playedCount = played,
                totalCount = total
            )
        }
        remoteRepository.play(track.id)
    }

    fun togglePlayPause() {
        val playing = _uiState.value.isPlaying
        if (playing) remoteRepository.pause() else remoteRepository.resume()
        _uiState.update { it.copy(isPlaying = !playing) }
    }

    fun reveal() {
        _uiState.update { it.copy(isRevealed = true) }
    }

    fun next() {
        viewModelScope.launch {
            val currentId = _uiState.value.currentTrack?.id ?: return@launch
            repository.markTrackAsPlayed(playlistId, currentId)
            loadAndPlayNextTrack()
        }
    }

    override fun onCleared() {
        remoteRepository.disconnect()
        super.onCleared()
    }
}

private fun TrackEntity.toUiModel() = TrackUiModel(
    id = id,
    title = title,
    artists = artists,
    releaseYear = releaseYear
)
