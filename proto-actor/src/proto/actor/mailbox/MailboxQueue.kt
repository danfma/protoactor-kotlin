package proto.actor.mailbox

interface MailboxQueue<T> {
    val isEmpty: Boolean
    val isNotEmpty get() = !isEmpty
    val size: Int

    fun push(message: T)
    fun pop(): T?
}
