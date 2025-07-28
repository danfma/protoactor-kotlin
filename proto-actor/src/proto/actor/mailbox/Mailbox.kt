package proto.actor.mailbox

import proto.actor.Dispatcher
import proto.actor.messages.SystemMessage

interface Mailbox {
    val status: MailboxStatus
    val pendingUserMessageCount: Int

    fun registerHandlers(invoker: MessageInvoker, dispatcher: Dispatcher)
    fun postUserMessage(message: Any)
    fun postSystemMessage(message: SystemMessage)
    fun start()
}
