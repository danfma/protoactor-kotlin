package proto.actor

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.DurationUnit

class PeriodicTimer(
    private val dispatcher: Dispatcher,
    private val tick: Duration,
    private val callback: () -> Unit,
) : AutoCloseable {
    private var job: Job? = null

    fun start() {
        job = dispatcher.defer {
            while (true) {
                delay(tick.toLong(DurationUnit.MILLISECONDS))
                callback()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun reset() {
        stop()
        start()
    }

    override fun close() {
        stop()
    }

    companion object {
        fun create(
            dispatcher: Dispatcher,
            elapsedTime: Duration,
            callback: () -> Unit
        ): PeriodicTimer = PeriodicTimer(dispatcher, elapsedTime, callback).apply { start() }
    }
}
