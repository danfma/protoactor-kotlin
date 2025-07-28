package proto.actor.exceptions

import proto.actor.messages.PID

class ProcessNameExistException(
    val name: String,
    val pid: PID? = null
) : Exception()

