package proto.actor

import proto.actor.messages.PID

interface StopperContext {
    fun stop(pid: PID)
    suspend fun waitStopOf(pid: PID)

    fun poison(pid: PID)
    suspend fun waitPoisonOf(pid: PID)
}
