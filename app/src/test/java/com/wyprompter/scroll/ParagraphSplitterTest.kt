package com.wyprompter.scroll

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParagraphSplitterTest {

    @Test
    fun `blank text returns empty`() {
        assertTrue(ParagraphSplitter.split("   ").isEmpty())
        assertTrue(ParagraphSplitter.split("").isEmpty())
    }

    @Test
    fun `single paragraph`() {
        val result = ParagraphSplitter.split("仅一段话")
        assertEquals(1, result.size)
        assertEquals("仅一段话", result[0])
    }

    @Test
    fun `splits on blank lines`() {
        val text = "第一段\n\n第二段\n\n\n  第三段  "
        val result = ParagraphSplitter.split(text)
        assertEquals(listOf("第一段", "第二段", "第三段"), result)
    }

    @Test
    fun `trims each paragraph`() {
        val result = ParagraphSplitter.split("   abc  \n\n   def  ")
        assertEquals(listOf("abc", "def"), result)
    }

    @Test
    fun `hasMultipleParagraphs`() {
        assertTrue(ParagraphSplitter.hasMultipleParagraphs("a\n\nb"))
        assertTrue(!ParagraphSplitter.hasMultipleParagraphs("a"))
    }
}