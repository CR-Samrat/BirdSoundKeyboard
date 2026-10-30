package com.birdsoundkeyboard

import android.media.AudioAttributes
import android.media.SoundPool
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import kotlin.random.Random

class BirdSoundModule(
    private val reactContext: ReactApplicationContext
) : ReactContextBaseJavaModule(reactContext) {

    private var soundPool: SoundPool? = null
    private val soundIds = mutableListOf<Int>()
    private val loadedSounds = mutableSetOf<Int>()
    private var lastPlayedIndex = -1
    private var loadPromise: Promise? = null

    override fun getName(): String = "BirdSound"

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()
            .apply {
                setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0) {
                        loadedSounds.add(sampleId)
                    }

                    val promise = loadPromise
                    if (promise != null &&
                        loadedSounds.size == soundIds.size
                    ) {
                        loadPromise = null
                        if (loadedSounds.size == 3) {
                            promise.resolve(true)
                        } else {
                            promise.reject(
                                "AUDIO_LOAD_FAILED",
                                "One or more Sparrow clips could not be loaded."
                            )
                        }
                    }
                }
            }
    }

    @ReactMethod
    fun loadSparrowSounds(promise: Promise) {
        val pool = soundPool
        if (pool == null) {
            promise.reject("AUDIO_UNAVAILABLE", "SoundPool is unavailable.")
            return
        }

        if (loadedSounds.size == 3) {
            promise.resolve(true)
            return
        }

        if (loadPromise != null) {
            promise.reject("AUDIO_BUSY", "Audio loading is already in progress.")
            return
        }

        loadPromise = promise
        soundIds.clear()

        try {
            soundIds.add(pool.load(reactContext, R.raw.sparrow_1, 1))
            soundIds.add(pool.load(reactContext, R.raw.sparrow_2, 1))
            soundIds.add(pool.load(reactContext, R.raw.sparrow_3, 1))
        } catch (error: Exception) {
            loadPromise = null
            promise.reject("AUDIO_LOAD_FAILED", error.message, error)
        }
    }

    @ReactMethod
    fun playRandomSparrowSound(promise: Promise) {
        val pool = soundPool

        if (pool == null || loadedSounds.size != 3) {
            promise.reject(
                "AUDIO_NOT_READY",
                "Load all three Sparrow clips before playback."
            )
            return
        }

        val availableIndices = soundIds.indices.filter {
            it != lastPlayedIndex
        }

        val selectedIndex = availableIndices[Random.nextInt(availableIndices.size)]
        val soundId = soundIds[selectedIndex]

        val streamId = pool.play(soundId, 1f, 1f, 1, 0, 1f)

        if (streamId == 0) {
            promise.reject("AUDIO_PLAY_FAILED", "The Sparrow sound could not play.")
            return
        }

        lastPlayedIndex = selectedIndex
        promise.resolve(selectedIndex + 1)
    }

    override fun invalidate() {
        loadPromise?.reject("AUDIO_CANCELLED", "The audio module was closed.")
        loadPromise = null
        soundPool?.release()
        soundPool = null
        soundIds.clear()
        loadedSounds.clear()
        super.invalidate()
    }
}