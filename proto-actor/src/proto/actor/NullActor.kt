package proto.actor

internal object NullActor : Actor {
    override suspend fun Context.receive(message: Any) {
        // do nothing
    }
}
