package proto.actor.mailbox

import proto.actor.fixture.ExceptionalMessage
import proto.actor.fixture.ExceptionalSystemMessage
import proto.actor.fixture.TestMailboxHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

open class EscalateFailureTests {
    private inline fun <reified T> Iterable<Exception>.singleExceptionOf(): T {
        return this.filterIsInstance<T>()
                .single()
    }

    class MessageHandlerTestException : Exception("Handler Exception")

    @Test
    fun `Should escalate failure when a User message is completed exceptionally`() {
        val mailboxHandler = TestMailboxHandler()
        val mailbox = newUnboundedMailbox()
        mailbox.registerHandlers(mailboxHandler, mailboxHandler)
        val taskException = MessageHandlerTestException()
        val message = ExceptionalMessage(taskException)

        mailbox.postUserMessage(message)

        val escalatedFailure = mailboxHandler.escalatedFailures
                .singleExceptionOf<MessageHandlerTestException>()
        assertEquals(taskException, escalatedFailure)
    }

    @Test
    fun `Should escalate failure when a System message is completed exceptionally`() {
        val mailboxHandler = TestMailboxHandler()
        val mailbox = newUnboundedMailbox()
        mailbox.registerHandlers(mailboxHandler, mailboxHandler)
        val taskException = MessageHandlerTestException()
        val systemMessage = ExceptionalSystemMessage(taskException)

        mailbox.postSystemMessage(systemMessage)

        val escalatedFailure = mailboxHandler.escalatedFailures
                .singleExceptionOf<MessageHandlerTestException>()
        assertEquals(taskException, escalatedFailure)
    }

    @Test
    fun `Should escalate failure when waiting for a User message to be completed exceptionally`() {
        val mailboxHandler = TestMailboxHandler()
        val mailbox = newUnboundedMailbox()
        mailbox.registerHandlers(mailboxHandler, mailboxHandler)
        val taskException = MessageHandlerTestException()

        val message = ExceptionalMessage(taskException)
        mailbox.postUserMessage(message)

        val escalatedFailures = mailboxHandler.escalatedFailures
        val escalatedFailure = escalatedFailures
                .singleExceptionOf<MessageHandlerTestException>()
        assertEquals(taskException, escalatedFailure)
    }

    @Test
    fun `Should escalate failure when waiting for a System message to be completed exceptionally`() {
        val mailboxHandler = TestMailboxHandler()
        val mailbox = newUnboundedMailbox()
        mailbox.registerHandlers(mailboxHandler, mailboxHandler)
        val taskException = MessageHandlerTestException()
        val systemMessage = ExceptionalSystemMessage(taskException)

        mailbox.postSystemMessage(systemMessage)

        val escalatedFailure = mailboxHandler.escalatedFailures
                .singleExceptionOf<MessageHandlerTestException>()
        assertEquals(taskException, escalatedFailure)
    }
}
