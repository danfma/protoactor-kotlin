package proto.actor

import mu.KotlinLogging
import proto.actor.messages.PID
import kotlin.time.Duration

private val logger = KotlinLogging.logger {}

class OneForOneStrategy(
    private val decider: (PID, Exception) -> SupervisorDirective,
    private val maxNrOfRetries: Int,
    private val withinTimeSpan: Duration? = null
) : SupervisorStrategy {
    override fun handleFailure(
        supervisor: Supervisor,
        child: PID,
        statistics: RestartStatistics,
        reason: Exception,
        message: Any
    ) {
        val directive = decider(child, reason)

        when (directive) {
            SupervisorDirective.Resume -> {
                logger.debug { "Resuming $child Reason $reason" }
                supervisor.resumeChildren(child)
            }

            SupervisorDirective.Restart -> {
                when {
                    shouldStop(statistics) -> {
                        logger.debug { "Stopping $child Reason $reason" }
                        supervisor.stopChildren(child)
                    }

                    else -> {
                        logger.debug { "Restarting $child Reason $reason" }
                        supervisor.restartChildren(reason, child)
                    }
                }
            }

            SupervisorDirective.Stop -> {
                logger.debug { "Stopping $child Reason $reason" }
                supervisor.stopChildren(child)
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
        private val logger = KotlinLogging.logger { }
    }
}
