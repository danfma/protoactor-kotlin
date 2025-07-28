package proto.actor

import proto.actor.messages.PID

interface Supervisor {
    val children: List<PID>

    fun escalateFailure(reason: Exception, message: Any)
    fun restartChildren(reason: Exception, vararg targets: PID)
    fun stopChildren(vararg targets: PID)
    fun resumeChildren(vararg targets: PID)
}
