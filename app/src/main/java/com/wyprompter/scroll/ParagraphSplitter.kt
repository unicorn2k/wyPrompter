package com.wyprompter.scroll

/**
 * Splits a manuscript into paragraphs. A paragraph is delimited by one or more
 * blank lines, or by a split marker line.
 */
object ParagraphSplitter {

    /** Splits text into non-empty paragraphs trimmed of surrounding whitespace. */
    fun split(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        return text
            .trim()
            .split(Regex("\\n\\s*\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun hasMultipleParagraphs(text: String): Boolean = split(text).size > 1
}