package proto.actor

import proto.actor.messages.PID
import proto.actor.processes.GuardianProcess
import java.util.concurrent.ConcurrentHashMap

class Guardians(val system: ActorSystem) {
    private val guardians = ConcurrentHashMap<SupervisorStrategy, GuardianProcess>()

    internal fun getById(strategy: SupervisorStrategy): PID {
        val guardian = guardians.getOrPut(strategy) {
            GuardianProcess(system, strategy)
        }

        return guardian.pid
    }
}
