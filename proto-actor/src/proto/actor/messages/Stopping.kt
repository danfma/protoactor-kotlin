package proto.actor.messages

import kotlinx.serialization.Serializable

@Serializable
data object Stopping : AutoReceiveMessage
