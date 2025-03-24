package com.example.gettahoofin

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.Closeable

/**
 * Handles playing sound effects in the app
 * Implements Closeable to work with ResourceManager
 */
class SoundPlayer(
    context: Context,
    private val appSettings: AppSettings // Add AppSettings parameter
) : Closeable {
    private val soundPool: SoundPool
    private val intervalChangeSound: Int

    init {
        // Set up sound pool
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        // Load sounds - assuming you have an interval change sound in res/raw
        // If you don't have this resource, you'll need to create it or modify this code
        intervalChangeSound = soundPool.load(context, R.raw.interval_change, 1)
    }

    /**
     * Play the interval change sound with volume from settings
     */
    fun playIntervalChangeSound() {
        if (appSettings.isSoundEnabled) {
            // Use the volume setting from appSettings (0.0f to 1.0f)
            val volume = appSettings.soundVolume
            soundPool.play(intervalChangeSound, volume, volume, 1, 0, 1.0f)
        }
    }

    /**
     * Release resources (implements Closeable interface)
     */
    override fun close() {
        soundPool.release()
        AppLogger.d("SoundPlayer", "SoundPool resources released")
    }

    /**
     * Legacy method for backward compatibility with existing code
     */
    @Suppress("unused")
    fun release() {
        close()
    }
}