package proto.actor

import proto.actor.messages.PID

class AlwaysRestartStrategy : SupervisorStrategy {
    override fun handleFailure(
        supervisor: Supervisor,
        child: PID,
        statistics: RestartStatistics,
        reason: Exception,
        message: Any
    ) = supervisor.restartChildren(reason, child)
}
