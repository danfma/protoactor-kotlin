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
import java.lang.AutoCloseable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.concurrent.withLock
import kotlin.coroutines.cancellation.CancellationException

typealias Action = () -> Unit

class SharedFutureProcess(system: ActorSystem, size: UInt) : Process(system), AutoCloseable {
    private val pid = register(system, this)
    private var onStarted: Action? = null
    private var onTimeout: Action? = null
    private val completedRequests = AtomicInt(0)
    private val createdRequests = AtomicInt(0)

    private val slots =
        Array<FutureHandle>(size.toInt()) {
            val requestId = (it + 1).toUInt()
            FutureHandle(requestId)
        }

    private val futures = slots.toMutableList()
    private val futuresLock = ReentrantLock()
    private val maxRequestId = (Int.MAX_VALUE - (Int.MAX_VALUE % size.toInt())).toUInt()

    var stopping = false
        private set

    val requestsInFlight: UInt
        get() {
            val completed = completedRequests.load()
            val created = createdRequests.load()

            return (created - completed).toUInt()
        }

    override fun close() {
        system.processRegistry.remove(pid)
        slots.forEach { it.tryCancel() }
    }

    override fun sendUserMessage(pid: PID, message: Any) {
        val slot = tryGetRequestSlot(pid.requestId)

        if (slot == null) {
            return
        }

        try {
            slot.complete(message)
        } finally {
            complete(pid.requestId, slot)
        }
    }

    override fun sendSystemMessage(pid: PID, message: SystemMessage) {
        if (message == Stop) {
            close()
            return
        }

        val slot = tryGetRequestSlot(pid.requestId)

        if (slot == null) {
            return
        }

        try {
            slot.complete(Nothing)
        } finally {
            complete(pid.requestId, slot)
        }
    }

    fun tryCreateHandle(): Future? {
        if (stopping) {
            return null
        }

        val requestSlot = tryTakeRequestSlot()

        if (requestSlot == null) {
            return null
        }

        val (pid, completableFuture) = requestSlot.init().also {
            createdRequests.incrementAndFetch()
        }

        onStarted?.invoke()

        return SharedFutureHandle(pid, completableFuture)
    }

    private fun tryTakeRequestSlot(): FutureHandle? {
        return futuresLock.withLock {
            if (futures.isEmpty())
                null
            else
                futures.removeFirst()
        }
    }

    private fun tryGetRequestSlot(requestId: UInt): FutureHandle? {
        if (requestId == 0u) {
            return null
        }

        val slotIndex = getSlotIndex(requestId)

        return slots[slotIndex].let { slot ->
            if (slot.requestId == requestId) {
                slot
            } else {
                null
            }
        }
    }

    private fun getSlotIndex(requestId: UInt): Int =
        (requestId.toInt() - 1) % slots.size

    private fun complete(requestId: UInt, slot: FutureHandle) {
        if (!slot.tryComplete(requestId)) {
            return
        }

        futuresLock.withLock {
            futures += slot
            completedRequests.incrementAndFetch()

            if (stopping && requestsInFlight == 0u) {
                stop(pid)
            }
        }
    }

    private fun cancel(requestId: UInt) {
        val slot = tryGetRequestSlot(requestId)

        if (slot == null) {
            return
        }

        try {
            slot.tryCancel()
        } finally {
            complete(requestId, slot)
        }
    }

    inner class FutureHandle(requestId: UInt) {
        private val atomicRequestId = AtomicLong(requestId.toLong())

        val requestId: UInt
            get() = atomicRequestId.load().toUInt()

        var completableFuture: CompletableFuture<Any>? = null
            private set

        fun init(): Pair<PID, CompletableFuture<Any>> {
            val future = CompletableFuture<Any>().also {
                completableFuture = it
            }

            val pid = pid.copy(requestId = requestId)

            return pid to future
        }

        fun tryComplete(requestId: UInt): Boolean {
            val jumpSize = slots.size
            val nextRequestId = (requestId + jumpSize.toUInt()) % maxRequestId

            val previousRequestId =
                this.atomicRequestId
                    .compareAndExchange(
                        expectedValue = requestId.toLong(),
                        newValue = nextRequestId.toLong()
                    )
                    .toUInt()

            return requestId == previousRequestId
        }

        fun tryCancel() {
            completableFuture?.cancel(false)
            completableFuture = null
        }

        fun complete(message: Any) {
            completableFuture?.complete(message)
        }
    }

    inner class SharedFutureHandle(
        override val pid: PID,
        completableFuture: CompletableFuture<Any>
    ) : Future {
        override val deferred: Deferred<Any> = completableFuture.asDeferred()

        override suspend fun getDeferred(): Any {
            return try {
                deferred.await()
            } catch (_: CancellationException) {
                cancel(pid.requestId)
                onTimeout?.invoke()

                throw TimeoutException("Request didn't receive any response within the expected time")
            }
        }

        override fun close() {
            cancel(pid.requestId)
        }
    }

    companion object {
        private fun register(system: ActorSystem, process: Process): PID {
            val name = system.processRegistry.nextId()
            val (pid, absent) = system.processRegistry.tryAdd(name, process)

            if (!absent) {
                throw ProcessNameExistException(name, pid)
            }

            return pid
        }
    }
}

