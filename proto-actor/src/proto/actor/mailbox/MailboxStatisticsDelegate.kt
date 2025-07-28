package proto.actor.mailbox

class MailboxStatisticsDelegate(vararg stats: MailboxStatistics) : MailboxStatistics {
    private var stats = stats.toList()

    fun add(stats: MailboxStatistics) {
        this.stats = this.stats + stats
    }

    fun remove(stats: MailboxStatistics) {
        this.stats = this.stats - stats
    }

    override fun mailboxStarted() {
        stats.forEach { it.mailboxStarted() }
    }

    override fun messagePosted(message: Any) {
        stats.forEach { it.messagePosted(message) }
    }

    override fun messageReceived(message: Any) {
        stats.forEach { it.messageReceived(message) }
    }

    override fun mailboxEmpty() {
        stats.forEach { it.mailboxEmpty() }
    }

    override fun messageDropped(msg: Any) {
        stats.forEach { it.messageDropped(msg) }
    }
}
