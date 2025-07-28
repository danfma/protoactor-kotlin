package proto.actor

import mu.KotlinLogging
import proto.actor.contexts.RootContextImpl
import proto.actor.messages.PID
import proto.actor.messages.SystemMessage
import proto.actor.processes.DeadLetterProcess
import proto.actor.processes.configure
import kotlin.uuid.Uuid

class ActorSystem(val config: ActorSystemConfig) {
    private var host = PID.NO_HOST
    private var port = 0

    val name = Uuid.random().toString()
    val processRegistry = ProcessRegistry(this)
    val root = createRoot()

    var address = PID.NO_HOST
        private set

    var isShuttingDown = false
        private set

    var deadLetter: Process = DeadLetterProcess(this)
        private set

    var deadLetterPid: PID = PID.from(address, DeadLetterProcess.NAME, deadLetter)
        private set

    val future by lazy {
        FutureFactory(this, config.sharedFutures, config.sharedFutureSize)
    }

    val guardians = Guardians(this)
    val eventStream = ActorSystemEventStream(this)
    val extensions = ActorSystemExtensions(this)

    init {
        deadLetter.configure()
        processRegistry.tryAdd(DeadLetterProcess.NAME, deadLetter)
    }

    fun configureProps(props: Props): Props =
        config.configureProps(props)

    internal fun sendUserMessage(message: Any, target: PID) {
        if (target == PID.none) {
            return
        }

        val process = resolveProcess(target)

        process.sendUserMessage(target, message)
    }

    internal fun sendSystemMessage(message: SystemMessage, target: PID) {
        if (target == PID.none) {
            return
        }

        val process = resolveProcess(target)

        process.sendSystemMessage(target, message)
    }

    internal fun sendAllUserMessage(message: Any, vararg targets: PID) {
        targets.forEach {
            sendUserMessage(message, it)
        }
    }

    internal fun sendAllSystemMessage(message: SystemMessage, vararg targets: PID) {
        targets.forEach {
            sendSystemMessage(message, it)
        }
    }

    private fun resolveProcess(pid: PID): Process {
        return pid.getCachedProcess() ?: resolveProcessByProcessRegistry(pid)
    }


    private fun resolveProcessByProcessRegistry(pid: PID): Process {
        val process = processRegistry.get(pid)

        return process.also {
            pid.setCachedProcess(it)
        }
    }

    internal fun configureSystemProps(name: String, props: Props): Props =
        config.configureSystemProps(name, props)

    fun createRoot(
        header: MessageHeader = MessageHeader.empty,
        vararg middlewares: (Sender) -> Sender
    ): RootContext {
        val root = RootContextImpl(this).withSenderMiddleware(*middlewares)
        val configured = config.configureRootContext(root)

        return configured
    }

    companion object {
        private val logger = KotlinLogging.logger {}

        fun create(config: ActorSystemConfig) = ActorSystem(config)

        fun create() = ActorSystem(ActorSystemConfig.default)
    }
}

