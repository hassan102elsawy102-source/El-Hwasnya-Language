package com.elhwasny.keyboard

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo

class KeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private lateinit var keyboardView: KeyboardView
    private lateinit var keyboard: Keyboard
    private var vibrator: Vibrator? = null

    private val cipherAlphabet = "ابتثجحخدذرزسشصضطظعغفقكلمنهوي"

    private enum class CipherMode { NONE, ENCODE, DECODE }
    private var cipherMode = CipherMode.NONE

    companion object {
        private const val KEYCODE_CIPHER = -100
    }

    override fun onCreateInputView(): View {
        vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator

        keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null) as KeyboardView
        keyboard = Keyboard(this, R.xml.qwerty)
        keyboardView.keyboard = keyboard
        keyboardView.setOnKeyboardActionListener(this)

        return keyboardView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        cipherMode = CipherMode.NONE
        updateCipherKeyLabel()
    }

    private fun shiftChar(ch: Char, dir: Int): Char {
        val idx = cipherAlphabet.indexOf(ch)
        if (idx == -1) return ch
        val len = cipherAlphabet.length
        val newIdx = ((idx + dir) % len + len) % len
        return cipherAlphabet[newIdx]
    }

    private fun updateCipherKeyLabel() {
        val cipherKey = keyboard.keys.firstOrNull {
            it.codes.isNotEmpty() && it.codes[0] == KEYCODE_CIPHER
        } ?: return

        cipherKey.label = when (cipherMode) {
            CipherMode.NONE -> "EHL"
            CipherMode.ENCODE -> "EHL ↑"
            CipherMode.DECODE -> "EHL ↓"
        }
        keyboardView.invalidateAllKeys()
    }

    private fun vibrateKey() {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(12)
        }
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        vibrateKey()
        val ic = currentInputConnection ?: return

        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> {
                ic.deleteSurroundingText(1, 0)
            }

            KEYCODE_CIPHER -> {
                cipherMode = when (cipherMode) {
                    CipherMode.NONE -> CipherMode.ENCODE
                    CipherMode.ENCODE -> CipherMode.DECODE
                    CipherMode.DECODE -> CipherMode.NONE
                }
                updateCipherKeyLabel()
            }

            Keyboard.KEYCODE_DONE -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }

            32 -> {
                ic.commitText(" ", 1)
            }

            else -> {
                var ch = primaryCode.toChar()
                ch = when (cipherMode) {
                    CipherMode.ENCODE -> shiftChar(ch, 1)
                    CipherMode.DECODE -> shiftChar(ch, -1)
                    CipherMode.NONE -> ch
                }
                ic.commitText(ch.toString(), 1)
            }
        }
    }

    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}
    override fun onText(text: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}

    fun encodeText(input: String): String =
        input.map { shiftChar(it, 1) }.joinToString("")

    fun decodeText(input: String): String =
        input.map { shiftChar(it, -1) }.joinToString("")
}
