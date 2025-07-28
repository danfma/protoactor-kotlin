package proto.actor.messages

import kotlinx.serialization.Serializable

@Serializable
enum class TerminatedReason {
    Stopped,
    AddressTerminated,
    NotFound
}

