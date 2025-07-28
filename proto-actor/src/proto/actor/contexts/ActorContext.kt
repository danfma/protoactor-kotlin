package proto.actor.contexts

import kotlinx.coroutines.Deferred
import mu.KotlinLogging
import proto.actor.*
import proto.actor.mailbox.MessageInvoker
import proto.actor.messages.*
import proto.actor.messages.Nothing
import kotlin.reflect.KClass
import kotlin.time.Clock
import kotlin.time.Duration

class ActorContext private constructor(
    override val system: ActorSystem,
    override val parent: PID,
    override val self: PID,
    private val props: Props,
    private val store: ContextStore = ContextStore.create()
) : MessageInvoker, Context, SenderContext, Supervisor, ContextStore by store {

    private val logger = KotlinLogging.logger {}
    private var messageOrEnvelope: Any = Nothing
    private var extras: ActorContextExtras? = null
    private var state: ContextState = ContextState.Stopped

    override var actor: Actor = incarnateActor()
        private set

    override val sender: PID?
        get() = MessageEnvelope.unwrapSender(messageOrEnvelope)

    override val message: Any
        get() = MessageEnvelope.unwrapMessage(messageOrEnvelope)

    override val header: MessageHeader
        get() = MessageEnvelope.unwrapHeaders(messageOrEnvelope)

    override var receiveTimeout: Duration = Duration.ZERO
        private set

    override val children: List<PID>
        get() = extras?.children ?: emptyList()

    private fun incarnateActor(): Actor {
        state = ContextState.Alive
        return props.producer(system, this)
    }

    override suspend fun invokeUserMessage(message: Any) {
        if (state == ContextState.Stopped || system.isShuttingDown) {
            system.deadLetter.sendUserMessage(self, message)
            return
        }

        val shouldResetReceiveTimeout =
            receiveTimeout > Duration.ZERO && MessageEnvelope.unwrapMessage(message) !is NoReceiveTimeoutInfluence

        try {
            val receiverMiddlewareChain = props.receiverMiddlewareChain
            val contextDecoratorChain = props.contextDecoratorChain
            val wrappedMessage = MessageEnvelope.wrap(message)

            if (receiverMiddlewareChain != null) {
                receiverMiddlewareChain(ensureExtras().context, wrappedMessage)
            } else if (contextDecoratorChain != null) {
                ensureExtras().context.receive(wrappedMessage)
            } else {
                messageOrEnvelope = message
                executeDefaultReceive()
            }
        } finally {
            if (shouldResetReceiveTimeout) {
                extras?.resetReceiveTimeoutTimer()
            }
        }
    }

    override suspend fun invokeSystemMessage(message: SystemMessage) {
        return try {
            when (message) {
                is Started -> handleStarted()
                is Stop -> handleStop()
                is Terminated -> handleTerminated(message)
                is Failure -> handleFailure(message)
                is Watch -> handleWatch(message)
                is Unwatch -> handleUnwatch(message)
                is Restart -> handleRestart()
                is SuspendMailbox, is ResumeMailbox -> {}
                is Continuation -> handleContinuation(message)
                is ReceiveTimeout -> handleReceiveTimeout()
                else -> handleUnknownSystemMessage(message)
            }
        } catch (e: Exception) {
            logger.error(e) { "$self failed to handle system message $message" }
            throw e
        }
    }

    override fun escalateFailure(reason: Exception, message: Any) {
        if (system.config.developerSupervisionLogging) {
            logger.warn { "[Supervision] Actor $self : ${actor.javaClass.simpleName} failed with message: $message, exception: $reason" }
        }

        val failure = Failure(
            who = self, reason = reason, restartStatistics = ensureExtras().restartStatistics, message = message
        )

        system.sendSystemMessage(failure, self)

        if (parent == PID.none) {
            handleRootFailure(failure)
        } else {
            system.sendSystemMessage(failure, parent)
        }
    }

    override fun cancelInvoke() {
        // Is this really needed?
    }

    override fun restartChildren(reason: Exception, vararg targets: PID) {
        system.sendAllSystemMessage(Restart(reason), *targets)
    }

    override fun stopChildren(vararg targets: PID) {
        system.sendAllSystemMessage(Stop, *targets)
    }

    override fun resumeChildren(vararg targets: PID) {
        system.sendAllSystemMessage(ResumeMailbox, *targets)
    }

    override fun respond(message: Any) {
        val sender = sender

        if (sender != null) {
            logger.debug { "$self responding to $sender with message $message" }
            sendUserMessage(sender, message)
        } else {
            logger.warn { "$self tried to respond bu sender is null, with message $message" }
        }
    }

    override fun watch(pid: PID) {
        system.sendSystemMessage(Watch(self), pid)
    }

    override fun unwatch(pid: PID) {
        system.sendSystemMessage(Unwatch(self), pid)
    }

    override fun setReceiveTimeout(duration: Duration) {
        if (duration <= Duration.ZERO) {
            throw IllegalArgumentException("Timeout must be greater than zero")
        }

        if (duration == receiveTimeout) {
            return
        }

        receiveTimeout = duration

        ensureExtras().let { extras ->
            extras.stopReceiveTimeoutTimer()

            if (extras.receiveTimeoutTimer == null) {
                extras.setReceiveTimeoutTimer(
                    PeriodicTimer.create(
                        dispatcher = props.dispatcher,
                        elapsedTime = receiveTimeout,
                        callback = ::onReceiveTimeoutCallback
                    )
                )
            } else {
                extras.resetReceiveTimeoutTimer()
            }
        }
    }

    private fun onReceiveTimeoutCallback() {
        if (extras?.receiveTimeoutTimer == null) {
            return
        }

        system.sendSystemMessage(ReceiveTimeout, self)
    }

    override fun cancelReceiveTimeout() {
        val extras = extras

        if (extras?.receiveTimeoutTimer == null) {
            return
        }

        extras.cancelReceiveTimeoutTimer()
        receiveTimeout = Duration.ZERO
    }

    override fun forward(target: PID) {
        when (messageOrEnvelope) {
            Nothing -> logger.warn { "Nothing to be propagated as message" }
            is SystemMessage -> logger.warn { "System message can not be forwarded (message = $messageOrEnvelope)" }
            else -> sendUserMessage(target, messageOrEnvelope)
        }
    }

    override fun <T> reenterAfter(
        deferred: Deferred<T>,
        action: suspend Context.(Deferred<T>) -> Unit
    ) {
        val message = messageOrEnvelope
        val context = this

        val continuation = Continuation.create(message, actor) {
            with(actor) {
                context.action(deferred)
            }
        }

        scheduleContinuation(deferred, continuation)
    }

    private fun scheduleContinuation(task: Deferred<*>, continuation: Continuation) {
        props.dispatcher.schedule {
            try {
                task.join()
            } catch (e: Throwable) {
                logger.debug(e) { "$self failed to complete task $task" }
            }

            if (!task.isCancelled) {
                system.sendSystemMessage(continuation, self)
            }
        }
    }

    override fun send(target: PID, message: Any) {
        sendUserMessage(target, message)
    }

    override fun request(target: PID, message: Any, sender: PID) {
        val envelope = MessageEnvelope.withSender(message, sender)

        sendUserMessage(target, envelope)
    }

    override suspend fun <T : Any> requestAndWait(
        responseType: KClass<T>,
        target: PID,
        message: Any
    ): T = SenderUtility.requestAndWait(
        sender = this,
        responseType = responseType,
        target = target,
        message = message
    )

    override fun getFuture(): Future = system.future.get()

    override suspend fun receive(envelope: MessageEnvelope) {
        messageOrEnvelope = envelope

        executeDefaultReceive()
    }

    override fun spawnNamed(
        props: Props, name: String, callback: ((Context) -> Unit)?
    ): PID {
        if (props.guardianStrategy != null) {
            throw IllegalArgumentException("Props used to spawn child can not have a guardian strategy")
        }

        try {
            val childId = when (name) {
                "" -> system.processRegistry.nextId()
                else -> "${self.id}/$name"
            }

            val pid = props.spawn(system, childId, self, callback)

            ensureExtras().addChild(pid)

            return pid
        } catch (e: Throwable) {
            logger.error(e) { "$self failed to spawn child actor $name" }
            throw e
        }
    }

    override fun stop(pid: PID) {
        pid.stop(system)
    }

    override suspend fun waitStopOf(pid: PID) {
        getFuture().use { future ->
            system.sendSystemMessage(Watch(future.pid), pid)
            stop(pid)
            future.deferred.join()
        }
    }

    override fun poison(pid: PID) {
        system.sendUserMessage(PoisonPill, pid)
    }

    override suspend fun waitPoisonOf(pid: PID) {
        getFuture().use { future ->
            system.sendSystemMessage(Watch(future.pid), pid)
            poison(pid)
            future.deferred.join()
        }
    }

    private suspend fun handleStarted() {
        val deadline = props.startDeadline

        if (deadline == Duration.ZERO) {
            invokeUserMessage(Started)
            return
        }

        val start = Clock.System.now()

        invokeUserMessage(Started)

        val elapsed = Clock.System.now() - start

        if (elapsed > deadline) {
            logger.warn { "Actor $self took too long to start, deadline is $deadline, actual start is $elapsed, your system might suffer from incorrect design, please consider reaching out to https://proto.actor/docs/training/ for help" }
        }
    }

    private suspend fun handleStop() {
        if (state >= ContextState.Stopping) {
            // already stopping or stopped
            return
        }

        state = ContextState.Stopping
        cancelReceiveTimeout()

        try {
            invokeUserMessage(Stopping)
        } catch (e: Exception) {
            logger.error(e) { "$self error while handling Stopping message" }
        }

        stopAllChildren()
    }

    private suspend fun handleTerminated(message: Terminated) {
        extras?.removeChild(message.who)
        invokeUserMessage(message)

        if (state == ContextState.Stopping || state == ContextState.Restarting) {
            tryRestartOrStop()
        }
    }

    private fun handleFailure(failure: Failure) {
        Supervision.defaultStrategy.handleFailure(
            this, failure.who, failure.restartStatistics, failure.reason, failure.message
        )
    }

    private fun handleRootFailure(failure: Failure) {
        Supervision.defaultStrategy.handleFailure(
            supervisor = this,
            child = failure.who,
            statistics = failure.restartStatistics,
            reason = failure.reason,
            message = failure.message
        )
    }

    private fun handleWatch(message: Watch) {
        if (state >= ContextState.Stopping) {
            system.sendSystemMessage(
                Terminated(self, TerminatedReason.Stopped),
                message.watcher
            )
        } else {
            ensureExtras().watch(message.watcher)
        }
    }

    private fun handleUnwatch(message: Unwatch) {
        extras?.unwatch(message.watcher)
    }

    private suspend fun handleRestart() {
        if (system.isShuttingDown) {
            handleStop()
            return
        }

        state = ContextState.Restarting

        cancelReceiveTimeout()
        invokeUserMessage(Restarting)
        stopAllChildren()
    }

    private suspend fun handleContinuation(message: Continuation) {
        if (state == ContextState.Stopped || system.isShuttingDown) {
            logger.warn { "$self dropping continuation (ReenterAfter) of $message" }
            return
        }

        messageOrEnvelope = message.message
        message.action()
    }

    private suspend fun handleReceiveTimeout() {
        messageOrEnvelope = ReceiveTimeout
        invokeUserMessage(messageOrEnvelope)
    }

    private fun handleUnknownSystemMessage(message: SystemMessage) {
        logger.warn { "Unknown system message $message" }
    }

    private fun handlePoisonPill() {
        stop(self)
    }

    private suspend fun handleAutoRespond(message: AutoRespond) {
        val contextDecoratorChain = props.contextDecoratorChain
        val context = if (contextDecoratorChain != null) ensureExtras().context else this

        with(actor) {
            context.receive(message)
        }

        val response = message.getAutoResponse(this)

        respond(response)
    }

    private suspend fun stopAllChildren() {
        extras?.children?.forEach { child ->
            system.root.stop(child)
        }

        tryRestartOrStop()
    }

    private suspend fun tryRestartOrStop() {
        if (extras?.children?.isNotEmpty() == true) {
            return
        }

        cancelReceiveTimeout()

        when (state) {
            ContextState.Restarting -> restart()
            ContextState.Stopping -> finalizeStop()
            else -> {}
        }
    }

    private suspend fun restart() {
        closeActorIfAutoClosable()
        actor = incarnateActor()
        system.sendSystemMessage(ResumeMailbox, self)
        invokeUserMessage(Started)
    }

    private suspend fun finalizeStop() {
        system.processRegistry.remove(self)

        // this is intentional
        invokeUserMessage(Stopped)
        extras?.close()
        closeActorIfAutoClosable()
        notifyWatchersActorHasStopped()

        state = ContextState.Stopped
    }

    private fun notifyWatchersActorHasStopped() {
        val terminated = Terminated(self, TerminatedReason.Stopped)
        val watchers = extras?.watchers

        if (watchers?.isNotEmpty() == true) {
            watchers.forEach { watcher ->
                system.sendSystemMessage(terminated, watcher)
            }
        }

        system.sendSystemMessage(terminated, parent)
    }

    private fun closeActorIfAutoClosable() {
        val actor = actor

        if (actor is AutoCloseable) {
            actor.close()
        }
    }

    private fun sendUserMessage(target: PID, message: Any) {
        val senderMiddlewareChain = props.senderMiddlewareChain

        if (senderMiddlewareChain == null) {
            system.sendUserMessage(message, target)
        } else {
            senderMiddlewareChain.invoke(
                ensureExtras().context, target, MessageEnvelope.wrap(message)
            )
        }
    }

    private fun ensureExtras(): ActorContextExtras {
        if (extras != null) {
            return extras!!
        }

        synchronized(this) {
            if (extras != null) {
                return extras!!
            }

            val context = props.contextDecoratorChain?.invoke(this) ?: this

            extras = ActorContextExtras(context)
        }

        return extras!!
    }

    private suspend fun executeDefaultReceive() {
        val message = message

        when (message) {
            is PoisonPill -> handlePoisonPill()
            is AutoRespond -> handleAutoRespond(message)
            else -> with(actor) {
                receive(message)
            }
        }
    }

    companion object {
        internal fun setup(system: ActorSystem, props: Props, parent: PID, self: PID) =
            ActorContext(system, parent, self, props)
    }

}
