package proto.actor.messages

import kotlinx.serialization.Serializable

@Serializable
data class Unwatch(
    val watcher: PID
) : SystemMessage
