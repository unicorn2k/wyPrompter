package com.wyprompter.remote

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent

/**
 * Maps Bluetooth remote / presenter key events to prev/next paragraph actions.
 * The pure mapping lives in [translateKey] so it can be unit tested; it is used
 * by the in-app (non-floating-window) teleprompter to page through the script.
 */
class RemoteKeyController(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())

    var onMediaKey: ((keyCode: Int) -> Unit)? = null
    var onVolumeKey: ((keyCode: Int) -> Unit)? = null

    private var lastVolume: Int = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    private val volumePoll = object : Runnable {
        override fun run() {
            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (current != lastVolume) {
                val key =
                    if (current > lastVolume) KeyEvent.KEYCODE_VOLUME_UP
                    else KeyEvent.KEYCODE_VOLUME_DOWN
                Log.d(TAG, "volume ${lastVolume} -> $current => $key")
                onVolumeKey?.invoke(key)
                lastVolume = current
            }
            handler.postDelayed(this, 100L)
        }
    }

    fun start() {
        lastVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        handler.post(volumePoll)
    }

    fun stop() {
        handler.removeCallbacks(volumePoll)
    }

    /** Translate a captured key code into a prev/next action (pure function). */
    fun translate(keyCode: Int): Action = translateKey(keyCode)

    enum class Action { NEXT, PREV, TOGGLE, IGNORED }

    companion object {
        const val TAG = "wyRemote"

        /** Pure key-to-action mapping, kept here so it can be unit tested. */
        fun translateKey(keyCode: Int): Action {
            return when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_MEDIA_NEXT,
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_PAGE_DOWN,
                KeyEvent.KEYCODE_CAMERA,
                KeyEvent.KEYCODE_FOCUS,
                KeyEvent.KEYCODE_PROFILE_SWITCH -> Action.NEXT

                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                KeyEvent.KEYCODE_MEDIA_REWIND,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_PAGE_UP -> Action.PREV

                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_MEDIA_PLAY,
                KeyEvent.KEYCODE_MEDIA_PAUSE,
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_SPACE,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER -> Action.TOGGLE

                else -> Action.IGNORED
            }
        }
    }
}