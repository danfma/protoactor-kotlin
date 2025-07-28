package proto.actor.mailbox

import proto.actor.messages.SystemMessage

interface MessageInvoker {
    suspend fun invokeSystemMessage(message: SystemMessage)
    suspend fun invokeUserMessage(message: Any)
    fun escalateFailure(reason: Exception, message: Any)
    fun cancelInvoke()
}
