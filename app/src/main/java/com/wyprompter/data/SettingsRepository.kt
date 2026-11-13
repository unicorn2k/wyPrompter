package com.wyprompter.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.core.content.edit

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var fontSize: Float
        get() = prefs.getFloat(KEY_FONT_SIZE, 28f)
        set(value) = prefs.edit { putFloat(KEY_FONT_SIZE, value) }

    var scrollSpeed: Float
        get() = prefs.getFloat(KEY_SCROLL_SPEED, 0.5f)
        set(value) = prefs.edit { putFloat(KEY_SCROLL_SPEED, value) }

    var opacity: Float
        get() = prefs.getFloat(KEY_OPACITY, 0.9f)
        set(value) = prefs.edit { putFloat(KEY_OPACITY, value) }

    var fontColor: Int
        get() = prefs.getInt(KEY_FONT_COLOR, Color.WHITE)
        set(value) = prefs.edit { putInt(KEY_FONT_COLOR, value) }

    var autoScroll: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SCROLL, true)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_SCROLL, value) }

    var scrollUp: Boolean
        get() = prefs.getBoolean(KEY_SCROLL_UP, false)
        set(value) = prefs.edit { putBoolean(KEY_SCROLL_UP, value) }

    var currentManuscriptId: Long
        get() = prefs.getLong(KEY_MANUSCRIPT_ID, -1L)
        set(value) = prefs.edit { putLong(KEY_MANUSCRIPT_ID, value) }

    var overlayWidth: Int
        get() = prefs.getInt(KEY_OVERLAY_WIDTH, 0)
        set(value) = prefs.edit { putInt(KEY_OVERLAY_WIDTH, value) }

    var overlayHeight: Int
        get() = prefs.getInt(KEY_OVERLAY_HEIGHT, 0)
        set(value) = prefs.edit { putInt(KEY_OVERLAY_HEIGHT, value) }

    var overlayX: Int
        get() = prefs.getInt(KEY_OVERLAY_X, Int.MIN_VALUE)
        set(value) = prefs.edit { putInt(KEY_OVERLAY_X, value) }

    var overlayY: Int
        get() = prefs.getInt(KEY_OVERLAY_Y, Int.MIN_VALUE)
        set(value) = prefs.edit { putInt(KEY_OVERLAY_Y, value) }

    var nextKey: Int
        get() = prefs.getInt(KEY_NEXT_KEY, 0)
        set(value) = prefs.edit { putInt(KEY_NEXT_KEY, value) }

    var prevKey: Int
        get() = prefs.getInt(KEY_PREV_KEY, 0)
        set(value) = prefs.edit { putInt(KEY_PREV_KEY, value) }

    /** Overlay background color; default transparent. */
    var backgroundColor: Int
        get() = prefs.getInt(KEY_BACKGROUND_COLOR, Color.TRANSPARENT)
        set(value) = prefs.edit { putInt(KEY_BACKGROUND_COLOR, value) }

    /**
     * Overlay size preset: 0 = 1/3 screen height, 1 = 1/2, 2 = 2/3,
     * -1 = custom (user resized via the overlay's resize button).
     */
    var overlayPresetSize: Int
        get() = prefs.getInt(KEY_OVERLAY_PRESET, 1).coerceIn(-1, 2)
        set(value) = prefs.edit { putInt(KEY_OVERLAY_PRESET, value) }

    /** True = 开始提词 opens the floating window; false = in-app fullscreen. */
    var floatingWindow: Boolean
        get() = prefs.getBoolean(KEY_FLOATING_WINDOW, true)
        set(value) = prefs.edit { putBoolean(KEY_FLOATING_WINDOW, value) }

    companion object {
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_SCROLL_SPEED = "scroll_speed"
        private const val KEY_OPACITY = "opacity"
        private const val KEY_FONT_COLOR = "font_color"
        private const val KEY_AUTO_SCROLL = "auto_scroll"
        private const val KEY_SCROLL_UP = "scroll_up"
        private const val KEY_MANUSCRIPT_ID = "manuscript_id"
        private const val KEY_OVERLAY_WIDTH = "overlay_width"
        private const val KEY_OVERLAY_HEIGHT = "overlay_height"
        private const val KEY_OVERLAY_X = "overlay_x"
        private const val KEY_OVERLAY_Y = "overlay_y"
        private const val KEY_NEXT_KEY = "next_key"
        private const val KEY_PREV_KEY = "prev_key"
        private const val KEY_BACKGROUND_COLOR = "background_color"
        private const val KEY_OVERLAY_PRESET = "overlay_preset"
        private const val KEY_FLOATING_WINDOW = "floating_window"
    }
}