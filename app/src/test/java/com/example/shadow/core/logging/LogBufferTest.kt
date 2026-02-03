package com.example.shadow.core.logging

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class LogBufferTest {
    @Test
    fun addRespectsMaxEntries() {
        val buffer = LogBuffer(maxEntries = 2)

        buffer.add("First")
        buffer.add("Second")
        buffer.add("Third")

        val entries = buffer.entries.value
        assertEquals(2, entries.size)
        assertEquals("Second", entries[0].message)
        assertEquals("Third", entries[1].message)
    }

    @Test
    fun formatIncludesMessage() {
        val entry = LogEntry(Instant.parse("2024-05-01T10:15:30Z"), "Hello")

        val formatted = entry.format()

        assertEquals("2024-05-01 10:15:30 • Hello", formatted)
    }
}
