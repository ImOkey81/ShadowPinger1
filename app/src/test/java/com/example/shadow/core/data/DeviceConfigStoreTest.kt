package com.example.shadow.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceConfigStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "datastore/device_config.preferences_pb").delete()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getOrCreateDeviceIdReturnsStableValue() = runTest {
        val store = DeviceConfigStore(context)

        val first = store.getOrCreateDeviceId()
        val second = store.getOrCreateDeviceId()

        assertTrue(first.isNotBlank())
        assertEquals(first, second)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun setDeviceTokenUpdatesFlow() = runTest {
        val store = DeviceConfigStore(context)

        store.setDeviceToken("token-123")

        assertEquals("token-123", store.deviceToken.first())
        store.setDeviceToken("token-456")
        assertNotEquals("token-123", store.deviceToken.first())
        assertEquals("token-456", store.deviceToken.first())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun deviceTokenStartsNull() = runTest {
        val store = DeviceConfigStore(context)

        assertEquals(null, store.deviceToken.first())
    }
}
