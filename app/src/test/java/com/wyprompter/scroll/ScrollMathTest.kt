package com.wyprompter.scroll

import org.junit.Assert.assertEquals
import org.junit.Test

class ScrollMathTest {

    @Test
    fun `no offset when content fits`() {
        assertEquals(0f, ScrollMath.maxScrollOffset(100, 200), 0.01f)
    }

    @Test
    fun `offset equals difference when content taller`() {
        assertEquals(200f, ScrollMath.maxScrollOffset(400, 200), 0.01f)
    }

    @Test
    fun `duration proportional to distance and inverse speed`() {
        // 200px at 100px/s => 2000ms
        assertEquals(2000L, ScrollMath.durationMs(200f, 100f))
    }

    @Test
    fun `duration zero when no offset`() {
        assertEquals(0L, ScrollMath.durationMs(0f, 100f))
        assertEquals(0L, ScrollMath.durationMs(50f, 0f))
    }

    @Test
    fun `progress clamps to range`() {
        assertEquals(0f, ScrollMath.progress(0f, 100f), 0.01f)
        assertEquals(0.5f, ScrollMath.progress(50f, 100f), 0.01f)
        assertEquals(1f, ScrollMath.progress(150f, 100f), 0.01f)
    }
}