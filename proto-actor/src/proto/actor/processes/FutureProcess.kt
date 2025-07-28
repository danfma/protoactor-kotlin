package proto.actor.processes

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.asDeferred
import proto.actor.ActorSystem
import proto.actor.Future
import proto.actor.Process
import proto.actor.exceptions.ProcessNameExistException
import proto.actor.messages.Nothing
import proto.actor.messages.PID
import proto.actor.messages.Stop
import proto.actor.messages.SystemMessage
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException

class FutureProcess(system: ActorSystem) : Process(system), Future {
    private val completableFuture = CompletableFuture<Any>()

    override val pid: PID = registry(system, this).copy(requestId = 1u)

    override val deferred: Deferred<Any>
        get() = completableFuture.asDeferred()

    private val dispatcher
        get() = system.config.dispatcher

    override fun sendUserMessage(pid: PID, message: Any) {
        dispatcher.schedule {
            try {
                completableFuture.complete(message)
            } finally {
                stop(pid)
            }
        }
    }

    override fun sendSystemMessage(pid: PID, message: SystemMessage) {
        if (message is Stop) {
            close()
            return
        }

        dispatcher.schedule {
            completableFuture.complete(Nothing)
            stop(pid)
        }
    }

    override suspend fun getDeferred(): Any {
        try {
            return deferred.await()
        } catch (e: Throwable) {
            throw TimeoutException("Request didn't receive any response within the expected time")
        } finally {
            stop(pid)
        }
    }

    override fun close() {
        completableFuture.cancel(true)
        system.processRegistry.remove(pid)
    }

    companion object {
        private fun registry(system: ActorSystem, process: Process): PID {
            val name = system.processRegistry.nextId()
            val (pid, absent) = system.processRegistry.tryAdd(name, process)

            if (!absent) {
                throw ProcessNameExistException(name, pid)
            }

            return pid
        }
    }
}
