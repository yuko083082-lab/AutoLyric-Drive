package com.example.autolyricdrive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LyricsViewModel(private val repository: LyricsRepository = LyricsRepository()) : ViewModel() {

    sealed class LyricsState {
        object Idle : LyricsState()
        object Loading : LyricsState()
        data class Success(val lrc: String, val offsetMs: Int) : LyricsState()
        data class Error(val message: String) : LyricsState()
    }

    private val _uiState = MutableStateFlow<LyricsState>(LyricsState.Idle)
    val uiState: StateFlow<LyricsState> = _uiState

    private var currentTitle: String = ""
    private var currentArtist: String = ""

    init {
        observeMusicEvents()
    }

    private fun observeMusicEvents() {
        viewModelScope.launch {
            MusicDataBus.musicEvents.collectLatest { musicInfo ->
                if (musicInfo.title != currentTitle || musicInfo.artist != currentArtist) {
                    currentTitle = musicInfo.title
                    currentArtist = musicInfo.artist
                    loadLyrics(musicInfo.title, musicInfo.artist, musicInfo.playOffsetMs)
                } else {
                    // 同じ曲の場合はオフセットのみ更新
                    val currentState = _uiState.value
                    if (currentState is LyricsState.Success) {
                        _uiState.value = currentState.copy(offsetMs = musicInfo.playOffsetMs)
                    }
                }
            }
        }
    }

    private fun loadLyrics(title: String, artist: String, offsetMs: Int) {
        viewModelScope.launch {
            _uiState.value = LyricsState.Loading
            val lrc = repository.fetchLyrics(title, artist)
            if (lrc != null) {
                _uiState.value = LyricsState.Success(lrc, offsetMs)
            } else {
                _uiState.value = LyricsState.Error("歌詞が見つかりませんでした")
            }
        }
    }
}
