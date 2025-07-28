package proto.actor.tests

import proto.actor.PID
import proto.actor.ProcessRegistry
import proto.actor.cachedProcess
import proto.actor.fixture.EmptyReceive
import proto.actor.fixture.TestMailbox
import proto.actor.fixture.TestProcess
import proto.actor.fromFunc
import proto.actor.spawn
import proto.actor.stop
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.util.*

class PIDTests {
    @Test
    fun `given actor not dead, cachedProcess should return it`() {
        val pid: PID = spawn(fromFunc(EmptyReceive))
        val p = pid.cachedProcess()
        assertNotNull(p)
    }

    @Test
    fun `given actor died, cachedProcess should not return it`() {
        val pid: PID = spawn(fromFunc(EmptyReceive).withMailbox { TestMailbox() })
        stop(pid)
        val p = pid.cachedProcess()
        assertNotNull(p)
    }

    @Test
    fun `given other process, cachedProcess should return it`() {
        val id = UUID.randomUUID().toString()
        val p = TestProcess()
        val pid = ProcessRegistry.put(id, p)
        val p2 = pid.cachedProcess()
        assertSame(p, p2)
    }
}

