package proto.actor.processes

import proto.actor.ActorSystem
import proto.actor.Process
import proto.actor.mailbox.Mailbox
import proto.actor.messages.PID
import proto.actor.messages.SystemMessage

class ActorProcess(system: ActorSystem, private val mailbox: Mailbox) : Process(system) {
    internal var isDead: Boolean = false
        private set

    override fun sendUserMessage(pid: PID, message: Any) {
        mailbox.postUserMessage(message)
    }

    override fun sendSystemMessage(pid: PID, message: SystemMessage) {
        mailbox.postSystemMessage(message)
    }

    override fun stop(pid: PID) {
        super.stop(pid)
        isDead = true
    }
}
