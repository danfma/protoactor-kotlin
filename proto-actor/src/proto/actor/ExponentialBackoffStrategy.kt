package proto.actor


import kotlinx.coroutines.delay
import proto.actor.messages.PID
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

class ExponentialBackoffStrategy(
    private val backoffWindow: Duration,
    private val initialBackoff: Duration
) : SupervisorStrategy {
    private val random: Random = Random()

    override fun handleFailure(
        supervisor: Supervisor,
        child: PID,
        statistics: RestartStatistics,
        reason: Exception,
        message: Any
    ) {
        if (statistics.numberOfFailures(backoffWindow) == 0) {
            statistics.reset()
        }

        statistics.fail()

        val backoff = statistics.failureCount * initialBackoff.toInt(DurationUnit.MILLISECONDS)
        val noise = random.nextInt(500)
        val duration = (backoff + noise).milliseconds

        Dispatcher.default.schedule {
            delay(duration)
            supervisor.restartChildren(reason, child)
        }
    }
}
