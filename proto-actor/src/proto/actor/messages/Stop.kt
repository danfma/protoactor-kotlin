package proto.actor.messages

import kotlinx.serialization.Serializable

@Serializable
data object Stop : SystemMessage, IgnoreDeadLetterLogging

