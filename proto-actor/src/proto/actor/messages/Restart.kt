package proto.actor.messages

data class Restart(val reason: Exception) : SystemMessage
