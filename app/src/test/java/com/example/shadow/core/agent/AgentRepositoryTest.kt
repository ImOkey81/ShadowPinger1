package com.example.shadow.core.agent

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class AgentRepositoryTest {
    @Test
    fun loadStateFallsBackToInitOnInvalidStoredValue() {
        val context = mockk<Context>()
        val prefs = mockk<SharedPreferences>()
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.getString(any(), any()) } returns "NOT_A_STATE"

        val repository = AgentRepository(context)

        val state = repository.loadState()

        assertEquals(AgentState.INIT, state)
    }

    @Test
    fun loadStateReturnsStoredValue() {
        val context = mockk<Context>()
        val prefs = mockk<SharedPreferences>()
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.getString(any(), any()) } returns AgentState.AUTHORIZED.name

        val repository = AgentRepository(context)

        val state = repository.loadState()

        assertEquals(AgentState.AUTHORIZED, state)
    }

    @Test
    fun saveStatePersistsStateName() {
        val context = mockk<Context>()
        val prefs = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.edit() } returns editor

        val repository = AgentRepository(context)

        repository.saveState(AgentState.AUTHORIZED)

        verify { editor.putString(any(), AgentState.AUTHORIZED.name) }
        verify { editor.apply() }
    }
}
