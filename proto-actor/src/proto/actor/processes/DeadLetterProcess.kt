package proto.actor.processes

import proto.actor.ActorSystem
import proto.actor.Dispatcher
import proto.actor.MessageEnvelope
import proto.actor.Process
import proto.actor.messages.PID
import proto.actor.messages.SystemMessage

class DeadLetterProcess(system: ActorSystem) : Process(system) {
    private val scope = Dispatcher.default

    override fun sendUserMessage(pid: PID, message: Any) {
        val event = when (message) {
            is MessageEnvelope -> DeadLetterEvent(pid, message.message, message.sender)
            else -> DeadLetterEvent(pid, message, null)
        }

        scope.schedule {
            system.eventStream.publish(event)
        }
    }

    override fun sendSystemMessage(pid: PID, message: SystemMessage) {
        scope.schedule {
            val event = DeadLetterEvent(pid, message, null)
            system.eventStream.publish(event)
        }
    }

    companion object {
        const val NAME = $$"$dead-letter"
    }
}
