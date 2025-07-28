package proto.actor.fixture

import proto.actor.PID
import proto.actor.messages.SystemMessage

class TestProcess : actor.proto.Process() {
    override fun sendUserMessage(pid: PID, message: Any) {
    }

    override fun sendSystemMessage(pid: PID, message: SystemMessage) {
    }
}

