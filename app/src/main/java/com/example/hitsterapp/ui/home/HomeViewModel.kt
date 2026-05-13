package com.example.hitsterapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hitsterapp.data.repository.PlaylistRepository
import com.example.hitsterapp.data.repository.PlaylistUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PlaylistRepository
) : ViewModel() {

    val playlists: StateFlow<List<PlaylistUiModel>> = repository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addPlaylist(url: String) {
        viewModelScope.launch { repository.fetchAndSavePlaylist(url) }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch { repository.deletePlaylist(playlistId) }
    }

    fun resetPlaylist(playlistId: String) {
        viewModelScope.launch { repository.resetPlaylistProgress(playlistId) }
    }
}
