package proto.actor.messages

import kotlinx.serialization.Serializable

@Serializable
data class DeadLetterResponse(
    val target: PID
)

