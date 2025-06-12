package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.content.res.AssetFileDescriptor

object SoundManager {
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return

        val appContext = context.applicationContext
        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val afd: AssetFileDescriptor = appContext.assets.openFd("soundtrack.mp3")
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(attrs)
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            isLooping = true
            prepare()
        }

        initialized = true
    }

    fun play() {
        mediaPlayer?.let {
            audioManager?.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            it.start()
        }
    }
    fun pause() {
        mediaPlayer?.pause()
        audioManager?.abandonAudioFocus(null)
    }

    fun setVolume(volume: Float) {
        mediaPlayer?.setVolume(volume, volume)
    }

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        initialized = false
    }
}
