package proto.actor

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class DefaultDispatcher(
    context: CoroutineContext = Dispatchers.Default,
    override var throughput: Int = 300
) : Dispatcher {
    private val scope: CoroutineScope = CoroutineScope(context) + SupervisorJob()

    override fun schedule(runner: suspend () -> Unit) {
        scope.launch {
            runner()
        }
    }

    override fun <T> defer(runner: suspend () -> T): Deferred<T> {
        return scope.async {
            runner()
        }
    }
}

