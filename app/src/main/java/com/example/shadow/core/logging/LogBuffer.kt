package com.example.shadow.core.logging

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LogEntry(
    val timestamp: Instant,
    val message: String,
) {
    fun format(): String {
        return "${FORMATTER.format(timestamp)} • $message"
    }

    companion object {
        private val FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withLocale(Locale.US)
            .withZone(ZoneId.systemDefault())
    }
}

class LogBuffer(private val maxEntries: Int = 500) {
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    fun add(message: String) {
        val updated = (_entries.value + LogEntry(Instant.now(), message)).takeLast(maxEntries)
        _entries.value = updated
    }
}
