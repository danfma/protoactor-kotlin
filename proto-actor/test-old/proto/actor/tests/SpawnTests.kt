package proto.actor.tests

import proto.actor.PID
import proto.actor.Props
import proto.actor.fixture.EmptyReceive
import proto.actor.fromFunc
import proto.actor.spawn
import proto.actor.withSpawner
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class SpawnTests {
    @Test
    fun `given props with spawner spawn should return pid created by spawner`() {
        val spawnedPid = PID("test", "test")
        val props: Props = fromFunc(EmptyReceive).withSpawner { _, _, _ -> spawnedPid }
        val pid: PID = spawn(props)
        assertSame(spawnedPid, pid)
    }
}

