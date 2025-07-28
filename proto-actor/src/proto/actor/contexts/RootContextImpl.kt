package proto.actor.contexts

import mu.KotlinLogging
import proto.actor.*
import proto.actor.messages.Nothing
import proto.actor.messages.PID
import proto.actor.messages.PoisonPill
import proto.actor.messages.Watch
import kotlin.reflect.KClass

class RootContextImpl(
    override val system: ActorSystem,
    private val store: ContextStore = ContextStore.create()
) : RootContext, ContextStore by store {

    private var senderMiddleware: Sender? = null

    override val message: Any = Nothing
    override val parent: PID = PID.none
    override val self: PID = PID.none
    override val sender: PID? = null
    override val actor: Actor = NoOpActor

    override var header: MessageHeader = MessageHeader.empty
        private set

    override fun spawnNamed(
        props: Props, name: String, callback: ((Context) -> Unit)?
    ): PID {
        val name = name.ifEmpty { system.processRegistry.nextId() }

        return try {
            val guardianStrategy = props.guardianStrategy

            val parent = if (guardianStrategy != null) system.guardians.getById(guardianStrategy)
            else PID.none

            props.spawn(system, name, parent, callback)
        } catch (e: Exception) {
            logger.error(e) { "Failed to spawn root level actor with name $name" }
            throw e
        }
    }

    override fun withSenderMiddleware(vararg middlewares: SenderMiddleware): RootContext {
        if (middlewares.isEmpty()) {
            return this
        }

        senderMiddleware = aggregateMiddleware(*middlewares)

        return this
    }

    fun withHeader(header: MessageHeader): RootContext {
        this.header = header
        return this
    }

    override fun send(target: PID, message: Any) {
        val sender = senderMiddleware

        if (sender != null) {
            sender(this, target, MessageEnvelope.wrap(message))
        } else {
            system.sendUserMessage(message, target)
        }
    }

    override fun request(target: PID, message: Any, sender: PID) {
        val envelope = MessageEnvelope.withSender(message, sender)

        send(target, envelope)
    }

    override suspend fun <T : Any> requestAndWait(
        responseType: KClass<T>, target: PID, message: Any
    ): T = SenderUtility.requestAndWait(
        sender = this,
        responseType = responseType,
        target = target,
        message = message
    )

    override fun getFuture(): Future = system.future.get()

    override fun stop(pid: PID) {
        if (pid == PID.none) {
            return
        }

        val process = system.processRegistry.get(pid)
        process.stop(pid)
    }

    override suspend fun waitStopOf(pid: PID) {
        val future = system.future.get()

        system.sendSystemMessage(Watch(future.pid), pid)
        stop(pid)

        future.deferred.await()
    }

    override fun poison(pid: PID) {
        system.sendUserMessage(PoisonPill, pid)
    }

    override suspend fun waitPoisonOf(pid: PID) {
        val future = system.future.get()

        system.sendSystemMessage(Watch(future.pid), pid)
        poison(pid)

        future.deferred.await()
    }

    private fun aggregateMiddleware(vararg middlewares: (Sender) -> Sender): Sender {
        return middlewares.reversed().fold(defaultSender) { inner, outer -> outer(inner) }
    }

    companion object {
        private val logger = KotlinLogging.logger {}

        private val defaultSender: Sender = { context, target, message ->
            context.system.sendUserMessage(message, target)
        }
    }

    object NoOpActor : Actor {
        override suspend fun Context.receive(message: Any) {
            // do nothing
        }
    }
}
