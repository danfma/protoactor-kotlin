package proto.actor

import proto.actor.processes.FutureProcess
import proto.actor.processes.SharedFutureProcess

class FutureFactory(
    val system: ActorSystem,
    useSharedFutures: Boolean,
    sharedFutureSize: UInt
) {
    private val sharedFutureProcess =
        if (useSharedFutures) {
            SharedFutureProcess(system, sharedFutureSize)
        } else {
            null
        }

    fun get() =
        sharedFutureProcess?.tryCreateHandle() ?: FutureProcess(system)
}

