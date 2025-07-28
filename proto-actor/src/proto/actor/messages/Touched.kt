package proto.actor.messages

import kotlinx.serialization.Serializable

@Serializable
data class Touched(
    val who: PID
)
