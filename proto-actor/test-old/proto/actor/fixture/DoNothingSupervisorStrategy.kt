package proto.actor.fixture

import proto.actor.PID
import proto.actor.RestartStatistics
import proto.actor.Supervisor
import proto.actor.SupervisorStrategy

class DoNothingSupervisorStrategy : SupervisorStrategy {
    override fun handleFailure(supervisor: Supervisor, child: PID, rs: RestartStatistics, reason: Exception) {
    }
}

