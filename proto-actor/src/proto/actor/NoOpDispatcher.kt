package proto.actor

import kotlinx.coroutines.Deferred

object NoOpDispatcher : Dispatcher {
    override var throughput: Int = 0

    override fun schedule(runner: suspend () -> Unit) {
        throw NotImplementedError()
    }

    override fun <T> defer(runner: suspend () -> T): Deferred<T> {
        throw NotImplementedError()
    }
}
