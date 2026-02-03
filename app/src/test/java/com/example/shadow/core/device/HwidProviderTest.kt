package com.example.shadow.core.device

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HwidProviderTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("shadow_agent", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun getOrCreateReturnsSameValueAfterFirstCall() {
        val provider = HwidProvider(context)

        val first = provider.getOrCreate()
        val second = provider.getOrCreate()

        assertTrue(first.isNotBlank())
        assertEquals(first, second)
    }
}
