package proto.actor

import mu.KotlinLogging
import proto.actor.messages.PID
import kotlin.time.Duration


class AllForOneStrategy(
    private val decider: (PID, Exception) -> SupervisorDirective,
    private val maxNrOfRetries: Int,
    private val withinTimeSpan: Duration?
) : SupervisorStrategy {

    override fun handleFailure(
        supervisor: Supervisor,
        child: PID,
        statistics: RestartStatistics,
        reason: Exception,
        message: Any
    ) {
        val directive: SupervisorDirective = decider(child, reason)

        when (directive) {
            SupervisorDirective.Resume -> {
                logger.debug { "[Supervision] Resuming $child Reason $reason" }
                supervisor.resumeChildren(child)
            }

            SupervisorDirective.Restart -> {
                when {
                    shouldStop(statistics) -> {
                        logger.debug { "Stopping $child Reason $reason" }
                        supervisor.stopChildren(*supervisor.children.toTypedArray())

                    }

                    else -> {
                        logger.debug { "Restarting $child Reason $reason" }
                        supervisor.restartChildren(reason, *supervisor.children.toTypedArray())
                    }
                }
            }

            SupervisorDirective.Stop -> {
                logger.debug { "Stopping $child Reason $reason" }
                supervisor.stopChildren(*supervisor.children.toTypedArray())
            }

            SupervisorDirective.Escalate -> {
                supervisor.escalateFailure(reason, message)
            }
        }
    }

    private fun shouldStop(statistics: RestartStatistics): Boolean {
        if (maxNrOfRetries == 0) {
            return true
        }

        statistics.fail()

        if (statistics.numberOfFailures(withinTimeSpan) > maxNrOfRetries) {
            statistics.reset()
            return true
        }

        return false
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
