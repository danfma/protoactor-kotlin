package proto.actor

interface Actor {
    suspend fun Context.receive(message: Any)
}

