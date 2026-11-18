package com.example

import com.example.data.system.FileSearchRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun quickMathEvaluatesValidExpressions() {
        assertEquals("14", FileSearchRepository.evaluateQuickMath("2 + 3 * 4"))
        assertEquals("156.6", FileSearchRepository.evaluateQuickMath("145 * 1.08"))
        assertEquals("25", FileSearchRepository.evaluateQuickMath("(10 + 15)"))
        assertNull(FileSearchRepository.evaluateQuickMath("notes"))
        assertNull(FileSearchRepository.evaluateQuickMath("10 / 0"))
    }

    @Test
    fun fileSizeFormattingWorks() {
        assertEquals("512 B", FileSearchRepository.formatFileSize(512))
        assertEquals("2 KB", FileSearchRepository.formatFileSize(2048))
        assertEquals("1.5 MB", FileSearchRepository.formatFileSize(1572864))
    }
}
