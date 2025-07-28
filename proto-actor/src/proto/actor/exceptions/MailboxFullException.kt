package proto.actor.exceptions

class MailboxFullException(val msg: Any) : Exception("Mailbox is full, cannot post message: $msg")
