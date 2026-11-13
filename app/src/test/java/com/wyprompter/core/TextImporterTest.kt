package com.wyprompter.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TextImporterTest {

    @Test
    fun `utf8 text decodes correctly`() {
        val text = "第一段\n\n第二段"
        val bytes = text.toByteArray(Charsets.UTF_8)
        assertEquals(text, TextImporter.fromTxt(bytes, "UTF-8"))
    }

    @Test
    fun `gbk text decodes with gbk encoding`() {
        val text = "中文提词内容"
        val bytes = text.toByteArray(java.nio.charset.Charset.forName("GBK"))
        assertEquals(text, TextImporter.fromTxt(bytes, "GBK"))
    }

    @Test
    fun `unknown encoding falls back to utf8`() {
        val bytes = "hello world".toByteArray(Charsets.UTF_8)
        assertEquals("hello world", TextImporter.fromTxt(bytes, "NO_SUCH_ENCODING"))
    }
}