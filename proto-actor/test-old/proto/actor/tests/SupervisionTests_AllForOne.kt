package proto.actor.tests

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import proto.actor.*
import proto.actor.Failure
import proto.actor.PID
import proto.actor.Restart
import proto.actor.Started
import proto.actor.StopInstance
import proto.actor.fixture.TestMailboxStatistics
import proto.actor.mailbox.newUnboundedMailbox
import proto.actor.messages.ResumeMailbox
import java.util.concurrent.TimeUnit

class SupervisionTests_AllForOne {
    companion object {
        private val Exception: Exception = Exception("boo hoo")
    }

    class ParentActor(child1Props: Props, child2Props: Props) : Actor {
        private val _child1Props: Props = child1Props
        private val _child2Props: Props = child2Props
        lateinit var child1: PID
        lateinit var child2: PID

        suspend override fun Context.receive(message: Any) {
            if (message is Started) {
                child1 = spawnChild(_child1Props)
                child2 = spawnChild(_child2Props)
            }
            if (message is String) {
                send(child1, this@receive.message)
            }
        }

    }

    class ChildActor : Actor {
        suspend override fun Context.receive(message: Any) {
            val tmp = message
            when (tmp) {
                is String -> {
                    throw Exception
                }
            }

        }
    }

    @Test
    fun `AllForOneStrategy should resume child on failure`() {
        val child1MailboxStats: TestMailboxStatistics = TestMailboxStatistics { it is ResumeMailbox }
        val child2MailboxStats: TestMailboxStatistics = TestMailboxStatistics { it is ResumeMailbox }
        val strategy: AllForOneStrategy = AllForOneStrategy({ _, _ -> SupervisorDirective.Resume }, 1, null)
        val child1Props: Props =
            fromProducer { ChildActor() }.withMailbox { newUnboundedMailbox(arrayOf(child1MailboxStats)) }
        val child2Props: Props =
            fromProducer { ChildActor() }.withMailbox { newUnboundedMailbox(arrayOf(child2MailboxStats)) }
        val parentProps: Props =
            fromProducer { ParentActor(child1Props, child2Props) }.withChildSupervisorStrategy(strategy)
        val parent: PID = spawn(parentProps)

        send(parent, "hello")
        child1MailboxStats.reset.await(1000L, TimeUnit.MILLISECONDS)

        assertTrue { child1MailboxStats.posted.contains(ResumeMailbox) }
        assertTrue { child1MailboxStats.received.contains(ResumeMailbox) }
        assertFalse { child2MailboxStats.posted.contains(ResumeMailbox) }
        assertFalse { child2MailboxStats.received.contains(ResumeMailbox) }
    }

    @Test
    fun `AllForOneStrategy should stop all children on failure`() {
        val child1MailboxStats = TestMailboxStatistics { it is Stopped }
        val child2MailboxStats = TestMailboxStatistics { it is Stopped }
        val strategy = AllForOneStrategy({ _, _ -> SupervisorDirective.Stop }, 1, null)
        val child1Props: Props =
            fromProducer { ChildActor() }.withMailbox { newUnboundedMailbox(arrayOf(child1MailboxStats)) }
        val child2Props: Props =
            fromProducer { ChildActor() }.withMailbox { newUnboundedMailbox(arrayOf(child2MailboxStats)) }
        val parentProps: Props =
            fromProducer { ParentActor(child1Props, child2Props) }.withChildSupervisorStrategy(strategy)
        val parent: PID = spawn(parentProps)

        send(parent, "hello")
        child1MailboxStats.reset.await(1000L, TimeUnit.MILLISECONDS)
        child2MailboxStats.reset.await(1000L, TimeUnit.MILLISECONDS)

        assertTrue { child1MailboxStats.posted.contains(StopInstance) }
        assertTrue { child1MailboxStats.received.contains(StopInstance) }
        assertTrue { child2MailboxStats.posted.contains(StopInstance) }
        assertTrue { child2MailboxStats.received.contains(StopInstance) }
    }

    @Test
    fun `AllForOneStrategy should restart all children on failure`() {
        val child1MailboxStats = TestMailboxStatistics { it is Stopped }
        val child2MailboxStats = TestMailboxStatistics { it is Stopped }
        val strategy = AllForOneStrategy({ _, _ -> SupervisorDirective.Restart }, 1, null)
        val child1Props: Props =
            fromProducer { ChildActor() }.withMailbox { newUnboundedMailbox(arrayOf(child1MailboxStats)) }
        val child2Props: Props =
            fromProducer { ChildActor() }.withMailbox { newUnboundedMailbox(arrayOf(child2MailboxStats)) }
        val parentProps: Props =
            fromProducer { ParentActor(child1Props, child2Props) }.withChildSupervisorStrategy(strategy)
        val parent: PID = spawn(parentProps)

        send(parent, "hello")
        child1MailboxStats.reset.await(1000L, TimeUnit.MILLISECONDS)
        child2MailboxStats.reset.await(1000L, TimeUnit.MILLISECONDS)

        assertTrue { child1MailboxStats.posted.any { it is Restart } }
        assertTrue { child1MailboxStats.received.any { it is Restart } }
        assertTrue { child2MailboxStats.posted.any { it is Restart } }
        assertTrue { child2MailboxStats.received.any { it is Restart } }
    }

    @Test
    fun `AllForOneStrategy should pass exception on restart`() {
        val child1MailboxStats = TestMailboxStatistics { it is Stopped }
        val child2MailboxStats = TestMailboxStatistics { it is Stopped }
        val strategy = AllForOneStrategy({ _, _ -> SupervisorDirective.Restart }, 1, null)
        val child1Props: Props =
            fromProducer { ChildActor() }.withMailbox { newUnboundedMailbox(arrayOf(child1MailboxStats)) }
        val child2Props: Props =
            fromProducer { ChildActor() }.withMailbox { newUnboundedMailbox(arrayOf(child2MailboxStats)) }
        val parentProps: Props =
            fromProducer { ParentActor(child1Props, child2Props) }.withChildSupervisorStrategy(strategy)
        val parent: PID = spawn(parentProps)

        send(parent, "hello")
        child1MailboxStats.reset.await(1000L, TimeUnit.MILLISECONDS)
        child2MailboxStats.reset.await(1000L, TimeUnit.MILLISECONDS)

        assertTrue { child1MailboxStats.posted.any { msg -> (msg is Restart) && msg.reason == Exception } }
        assertTrue { child1MailboxStats.received.any { msg -> (msg is Restart) && msg.reason == Exception } }
        assertTrue { child2MailboxStats.posted.any { msg -> (msg is Restart) && msg.reason == Exception } }
        assertTrue { child2MailboxStats.received.any { msg -> (msg is Restart) && msg.reason == Exception } }
    }

    @Test
    fun `AllForOneStrategy should escalate failure to parent`() {
        val parentMailboxStats = TestMailboxStatistics { it is Stopped }
        val strategy = AllForOneStrategy({ _, _ -> SupervisorDirective.Escalate }, 1, null)
        val childProps: Props = fromProducer { ChildActor() }
        val parentProps: Props =
            fromProducer { ParentActor(childProps, childProps) }.withChildSupervisorStrategy(strategy)
                .withMailbox { newUnboundedMailbox(arrayOf(parentMailboxStats)) }
        val parent: PID = spawn(parentProps)

        send(parent, "hello")
        parentMailboxStats.reset.await(1000L, TimeUnit.MILLISECONDS)

        val failure = parentMailboxStats.received.filterIsInstance<Failure>().single()
        assertEquals("boo hoo", failure.reason.message)
    }
}
