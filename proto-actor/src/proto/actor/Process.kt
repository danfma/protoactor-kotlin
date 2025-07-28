package proto.actor

import proto.actor.messages.PID
import proto.actor.messages.Stop
import proto.actor.messages.SystemMessage

abstract class Process(internal val system: ActorSystem) {
    abstract fun sendUserMessage(pid: PID, message: Any)
    abstract fun sendSystemMessage(pid: PID, message: SystemMessage)
    open fun stop(pid: PID) = sendSystemMessage(pid, Stop)
}

