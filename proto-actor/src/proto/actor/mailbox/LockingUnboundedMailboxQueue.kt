package proto.actor.mailbox

import java.util.concurrent.ConcurrentLinkedQueue

class LockingUnboundedMailboxQueue<T : Any> : MailboxQueue<T> {
    private val queue = ConcurrentLinkedQueue<T>()

    override val isEmpty: Boolean
        get() = size == 0

    override val size: Int
        get() = queue.size

    override fun push(message: T) {
        queue.offer(message)
    }

    override fun pop(): T? {
        return queue.poll()
    }
}
