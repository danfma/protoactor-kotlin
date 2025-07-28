package proto.actor.tests

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import proto.actor.*
import proto.actor.PID
import proto.actor.Started
import proto.actor.fixture.DoNothingActor
import proto.actor.fixture.TestMailbox
import proto.actor.messages.Terminated
import java.time.Duration

class WatchTests {
    @Test
    fun `can watch local actors`() {
        runBlocking {
            val watchee: PID = spawn(fromProducer { DoNothingActor() }.withMailbox { TestMailbox() })
            val watcher: PID = spawn(fromProducer { LocalActor(watchee) }.withMailbox { TestMailbox() })
            stop(watchee)
            val terminatedMessageReceived: Boolean = requestAwait(watcher, "?", Duration.ofSeconds(5))
            assertTrue(terminatedMessageReceived)
        }
    }

    class LocalActor(watchee: PID) : Actor {
        private val _watchee: PID = watchee
        private var _terminateReceived: Boolean = false
        override suspend fun Context.receive(message: Any) {
            when (message) {
                is Started -> watch(_watchee)
                is String -> respond(_terminateReceived)
                is Terminated -> _terminateReceived = true
            }
        }
    }
}

