package com.birdsoundkeyboard

import android.inputmethodservice.InputMethodService
import android.media.AudioAttributes
import android.media.SoundPool
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.random.Random

class BirdSoundIME : InputMethodService() {

    private var soundPool: SoundPool? = null
    private val soundIds = mutableListOf<Int>()
    private val loadedSoundIds = mutableSetOf<Int>()
    private var lastPlayedIndex = -1
    private var loadedBirdId: String? = null

    private var isShifted = false
    private var isCapsLock = false
    private var isSymbolMode = false
    private var isSecondSymbolPage = false
    private var lastShiftTapTime = 0L

    private var keyboardRoot: LinearLayout? = null

    private val keyboardBackground = Color.rgb(24, 32, 25)
    private val keyBackground = Color.rgb(49, 65, 49)
    private val specialKeyBackground = Color.rgb(65, 82, 64)
    private val keyTextColor = Color.rgb(243, 246, 237)
    private val accentColor = Color.rgb(184, 217, 154)

    override fun onCreate() {
        super.onCreate()
        initializeSounds(forceReload = true)
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        initializeSounds(forceReload = false)
    }

    private fun initializeSounds(forceReload: Boolean) {
        val selectedBirdId = getSharedPreferences(BirdSoundModule.PREFERENCES_NAME, 0)
            .getString(BirdSoundModule.SELECTED_BIRD_KEY, "1") ?: "1"

        if (!forceReload && selectedBirdId == loadedBirdId && soundPool != null) return

        soundPool?.release()
        soundPool = null
        soundIds.clear()
        loadedSoundIds.clear()
        lastPlayedIndex = -1
        loadedBirdId = selectedBirdId

        val audioResources = when (selectedBirdId) {
            "2" -> listOf(R.raw.robin_1, R.raw.robin_2, R.raw.robin_3)
            else -> listOf(R.raw.sparrow_1, R.raw.sparrow_2, R.raw.sparrow_3)
        }

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attributes)
            .build()
            .apply {
                setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0) {
                        loadedSoundIds.add(sampleId)
                    }
                }
            }

        try {
            audioResources.forEach { resourceId ->
                soundIds.add(soundPool!!.load(this, resourceId, 1))
            }
        } catch (_: Exception) {
            soundPool?.release()
            soundPool = null
            soundIds.clear()
        }
    }

    private fun playBirdSound() {
        val pool = soundPool ?: return

        val availableIndices = soundIds.indices.filter { index ->
            index != lastPlayedIndex &&
                loadedSoundIds.contains(soundIds[index])
        }

        if (availableIndices.isEmpty()) return

        val index = availableIndices[
            Random.nextInt(availableIndices.size)
        ]

        val streamId = pool.play(
            soundIds[index],
            0.65f,
            0.65f,
            1,
            0,
            1.0f
        )

        if (streamId != 0) {
            lastPlayedIndex = index
        }
    }

    override fun onCreateInputView(): View {
        val keyboard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
            setOnApplyWindowInsetsListener { view: View, insets: android.view.WindowInsets ->
                val bottomInset = if (android.os.Build.VERSION.SDK_INT >= 30) {
                    insets.getInsets(
                        android.view.WindowInsets.Type.navigationBars()
                    ).bottom
                } else {
                    0
                }

                view.setPadding(
                    dp(4),
                    dp(4),
                    dp(4),
                    dp(4) + bottomInset
                )

                insets
            }
            setBackgroundColor(keyboardBackground)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        keyboardRoot = keyboard
        window?.window?.navigationBarColor = keyboardBackground

        if (android.os.Build.VERSION.SDK_INT >= 29) {
            window?.window?.isNavigationBarContrastEnforced = true
        }

        val title = TextView(this).apply {
            text = "🐦 BirdSound"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(accentColor)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(2))
        }

        keyboard.addView(title)
        renderKeys(keyboard)

        return keyboard
    }

    private fun renderKeys(keyboard: LinearLayout) {
        keyboard.removeViews(
            1,
            (keyboard.childCount - 1).coerceAtLeast(0)
        )

        if (!isSymbolMode) {
            addKeyRow(
                keyboard,
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
            )
            addKeyRow(
                keyboard,
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
            )
            addKeyRow(
                keyboard,
                listOf("⇧", "z", "x", "c", "v", "b", "n", "m", "⌫")
            )
            addKeyRow(
                keyboard,
                listOf("?123", ",", "space", ".", "↵")
            )
        } else if (!isSecondSymbolPage) {
            addKeyRow(
                keyboard,
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
            )
            addKeyRow(
                keyboard,
                listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/")
            )
            addKeyRow(
                keyboard,
                listOf("=\\<", "*", "\"", "'", ":", ";", "!", "?", "⌫")
            )
            addKeyRow(
                keyboard,
                listOf("ABC", ",", "space", ".", "↵")
            )
        } else {
            addKeyRow(
                keyboard,
                listOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "Δ")
            )
            addKeyRow(
                keyboard,
                listOf("£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\")
            )
            addKeyRow(
                keyboard,
                listOf("<", ">", "[", "]", "_", "©", "®", "™", "⌫")
            )
            addKeyRow(
                keyboard,
                listOf("?123", "ABC", "space", ".", "↵")
            )
        }
    }

    private fun addKeyRow(
        keyboard: LinearLayout,
        keys: List<String>
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        for (key in keys) {
            val displayedText = when (key) {
                "space" -> "English (US)"
                else -> if (
                    !isSymbolMode &&
                    key.length == 1 &&
                    key[0].isLetter()
                ) {
                    if (isShifted || isCapsLock) key.uppercase()
                    else key
                } else {
                    key
                }
            }

            val button = Button(this).apply {
                text = displayedText
                textSize = if (key == "space") 11f else 17f
                isAllCaps = false
                setTextColor(
                    if (key == "⇧" && (isShifted || isCapsLock)) {
                        accentColor
                    } else {
                        keyTextColor
                    }
                )

                val isSpecial = key in listOf(
                    "⇧", "⌫", "?123", "ABC", "↵", "=\\<"
                )

                setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                        if (isSpecial) specialKeyBackground
                        else keyBackground
                    )
                )

                setPadding(0, 0, 0, 0)
                minWidth = 0
                minimumWidth = 0
                minHeight = dp(45)
                minimumHeight = dp(45)

                layoutParams = LinearLayout.LayoutParams(
                    0,
                    dp(45),
                    when (key) {
                        "space" -> 2.5f
                        "⇧", "⌫", "?123", "ABC", "↵", "=\\<" -> 1.35f
                        else -> 1f
                    }
                ).apply {
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                }

                setOnClickListener {
                    handleKey(key)
                }
            }

            row.addView(button)
        }

        keyboard.addView(row)
    }

    private fun handleKey(key: String) {
        val connection: InputConnection =
            currentInputConnection ?: return

        when (key) {
            "⇧" -> {
                val now = System.currentTimeMillis()

                if (now - lastShiftTapTime < 400L) {
                    isCapsLock = !isCapsLock
                    isShifted = isCapsLock
                } else if (!isCapsLock) {
                    isShifted = !isShifted
                }

                lastShiftTapTime = now
                refreshKeyboard()
                return
            }

            "?123" -> {
                isSymbolMode = true
                isSecondSymbolPage = false
                refreshKeyboard()
                return
            }

            "=\\<" -> {
                isSecondSymbolPage = true
                refreshKeyboard()
                return
            }

            "ABC" -> {
                isSymbolMode = false
                isSecondSymbolPage = false
                refreshKeyboard()
                return
            }

            "⌫" -> {
                val deleted = connection.deleteSurroundingText(1, 0)

                if (!deleted) {
                    connection.sendKeyEvent(
                        KeyEvent(
                            KeyEvent.ACTION_DOWN,
                            KeyEvent.KEYCODE_DEL
                        )
                    )
                    connection.sendKeyEvent(
                        KeyEvent(
                            KeyEvent.ACTION_UP,
                            KeyEvent.KEYCODE_DEL
                        )
                    )
                }
                playBirdSound()
                return
            }

            "space" -> {
                connection.commitText(" ", 1)
                playBirdSound()
                consumeShift()
                return
            }

            "↵" -> {
                val editorInfo = currentInputEditorInfo
                val inputType = editorInfo?.inputType ?: 0

                val isMultiline = (
                    inputType and
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                ) != 0

                if (isMultiline) {
                    connection.commitText("\n", 1)
                } else {
                    val action = editorInfo?.imeOptions?.and(
                        android.view.inputmethod.EditorInfo.IME_MASK_ACTION
                    ) ?: android.view.inputmethod.EditorInfo.IME_ACTION_UNSPECIFIED

                    if (
                        action != android.view.inputmethod.EditorInfo.IME_ACTION_UNSPECIFIED &&
                        action != android.view.inputmethod.EditorInfo.IME_ACTION_NONE
                    ) {
                        connection.performEditorAction(action)
                    } else {
                        connection.sendKeyEvent(
                            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
                        )
                        connection.sendKeyEvent(
                            KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)
                        )
                    }
                }

                playBirdSound()
                consumeShift()
                return
            }

            "," , "." -> {
                connection.commitText(key, 1)
                playBirdSound()
                consumeShift()
                return
            }

            else -> {
                val output = if (
                    !isSymbolMode &&
                    key.length == 1 &&
                    key[0].isLetter() &&
                    (isShifted || isCapsLock)
                ) {
                    key.uppercase()
                } else {
                    key
                }

                connection.commitText(output, 1)
                playBirdSound()
                consumeShift()
            }
        }
    }

    private fun consumeShift() {
        if (isShifted && !isCapsLock) {
            isShifted = false
            refreshKeyboard()
        }
    }

    private fun refreshKeyboard() {
        keyboardRoot?.let { renderKeys(it) }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        soundPool?.release()
        soundPool = null
        soundIds.clear()
        loadedSoundIds.clear()
        keyboardRoot = null
        super.onDestroy()
    }
}