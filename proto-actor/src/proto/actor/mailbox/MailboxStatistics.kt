package proto.actor.mailbox

interface MailboxStatistics {
    fun mailboxStarted()
    fun messagePosted(message: Any)
    fun messageReceived(message: Any)
    fun mailboxEmpty()
    fun messageDropped(msg: Any)
}

