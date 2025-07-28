package proto.actor.tests

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import proto.actor.Actor
import proto.actor.PID
import proto.actor.contexts.ActorContext
import proto.actor.fixture.DoNothingSupervisorStrategy
import java.time.Duration

class LocalContextTests {
    @Test
    fun `given context ctor should set required fields`() {
        val producer: () -> Actor = { NullActor }
        val supervisorStrategyMock = DoNothingSupervisorStrategy()
        val parent = PID("test", "test")
        val self = PID("abc", "def")
        val context = ActorContext(producer, self, supervisorStrategyMock, listOf(), listOf(), parent)
        assertEquals(parent, context.parent)
        assertNull(context.sender)
        assertNotNull(context.children)
        assertEquals(context.children, setOf<PID>())
        assertEquals(Duration.ZERO, context.getReceiveTimeout())
    }
}

