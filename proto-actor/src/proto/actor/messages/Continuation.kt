package proto.actor.messages

import proto.actor.Actor

data class Continuation(
    val action: suspend () -> Unit,
    val message: Any,
    val actor: Actor
) : SystemMessage {
    companion object {
        fun create(
            message: Any,
            actor: Actor,
            action: suspend () -> Unit
        ) = Continuation(action, message, actor)
    }
}
