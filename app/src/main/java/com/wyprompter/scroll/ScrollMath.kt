package com.wyprompter.scroll

import kotlin.math.max

/**
 * Pure scroll math, independent of Android, so it can be unit tested.
 *
 * A manuscript paragraph is laid out inside a viewport. Because the paragraph
 * height may exceed the viewport, the content must be translated vertically so
 * the reader sees each line. This class computes how far the content must move
 * and how long it should take given a speed setting.
 *
 * @param scrollUp when true content scrolls bottom-to-top reading direction
 *   is reversed (text moves up), otherwise the teleprompter style moves text up.
 */
object ScrollMath {

    /**
     * @param contentHeight measured height of the paragraph text (px)
     * @param viewportHeight visible height of the scroll area (px)
     * @param speedPxPerSecond how many pixels the content moves per second
     * @return maximum vertical offset the content can scroll to, in px
     */
    fun maxScrollOffset(contentHeight: Int, viewportHeight: Int): Float {
        return max(0f, (contentHeight - viewportHeight).toFloat())
    }

    /**
     * Total animation duration in milliseconds to move from 0 to [maxOffset]
     * at the given speed.
     */
    fun durationMs(maxOffset: Float, speedPxPerSecond: Float): Long {
        if (maxOffset <= 0f || speedPxPerSecond <= 0f) return 0L
        return (maxOffset / speedPxPerSecond * 1000f).toLong()
    }

    /** Fraction of the content already visible given current translation. */
    fun progress(currentOffset: Float, maxOffset: Float): Float {
        if (maxOffset <= 0f) return 1f
        return (currentOffset / maxOffset).coerceIn(0f, 1f)
    }
}