package com.wyprompter.core

import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Extracts plain text from TXT and Word (.docx) content.
 */
object TextImporter {

    fun fromTxt(bytes: ByteArray, encoding: String = "UTF-8"): String {
        val charset = runCatching { java.nio.charset.Charset.forName(encoding) }
            .getOrElse { Charsets.UTF_8 }
        return bytes.toString(charset)
    }

    /** Parse a .docx file into paragraph text. */
    fun fromDocx(bytes: ByteArray): String {
        return parseDocx(ByteArrayInputStream(bytes))
    }

    private fun parseDocx(input: InputStream): String {
        val doc = org.apache.poi.xwpf.usermodel.XWPFDocument(input)
        try {
            val sb = StringBuilder()
            val paragraphs = doc.paragraphs
            for (i in paragraphs.indices) {
                sb.append(paragraphs[i].text)
                if (i < paragraphs.size - 1) sb.append('\n')
            }
            val tables = doc.tables
            for (table in tables) {
                for (row in table.rows) {
                    for (cell in row.tableCells) {
                        sb.append(cell.text).append('\n')
                    }
                }
            }
            return sb.toString()
        } finally {
            doc.close()
        }
    }
}