package com.birdsoundkeyboard

import android.content.Intent
import android.provider.Settings
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class BirdSoundModule(
    private val reactContext: ReactApplicationContext
) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "BirdSound"

    @ReactMethod
    fun setSelectedBird(birdId: String, promise: Promise) {
        if (birdId != "1" && birdId != "2") {
            promise.reject("UNAVAILABLE_BIRD", "This bird is not available yet.")
            return
        }

        reactContext.getSharedPreferences(PREFERENCES_NAME, 0)
            .edit()
            .putString(SELECTED_BIRD_KEY, birdId)
            .apply()
        promise.resolve(true)
    }

    @ReactMethod
    fun openInputMethodSettings(promise: Promise) {
        try {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            reactContext.startActivity(intent)
            promise.resolve(true)
        } catch (error: Exception) {
            promise.reject("SETTINGS_UNAVAILABLE", error.message, error)
        }
    }

    companion object {
        const val PREFERENCES_NAME = "BirdSoundPreferences"
        const val SELECTED_BIRD_KEY = "selectedBird"
    }
}