package com.example.shadow.core.ping

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceholderIcmpPingEngineTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun pingReturnsUnreachableResult() = runTest {
        val engine = PlaceholderIcmpPingEngine()

        val result = engine.ping("192.0.2.1", timeoutMs = 1000, retries = 1)

        assertEquals("192.0.2.1", result.ip)
        assertFalse(result.isReachable)
        assertNull(result.timeMs)
    }
}
