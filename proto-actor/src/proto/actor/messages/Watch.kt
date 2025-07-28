package proto.actor.messages

import kotlinx.serialization.Serializable

@Serializable
data class Watch(
    val watcher: PID
) : SystemMessage
