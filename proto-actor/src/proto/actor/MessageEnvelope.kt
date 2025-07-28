package proto.actor

import proto.actor.messages.PID

data class MessageEnvelope(
    val message: Any,
    val sender: PID?,
    val header: MessageHeader? = null
) {
    fun getHeader(key: String, default: String): String = when (header) {
        null -> default
        else -> header.getOrDefault(key, default)
    }

    fun setHeader(key: String, value: String): MessageEnvelope {
        return copy(header = ensureHeader().put(key, value))
    }

    private fun ensureHeader(): MessageHeader {
        return header ?: MessageHeader.empty
    }

    companion object {
        fun unwrapSender(message: Any) =
            (message as? MessageEnvelope)?.sender

        fun unwrapMessage(message: Any) =
            if (message is MessageEnvelope) message.message else message

        fun unwrapHeaders(message: Any) =
            (message as? MessageEnvelope)?.header ?: MessageHeader.empty

        fun wrap(message: Any) =
            when (message) {
                is MessageEnvelope -> message
                else -> MessageEnvelope(message, sender = null, header = null)
            }

        fun withSender(message: Any, sender: PID) =
            when (message) {
                is MessageEnvelope -> message.copy(sender = sender)
                else -> MessageEnvelope(message, sender, header = null)
            }
    }
}

