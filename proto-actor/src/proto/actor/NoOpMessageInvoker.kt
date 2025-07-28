package proto.actor

import proto.actor.mailbox.MessageInvoker
import proto.actor.messages.SystemMessage

object NoOpMessageInvoker : MessageInvoker {
    override suspend fun invokeSystemMessage(message: SystemMessage) {
        throw NotImplementedError()
    }

    override suspend fun invokeUserMessage(message: Any) {
        throw NotImplementedError()
    }

    override fun escalateFailure(reason: Exception, message: Any) {
        throw NotImplementedError()
    }

    override fun cancelInvoke() {
        throw NotImplementedError()
    }
}
