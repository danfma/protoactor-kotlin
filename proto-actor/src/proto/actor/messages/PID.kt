package proto.actor.messages

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import proto.actor.ActorSystem
import proto.actor.Process
import proto.actor.processes.ActorProcess
import proto.actor.processes.DeadLetterProcess

@Serializable
data class PID(
    val address: String,
    val id: String,
    val requestId: UInt = NO_REQUEST_ID
) : CustomDiagnosticMessage {

    @Transient
    internal var processRef: Process? = null
        private set

    constructor(address: String, id: String, process: Process)
            : this(address, id) {
        processRef = process
    }

    internal fun getCachedProcess(): Process? {
        val actorProcess = processRef as? ActorProcess

        return actorProcess?.let { process ->
            if (process.isDead) {
                processRef = null
                null
            } else {
                process
            }
        }
    }

    internal fun setCachedProcess(process: Process) {
        if (process !is DeadLetterProcess) {
            processRef = process
        }
    }

    fun stop(system: ActorSystem) {
        val ref = processRef ?: system.processRegistry.get(this)

        ref.stop(this)
    }

    override fun toDiagnosticString(): String {
        if (requestId != NO_REQUEST_ID) {
            return "$address/$id:$requestId"
        }

        return "$address/$id"
    }

    companion object {
        const val NO_REQUEST_ID = UInt.MIN_VALUE
        const val NO_HOST = "nonhost"
        const val CLIENT_PREFIX = $$"$client"
        const val NONE = $$"$none"

        val none = from(NO_HOST, NONE)

        fun from(address: String, id: String) = PID(address, id)

        internal fun from(address: String, id: String, process: Process) = PID(address, id, process)
    }
}

