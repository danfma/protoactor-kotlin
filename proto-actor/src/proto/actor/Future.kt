package proto.actor

import kotlinx.coroutines.Deferred
import proto.actor.messages.PID

interface Future : AutoCloseable {
    val pid: PID
    val deferred: Deferred<Any>

    suspend fun getDeferred(): Any
}
