package proto.actor

import proto.actor.messages.PID

interface SupervisorStrategy {
    fun handleFailure(
        supervisor: Supervisor,
        child: PID,
        statistics: RestartStatistics,
        reason: Exception,
        message: Any
    )
}
