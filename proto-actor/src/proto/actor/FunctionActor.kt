package proto.actor

class FunctionActor(val receiver: suspend Context.() -> Unit) : Actor {
    override suspend fun Context.receive(message: Any) = receiver()
}
