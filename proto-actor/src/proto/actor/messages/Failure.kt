package proto.actor.messages

import proto.actor.RestartStatistics

data class Failure(
    val who: PID,
    val reason: Exception,
    val restartStatistics: RestartStatistics,
    val message: Any
) : SystemMessage
