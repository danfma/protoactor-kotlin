package proto.actor.processes

import proto.actor.messages.PID

data class DeadLetterEvent(val pid: PID, val message: Any, val sender: PID?)

