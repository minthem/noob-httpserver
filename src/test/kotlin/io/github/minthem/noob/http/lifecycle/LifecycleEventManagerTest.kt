package io.github.minthem.noob.http.lifecycle

import io.github.minthem.noob.http.exception.LifecycleException
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class LifecycleEventManagerTest {

    private data class Event(val id: String, val event: String)

    private class LifecycleEventMock(private val id: String, private val event: MutableList<Event>) : LifecycleEvent {
        override fun onStart() {
            event.add(Event(id, "start"))
        }

        override fun onStop() {
            event.add(Event(id, "stop"))
        }
    }

    @Test
    fun `should start and stop after registration`() {
        val events = mutableListOf<Event>()
        val hook = LifecycleEventMock("test", events)
        val manager = LifecycleManager()
        manager.register(hook)
        manager.startAll()
        manager.stopAll()

        val expected = listOf(
            Event("test", "start"),
            Event("test", "stop"),
        )
        assertEquals(expected, events)
    }

    @Test
    fun `should start and stop multiple registered events`() {
        val events = mutableListOf<Event>()
        val hook1 = LifecycleEventMock("test1", events)
        val hook2 = LifecycleEventMock("test2", events)
        val hook3 = LifecycleEventMock("test3", events)
        val manager = LifecycleManager()
        manager.register(hook1)
        manager.register(hook2)
        manager.register(hook3)

        manager.startAll()
        manager.stopAll()

        val expected = listOf(
            Event("test1", "start"),
            Event("test2", "start"),
            Event("test3", "start"),
            Event("test3", "stop"),
            Event("test2", "stop"),
            Event("test1", "stop"),
        )
        assertEquals(expected, events)
    }

    @Test
    fun `should start and stop even when no events are registered`() {
        val events = mutableListOf<Event>()
        val manager = LifecycleManager()
        manager.startAll()
        manager.stopAll()

        val expected: List<Event> = emptyList()
        assertEquals(expected, events)
    }


    @Test
    fun `should start and stop after unregistering one of multiple registered events`() {
        val events = mutableListOf<Event>()
        val hook1 = LifecycleEventMock("test1", events)
        val hook2 = LifecycleEventMock("test2", events)
        val hook3 = LifecycleEventMock("test3", events)
        val manager = LifecycleManager()
        manager.register(hook1)
        val id2 = manager.register(hook2)
        manager.register(hook3)

        manager.unregister(id2)

        manager.startAll()
        manager.stopAll()

        val expected = listOf(
            Event("test1", "start"),
            Event("test3", "start"),
            Event("test3", "stop"),
            Event("test1", "stop"),
        )
        assertEquals(expected, events)
    }

    @Test
    fun `should start and stop after unregistering multiple events`() {
        val events = mutableListOf<Event>()
        val hook1 = LifecycleEventMock("test1", events)
        val hook2 = LifecycleEventMock("test2", events)
        val hook3 = LifecycleEventMock("test3", events)
        val hook4 = LifecycleEventMock("test4", events)
        val manager = LifecycleManager()
        manager.register(hook1)
        val id2 = manager.register(hook2)
        manager.register(hook3)
        val id4 = manager.register(hook4)

        manager.unregister(id2)
        manager.unregister(id4)

        manager.startAll()
        manager.stopAll()

        val expected = listOf(
            Event("test1", "start"),
            Event("test3", "start"),
            Event("test3", "stop"),
            Event("test1", "stop"),
        )
        assertEquals(expected, events)
    }

    @Test
    fun `should not fail when unregistering a non-existent id and should still start and stop`() {
        val events = mutableListOf<Event>()
        val hook = LifecycleEventMock("test", events)
        val manager = LifecycleManager()
        manager.register(hook)

        manager.unregister("non-existing-id")

        manager.startAll()
        manager.stopAll()

        val expected = listOf(
            Event("test", "start"),
            Event("test", "stop"),
        )
        assertEquals(expected, events)
    }

    @Test
    fun `should not allow start to be called twice`() {
        val events = mutableListOf<Event>()
        val hook = LifecycleEventMock("test", events)
        val manager = LifecycleManager()
        manager.register(hook)
        manager.startAll()

        val exp = assertThrows<IllegalStateException> {
            manager.startAll()
        }
        assertEquals("Cannot start hooks twice", exp.message)
    }

    @Test
    fun `should not allow stop to be called twice`() {
        val events = mutableListOf<Event>()
        val hook = LifecycleEventMock("test", events)
        val manager = LifecycleManager()
        manager.register(hook)
        manager.startAll()
        manager.stopAll()

        val exp = assertThrows<IllegalStateException> {
            manager.stopAll()
        }
        assertEquals("Cannot stop hooks twice", exp.message)
    }

    @Test
    fun `should not allow registration after start`() {
        val events = mutableListOf<Event>()
        val hook1 = LifecycleEventMock("test1", events)
        val hook2 = LifecycleEventMock("test2", events)
        val manager = LifecycleManager()
        manager.register(hook1)
        manager.startAll()

        val exp = assertThrows<IllegalStateException> {
            manager.register(hook2)
        }

        assertEquals("Cannot register hooks after start", exp.message)
    }

    @Test
    fun `should not allow unregistration after start`() {
        val events = mutableListOf<Event>()
        val hook1 = LifecycleEventMock("test1", events)
        val hook2 = LifecycleEventMock("test2", events)
        val manager = LifecycleManager()
        manager.register(hook1)
        val hookId2 = manager.register(hook2)
        manager.startAll()

        val exp = assertThrows<IllegalStateException> {
            manager.unregister(hookId2)
        }

        assertEquals("Cannot unregister hooks after start", exp.message)
    }

    @Test
    fun `should execute all events even when start fails`() {
        val events = mutableListOf<Event>()

        class LifecycleEventSideEffect(private val id: String, private val event: MutableList<Event>) : LifecycleEvent {
            override fun onStart() {
                throw RuntimeException("start error")
            }

            override fun onStop() {
                event.add(Event(id, "stop"))
            }
        }

        val hook1 = LifecycleEventMock("test1", events)
        val hook2 = LifecycleEventSideEffect("test2", events)
        val hook3 = LifecycleEventMock("test3", events)
        val manager = LifecycleManager()
        manager.register(hook1)
        manager.register(hook2)
        manager.register(hook3)

        assertThrows<LifecycleException> {
            manager.startAll()
        }

        assertDoesNotThrow { manager.stopAll() }

        val expected = listOf(
            Event("test1", "start"),
            Event("test3", "start"),
            Event("test3", "stop"),
            Event("test2", "stop"),
            Event("test1", "stop"),
        )
        assertEquals(expected, events)
    }

    @Test
    fun `should execute all events even when stop fails`() {
        val events = mutableListOf<Event>()

        class LifecycleEventSideEffect(private val id: String, private val event: MutableList<Event>) : LifecycleEvent {
            override fun onStart() {
                event.add(Event(id, "start"))
            }

            override fun onStop() {
                throw RuntimeException("stop error")
            }
        }

        val hook1 = LifecycleEventMock("test1", events)
        val hook2 = LifecycleEventSideEffect("test2", events)
        val hook3 = LifecycleEventMock("test3", events)
        val manager = LifecycleManager()
        manager.register(hook1)
        manager.register(hook2)
        manager.register(hook3)

        assertDoesNotThrow { manager.startAll() }

        assertThrows<LifecycleException> {
            manager.stopAll()
        }

        val expected = listOf(
            Event("test1", "start"),
            Event("test2", "start"),
            Event("test3", "start"),
            Event("test3", "stop"),
            Event("test1", "stop"),
        )
        assertEquals(expected, events)
    }
}
