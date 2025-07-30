package proto.actor

import proto.actor.exceptions.ProcessNameExistException
import proto.actor.messages.PID
import proto.actor.processes.ActorProcess
import proto.actor.processes.GuardianProcess
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class ProcessRegistry(private val system: ActorSystem) {
    private val hostResolvers: MutableList<(PID) -> Process?> = mutableListOf()
    private val localProcesses = ConcurrentHashMap<String, Process>()
    private val sequenceId: AtomicInteger = AtomicInteger(0)

    val processCount
        get() = localProcesses.size

    fun registerHostResolver(resolver: (PID) -> Process) {
        hostResolvers.add(resolver)
    }

    internal fun getLocalActorPids(): List<PID> {
        return localProcesses
            .filter { (_, process) -> process is ActorProcess && !process.isDead }
            .map { (id, process) -> PID.from(system.address, id, process) }
    }

    fun get(pid: PID): Process {
        fun isPidHostless() = pid.address == PID.NO_HOST
        fun isPidLocalAndNonClient() = pid.address == system.address && !pid.id.startsWith(PID.CLIENT_PREFIX)

        if (isPidHostless() || isPidLocalAndNonClient()) {
            return localProcesses.getOrDefault(pid.id, system.deadLetter)
        }

        val ref = hostResolvers
            .asSequence()
            .map { resolver -> resolver(pid) }
            .filterNotNull()
            .firstOrNull()

        if (ref == null) {
            throw UnsupportedOperationException("Unknown host")
        }

        return ref
    }

    fun find(predicate: (String) -> Boolean): Sequence<PID> {
        return localProcesses
            .asSequence()
            .filter { predicate(it.key) }
            .map { PID.from(system.address, it.key, it.value) }
    }

    fun tryAdd(id: String, process: Process): Pair<PID, Boolean> {
        val pid = PID.from(system.address, id, process)
        val added = localProcesses.putIfAbsent(pid.id, process).let { it == null || it == process }

        if (added) {
            return pid to true
        }

        return PID.from(system.address, id) to false
    }

    fun add(name: String, process: GuardianProcess): PID {
        val (pid, added) = tryAdd(name, process)

        if (!added) {
            throw ProcessNameExistException(name, pid)
        }

        return pid
    }

    fun remove(pid: PID) {
        localProcesses.remove(pid.id)
    }

    fun nextId(): String {
        return sequenceId.incrementAndGet().let {
            $$"$$$it"
        }
    }
}

