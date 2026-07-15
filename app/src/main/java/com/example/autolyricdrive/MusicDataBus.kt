package com.example.autolyricdrive

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * RecognitionService と LyricsViewModel を繋ぐためのシングルトンイベントバス
 */
object MusicDataBus {
    data class MusicInfo(
        val title: String,
        val artist: String,
        val playOffsetMs: Int
    )

    private val _musicEvents = MutableSharedFlow<MusicInfo>(replay = 1)
    val musicEvents = _musicEvents.asSharedFlow()

    suspend fun post(info: MusicInfo) {
        _musicEvents.emit(info)
    }
}
