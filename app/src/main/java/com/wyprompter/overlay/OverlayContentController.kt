package com.wyprompter.overlay

import android.view.View
import android.widget.TextView
import com.wyprompter.R
import com.wyprompter.scroll.AutoScrollEngine

/**
 * Owns the overlay content view and the auto-scroll engine.
 *
 * The whole manuscript is placed as one scrollable block and scrolled
 * continuously, teleprompter style. "Next"/"Previous" jump by one viewport full
 * of text. The page indicator shows the current scroll progress in percent.
 */
class OverlayContentController(
    private val root: View,
    private val speedProvider: () -> Float
) {

    private val textView: TextView = root.findViewById(R.id.overlay_text)
    private val viewport: View = root.findViewById(R.id.overlay_viewport)
    private val pageIndicator: TextView = root.findViewById(R.id.overlay_page_indicator)

    val engine: AutoScrollEngine = AutoScrollEngine(textView, viewport, speedProvider)

    init {
        engine.onProgress = { progress ->
            pageIndicator.text = "${(progress * 100).toInt()}%"
        }
    }

    /** Replace the content and reset scroll to the top. */
    fun setContent(content: String) {
        textView.text = content
        engine.reset()
        pageIndicator.text = "0%"
    }

    /** Jump one viewport full of text further down. */
    fun next() {
        engine.stop(reportEnd = false)
        val max = engine.maxScrollOffset().toInt()
        val target = (viewport.scrollY + viewport.height).coerceAtMost(max)
        viewport.scrollTo(0, target)
        updateIndicator()
    }

    /** Jump one viewport full of text back up. */
    fun previous() {
        engine.stop(reportEnd = false)
        val target = (viewport.scrollY - viewport.height).coerceAtLeast(0)
        viewport.scrollTo(0, target)
        updateIndicator()
    }

    private fun updateIndicator() {
        val max = engine.maxScrollOffset()
        val progress = if (max > 0f) {
            (viewport.scrollY / max).coerceIn(0f, 1f)
        } else {
            0f
        }
        pageIndicator.text = "${(progress * 100).toInt()}%"
    }

    fun setFontSize(size: Float) {
        textView.textSize = size
    }

    fun setTextColor(color: Int) {
        textView.setTextColor(color)
    }

    fun setBackgroundColor(color: Int) {
        root.setBackgroundColor(color)
    }

    fun release() {
        engine.release()
    }
}