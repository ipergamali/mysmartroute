package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import android.media.MediaPlayer
import com.ioannapergamali.mysmartroute.R

object SoundManager {
    private var mediaPlayer: MediaPlayer? = null

    fun initialize(context: Context) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, R.raw.soundtrack).apply {
                isLooping = true
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
