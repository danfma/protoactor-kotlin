package proto.actor

import kotlinx.coroutines.Deferred
import proto.actor.messages.PID
import kotlin.time.Duration

interface Context : SenderContext, ReceiverContext, SpawnerContext, StopperContext {
    val receiveTimeout: Duration
    val children: List<PID>

    /**
     * Sends a response to the current Sender. If the Sender is null,
     * this call has no effect apart from warning log entry.
     */
    fun respond(message: Any)

    /**
     * Sends a response to the current Sender. If the Sender is null, this call
     * has no effect apart from warning log entry.
     */
    fun respond(message: Any, header: MessageHeader) {
        respond(MessageEnvelope(message, null, header))
    }

    fun watch(pid: PID)

    fun unwatch(pid: PID)

    fun setReceiveTimeout(duration: Duration)

    fun cancelReceiveTimeout()

    fun forward(target: PID)

    fun <T> reenterAfter(deferred: Deferred<T>, action: suspend Context.(Deferred<T>) -> Unit)
}

