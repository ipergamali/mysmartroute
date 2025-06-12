package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import android.media.MediaPlayer

object SoundManager {
    private var mediaPlayer: MediaPlayer? = null

    fun initialize(context: Context) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                val fd = context.assets.openFd("soundtrack.mp3")
                setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                isLooping = true
                prepare()
            }
        }
    }

    fun play() { mediaPlayer?.start() }
    fun pause() { mediaPlayer?.pause() }

    fun setVolume(volume: Float) {
        mediaPlayer?.setVolume(volume, volume)
    }

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
