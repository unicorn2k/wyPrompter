package com.wyprompter.scroll

import android.animation.ValueAnimator
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * Drives vertical auto-scroll of a [ScrollView] hosted widget using
 * [ValueAnimator].
 *
 * The viewport is a vertical `ScrollView` whose child (the paragraph text) may
 * be taller than the viewport. This engine animates `scrollY` from 0 down to the
 * maximum scroll offset so the reader sees each line. Scrolling the ScrollView
 * directly (instead of manually translating the child) is robust: the scroll
 * range only becomes positive when the content actually overflows the viewport.
 *
 * Duration is derived from a speed provider (px per second) so it adapts to
 * content height and speed setting.
 */
class AutoScrollEngine(
    private val contentView: View,
    private val scrollableView: View,
    private val speedProvider: () -> Float
) {

    private var animator: ValueAnimator? = null
    private var maxOffset: Float = 0f
    private var layoutRetries = 0

    val isRunning: Boolean
        get() = animator?.isRunning == true

    var onScrollEnd: (() -> Unit)? = null

    /** Progress callback (0..1) fired on every animation frame. */
    var onProgress: ((Float) -> Unit)? = null

    /** Current scrollable distance = content height - viewport height. */
    fun maxScrollOffset(): Float {
        return computeMaxOffset()
    }

    /** Full scrollable distance = content height - viewport height. */
    private fun computeMaxOffset(): Float {
        return ScrollMath.maxScrollOffset(contentView.height, scrollableView.height)
    }

    /**
     * Start scrolling the current content toward the bottom.
     *
     * Resumes from the current scroll position so a paused/restarted paragraph
     * does not jump back to the top. If the view is not laid out yet (height
     * still 0, e.g. right after the window is added), this retries on the next
     * few layout passes instead of silently doing nothing.
     */
    fun start() {
        if (scrollableView.height <= 0 || contentView.height <= 0) {
            // `View.post` queues the runnable until the view is attached and a
            // layout pass has run, so no isAttachedToWindow gate is needed here
            // (that would be false right after addView and never recover).
            if (layoutRetries < MAX_LAYOUT_RETRIES) {
                layoutRetries++
                scrollableView.post { start() }
            } else {
                Log.d(TAG, "scroll start gave up: content never measured")
                onScrollEnd?.invoke()
            }
            return
        }
        layoutRetries = 0
        stop(reportEnd = false)
        maxOffset = computeMaxOffset()
        if (maxOffset <= 0f) {
            Log.d(TAG, "scroll start skipped: no overflow (content=${contentView.height}, viewport=${scrollableView.height})")
            onScrollEnd?.invoke()
            return
        }
        var from = scrollableView.scrollY.toFloat().coerceIn(0f, maxOffset)
        if (from >= maxOffset) {
            // Already at the bottom: restart the paragraph instead of stalling.
            from = 0f
            scrollableView.scrollTo(0, 0)
        }
        val duration = ScrollMath.durationMs(maxOffset - from, speedProvider()).coerceAtLeast(1L)
        Log.d(TAG, "scroll start content=${contentView.height} viewport=${scrollableView.height} max=$maxOffset from=$from duration=$duration")
        val animator = ValueAnimator.ofFloat(from, maxOffset)
        animator.duration = duration
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { value ->
            val offset = (value.animatedValue as Float).toInt()
            scrollableView.scrollTo(0, offset)
            onProgress?.invoke((offset.toFloat() / maxOffset).coerceIn(0f, 1f))
        }
        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                if (animation === this@AutoScrollEngine.animator) {
                    onScrollEnd?.invoke()
                }
            }
        })
        this.animator = animator
        animator.start()
    }

    fun pause() {
        animator?.pause()
    }

    fun resume() {
        animator?.resume()
    }

    /** Stop and leave the content where it is. */
    fun stop(reportEnd: Boolean = false) {
        animator?.let {
            it.removeAllUpdateListeners()
            it.removeAllListeners()
            it.cancel()
        }
        animator = null
        if (reportEnd) onScrollEnd?.invoke()
    }

    /** Stop and reset scroll to 0 (top of current paragraph). */
    fun reset() {
        stop(reportEnd = false)
        scrollableView.scrollTo(0, 0)
    }

    fun release() {
        stop(reportEnd = false)
        scrollableView.scrollTo(0, 0)
    }

    companion object {
        private const val MAX_LAYOUT_RETRIES = 20
        private const val TAG = "wyScroll"
    }
}