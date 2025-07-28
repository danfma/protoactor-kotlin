package proto.actor.extensions

import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.job
import kotlinx.coroutines.withTimeout
import proto.actor.messages.PID
import kotlin.time.Clock
import kotlin.time.Duration

internal suspend fun Job.waitUpTo(deadline: Duration): Boolean {
    if (!job.isCompleted) {
        try {
            withTimeout(deadline) {
                job.join()
            }
        } catch (_: TimeoutCancellationException) {
            return false
        }
    }

    return true
}

suspend inline fun <T> measureTime(block: suspend () -> T): Pair<T, Duration> {
    val start = Clock.System.now()
    val result = block()
    val end = Clock.System.now()

    return result to (end - start)
}

fun Any.getClassName() = this::class.simpleName ?: "UnknownClass"

fun PID?.describe() = this?.toString() ?: "None"

