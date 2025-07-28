package proto.actor.fixture

import kotlinx.coroutines.runBlocking
import proto.actor.mailbox.Dispatcher
import proto.actor.mailbox.Mailbox
import proto.actor.mailbox.MessageInvoker
import proto.actor.messages.SystemMessage

class TestMailbox : Mailbox {
    private lateinit var _invoker: MessageInvoker
    private val userMessages: MutableList<Any> = mutableListOf()
    private val systemMessages: MutableList<Any> = mutableListOf()
    override fun postUserMessage(message: Any) {
        userMessages.add(message)
        runBlocking { _invoker.invokeUserMessage(message) }
    }

    override fun postSystemMessage(message: Any) {
        systemMessages.add(message)
        runBlocking { _invoker.invokeSystemMessage(message as SystemMessage) }
    }

    override fun registerHandlers(invoker: MessageInvoker, dispatcher: Dispatcher) {
        _invoker = invoker
    }

    override suspend fun run() {
    }

    override fun start() {}
}

