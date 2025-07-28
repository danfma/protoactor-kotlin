package proto.actor

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

class RestartStatistics(
    failureCount: Int,
    lastFailureTime: Instant? = null,
    private val clock: Clock = Clock.System
) {
    private val failureTimes = mutableListOf<Instant>()

    init {
        for (i in 0 until failureCount) {
            failureTimes.add(lastFailureTime ?: clock.now())
        }
    }

    val failureCount: Int
        get() = failureTimes.size

    fun fail() {
        failureTimes.add(clock.now())
    }

    fun reset() {
        failureTimes.clear()
    }

    fun numberOfFailures(within: Duration?): Int {
        if (within == null) {
            return failureTimes.size
        }

        var result = 0
        val now = clock.now()

        for (failureTime in failureTimes) {
            if (now - failureTime < within) {
                result++
            }
        }

        return result
    }
}
