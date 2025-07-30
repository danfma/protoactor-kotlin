package proto.actor.mailbox

import proto.actor.Dispatcher
import proto.actor.MessageEnvelope
import proto.actor.NoOpDispatcher
import proto.actor.NoOpMessageInvoker
import proto.actor.messages.*
import proto.actor.messages.Nothing
import kotlin.concurrent.atomics.AtomicInt

class DefaultMailbox(
    val systemMessages: MailboxQueue<SystemMessage> = LockingUnboundedMailboxQueue(),
    val userMessages: MailboxQueue<Any> = LockingUnboundedMailboxQueue(),
    vararg statistics: MailboxStatistics
) : Mailbox {
    private var dispatcher: Dispatcher = NoOpDispatcher
    private var invoker: MessageInvoker = NoOpMessageInvoker
    private val atomicStatus = AtomicInt(MailboxStatus.IDLE.ordinal)
    private val statisticsDelegate = MailboxStatisticsDelegate(*statistics)
    private var suspended = false

    override val status: MailboxStatus
        get() = when (atomicStatus.load()) {
            MailboxStatus.IDLE.ordinal -> MailboxStatus.IDLE
            MailboxStatus.BUSY.ordinal -> MailboxStatus.BUSY
            else -> throw IllegalStateException("Unknown mailbox status")
        }

    override val pendingUserMessageCount: Int
        get() = userMessages.size

    override fun registerHandlers(invoker: MessageInvoker, dispatcher: Dispatcher) {
        this.invoker = invoker
        this.dispatcher = dispatcher
    }

    override fun postUserMessage(message: Any) {
        val batch = MessageEnvelope.unwrapMessage(message) as? MessageBatch

        if (batch != null) {
            val messages = batch.messages

            for (messageBatch in messages) {
                userMessages.push(messageBatch)
                statisticsDelegate.messagePosted(messageBatch)
            }

            if (batch is AutoRespond) {
                userMessages.push(batch)
            }

            statisticsDelegate.messagePosted(message)
            schedule()
            return
        }

        userMessages.push(message)
        statisticsDelegate.messagePosted(message)
        schedule()
    }

    override fun postSystemMessage(message: SystemMessage) {
        systemMessages.push(message)

        if (message is Stop) {
            invoker.cancelInvoke()
        }

        statisticsDelegate.messagePosted(message)
        schedule()
    }

    override fun start() {
        statisticsDelegate.mailboxStarted()
    }

    private fun schedule() {
        val previousStatus = atomicStatus.compareAndExchange(MailboxStatus.IDLE.ordinal, MailboxStatus.BUSY.ordinal)

        if (previousStatus != MailboxStatus.IDLE.ordinal) {
            return
        }

        dispatcher.schedule {
            processMessages()
        }
    }

    private suspend fun processMessages() {
        var message: Any = Nothing

        try {
            for (i in 0 until dispatcher.throughput) {
                message = systemMessages.pop() ?: Nothing

                if (message is SystemMessage) {
                    suspended = when (message) {
                        is SuspendMailbox -> true
                        is ResumeMailbox -> false
                        else -> suspended
                    }

                    if (!tryAwait(message)) {
                        return
                    }

                    statisticsDelegate.messageReceived(message)
                    continue
                }

                if (suspended) {
                    break
                }

                message = userMessages.pop() ?: Nothing

                if (message != Nothing) {
                    if (!tryAwait(message)) {
                        return
                    }

                    statisticsDelegate.messageReceived(message)
                } else {
                    break
                }
            }
        } catch (e: Exception) {
            invoker.escalateFailure(e, message)
        } finally {
            atomicStatus.compareAndSet(MailboxStatus.BUSY.ordinal, MailboxStatus.IDLE.ordinal)
        }
    }

    private suspend fun tryAwait(message: Any): Boolean {
        return try {
            if (message is SystemMessage) {
                invoker.invokeSystemMessage(message)
            } else {
                invoker.invokeUserMessage(message)
            }

            statisticsDelegate.messageReceived(message)
            true
        } catch (e: Exception) {
            invoker.escalateFailure(e, message)
            false
        }
    }
}


