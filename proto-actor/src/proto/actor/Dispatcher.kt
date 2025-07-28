package proto.actor

import kotlinx.coroutines.Deferred

interface Dispatcher {
    var throughput: Int

    fun schedule(runner: suspend () -> Unit)

    fun <T> defer(runner: suspend () -> T): Deferred<T>

    companion object {
        val default: Dispatcher = DefaultDispatcher()
    }
}
