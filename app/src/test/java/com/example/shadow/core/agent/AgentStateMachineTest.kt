package com.example.shadow.core.agent

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentStateMachineTest {
    @Test
    fun transitionMovesToAllowedStateAndPersists() {
        val repository = mockk<AgentRepository>(relaxed = true)
        every { repository.loadState() } returns AgentState.INIT

        val machine = AgentStateMachine(repository)

        val transitioned = machine.transition(AgentState.REGISTERED)

        assertTrue(transitioned)
        assertEquals(AgentState.REGISTERED, machine.currentState)
        verify { repository.saveState(AgentState.REGISTERED) }
    }

    @Test
    fun transitionRejectsDisallowedState() {
        val repository = mockk<AgentRepository>(relaxed = true)
        every { repository.loadState() } returns AgentState.INIT

        val machine = AgentStateMachine(repository)

        val transitioned = machine.transition(AgentState.AUTHORIZED)

        assertFalse(transitioned)
        assertEquals(AgentState.INIT, machine.currentState)
        verify(exactly = 0) { repository.saveState(any()) }
    }
}
