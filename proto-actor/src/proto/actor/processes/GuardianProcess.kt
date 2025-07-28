package proto.actor.processes

import proto.actor.ActorSystem
import proto.actor.Process
import proto.actor.Supervisor
import proto.actor.SupervisorStrategy
import proto.actor.exceptions.GuardianException
import proto.actor.messages.*

class GuardianProcess(
    system: ActorSystem,
    private val strategy: SupervisorStrategy
) : Process(system), Supervisor {

    val pid = system.processRegistry.add(name = nextGuardianName(system), process = this)

    override val children: List<PID>
        get() = throw GuardianException("Guardian does not hold its children PIDs")

    override fun sendUserMessage(pid: PID, message: Any) {
        throw GuardianException("Guardian cannot receive any user message")
    }

    override fun sendSystemMessage(pid: PID, message: SystemMessage) {
        if (message is Failure) {
            strategy.handleFailure(
                supervisor = this,
                child = message.who,
                statistics = message.restartStatistics,
                reason = message.reason,
                message = message.message
            )
        }
    }

    override fun escalateFailure(reason: Exception, message: Any) {
        throw GuardianException("Guardian cannot escalate failures")
    }

    override fun restartChildren(reason: Exception, vararg targets: PID) {
        system.sendAllSystemMessage(Restart(reason), *targets)
    }

    override fun stopChildren(vararg targets: PID) {
        targets.forEach {
            system.root.stop(it)
        }
    }

    override fun resumeChildren(vararg targets: PID) {
        system.sendAllSystemMessage(ResumeMailbox, *targets)
    }

    companion object {
        private fun nextGuardianName(system: ActorSystem): String {
            return $$"$guardian-$${system.processRegistry.nextId()}"
        }
    }
}
