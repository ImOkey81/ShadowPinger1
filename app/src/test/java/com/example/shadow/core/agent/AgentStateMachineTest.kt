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
    fun initialStateLoadsFromRepository() {
        val repository = mockk<AgentRepository>()
        every { repository.loadState() } returns AgentState.PERMISSIONS_GRANTED

        val machine = AgentStateMachine(repository)

        assertEquals(AgentState.PERMISSIONS_GRANTED, machine.currentState)
        verify { repository.loadState() }
    }

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

    @Test
    fun transitionFollowsFullHappyPath() {
        val repository = mockk<AgentRepository>(relaxed = true)
        every { repository.loadState() } returns AgentState.INIT

        val machine = AgentStateMachine(repository)

        val steps = listOf(
            AgentState.REGISTERED,
            AgentState.AUTHORIZED,
            AgentState.PERMISSIONS_GRANTED,
            AgentState.SIMS_MAPPED,
            AgentState.KAFKA_REGISTERED,
            AgentState.IDLE,
            AgentState.TESTING,
            AgentState.REPORTING,
            AgentState.IDLE,
        )

        steps.forEach { target ->
            assertTrue(machine.transition(target))
            assertEquals(target, machine.currentState)
        }
    }
}
