package proto.actor

interface ReceiverContext : InfoContext {
    suspend fun receive(envelope: MessageEnvelope)
}
