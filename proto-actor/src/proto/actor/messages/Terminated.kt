package proto.actor.messages

import kotlinx.serialization.Serializable

@Serializable
data class Terminated(
    val who: PID,
    val why: TerminatedReason
) : SystemMessage
