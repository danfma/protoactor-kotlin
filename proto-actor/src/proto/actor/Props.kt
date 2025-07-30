package proto.actor

import mu.KLogger
import proto.actor.logging.ActorLoggingContext
import proto.actor.logging.LogLevel
import proto.actor.mailbox.DefaultMailbox
import proto.actor.mailbox.Mailbox
import proto.actor.messages.PID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

typealias MailboxProducer = () -> Mailbox
typealias Receive = suspend (Context) -> Unit
typealias Receiver = suspend (ReceiverContext, MessageEnvelope) -> Unit
typealias ReceiverMiddleware = (Receiver) -> Receiver
typealias Sender = (SenderContext, PID, MessageEnvelope) -> Unit

typealias ReceiveMiddleware = (Receive) -> Receive
typealias SenderMiddleware = (Sender) -> Sender

typealias ProducerWithSystemAndContext = (system: ActorSystem, context: Context) -> Actor


class Props {
    var spawner: Spawner = Spawner.default
        private set

    var producer: ProducerWithSystemAndContext = nullProducer
        private set

    var startDeadline: Duration = 100.milliseconds
        private set

    var mailboxProducer: MailboxProducer = { DefaultMailbox() }
        private set

    var guardianStrategy: SupervisorStrategy? = null
        private set

    var supervisorStrategy: SupervisorStrategy? = Supervision.defaultStrategy
        private set

    var dispatcher: Dispatcher = Dispatcher.default
        private set

    var receiverMiddlewares: Array<ReceiverMiddleware> = emptyArray()
        private set

    var senderMiddlewares: Array<SenderMiddleware> = emptyArray()
        private set

    var contextDecorators: Array<ContextDecorator> = emptyArray()
        private set

    internal var receiverMiddlewareChain: Receiver? = null
        private set

    internal var senderMiddlewareChain: Sender? = null
        private set

    internal var contextDecoratorChain: ((Context) -> Context)? = null
        private set

    fun withSpawner(spawner: Spawner): Props {
        this.spawner = spawner
        return this
    }

    fun withProducer(producer: ProducerWithSystemAndContext): Props {
        this.producer = producer
        return this
    }

    fun withStartDeadline(startDeadline: Duration): Props {
        this.startDeadline = startDeadline
        return this
    }

    fun withMailboxProducer(mailboxProducer: MailboxProducer): Props {
        this.mailboxProducer = mailboxProducer
        return this
    }

    fun withGuardianStrategy(guardianStrategy: SupervisorStrategy): Props {
        this.guardianStrategy = guardianStrategy
        return this
    }

    fun withSupervisorStrategy(supervisorStrategy: SupervisorStrategy): Props {
        this.supervisorStrategy = supervisorStrategy
        return this
    }

    fun withDispatcher(dispatcher: Dispatcher): Props {
        this.dispatcher = dispatcher
        return this
    }

    fun withMailbox(mailboxProducer: () -> Mailbox): Props {
        this.mailboxProducer = mailboxProducer
        return this
    }

    fun withGuardianSupervisorStrategy(supervisorStrategy: SupervisorStrategy): Props {
        this.guardianStrategy = supervisorStrategy
        return this
    }

    fun withChildSupervisorStrategy(supervisorStrategy: SupervisorStrategy): Props {
        this.supervisorStrategy = supervisorStrategy
        return this
    }

    fun withReceiverMiddlewares(vararg receiverMiddlewares: ReceiverMiddleware): Props {
        this.receiverMiddlewares += receiverMiddlewares
        this.receiverMiddlewareChain = this.receiverMiddlewares
            .reversed()
            .fold(defaultReceiver) { inner, outer -> outer(inner) }

        return this
    }

    fun withSenderMiddlewares(vararg senderMiddlewares: SenderMiddleware): Props {
        this.senderMiddlewares += senderMiddlewares
        this.senderMiddlewareChain = this.senderMiddlewares
            .reversed()
            .fold(defaultSender) { inner, outer -> outer(inner) }

        return this
    }

    fun withContextDecorators(vararg contextDecorators: ContextDecorator): Props {
        this.contextDecorators += contextDecorators
        this.contextDecoratorChain = contextDecorators
            .reversed()
            .fold(defaultContextDecorator) { inner, outer ->
                { context -> outer(inner(context)) }
            }

        return this
    }

    fun withDeadlineDecorator(deadline: Duration, logger: KLogger): Props {
        return withContextDecorators(
            { context -> DeadlineContextDecorator(context, deadline, logger, this) }
        )
    }

    fun withLoggingContextDecorator(
        logger: KLogger,
        logLevel: LogLevel = LogLevel.Debug,
        infrastructureLogLevel: LogLevel = LogLevel.None,
        exceptionLogLevel: LogLevel = LogLevel.Error
    ): Props {
        return withContextDecorators(
            { context ->
                ActorLoggingContext(context, logger, logLevel, infrastructureLogLevel, exceptionLogLevel)
            }
        )
    }

    fun createMailbox(): Mailbox = mailboxProducer()

    internal fun spawn(system: ActorSystem, name: String, parent: PID, callback: (Context.() -> Unit)?): PID {
        return spawner.spawn(system, name, this, parent, callback)
    }

    internal fun spawnSystem(system: ActorSystem, name: String, parent: PID, callback: ((Context) -> Unit)?): PID {
        return Spawner.default.spawn(system, name, this, parent, callback)
    }

    companion object {
        val empty
            get() = Props()

        private val nullProducer: ProducerWithSystemAndContext = { _, _ -> NullActor }
        private val defaultContextDecorator: ContextDecorator = { context -> context }
        private val defaultReceiver: Receiver = { context, envelope -> context.receive(envelope) }
        private val defaultSender: Sender = { context, pid, envelope -> context.send(pid, envelope) }

        fun <TActor : Actor> fromProducer(producer: () -> TActor) =
            empty.withProducer { _, _ ->
                producer()
            }

        fun <TActor : Actor> fromProducerWithSystem(producer: (ActorSystem) -> TActor) =
            empty.withProducer { system, _ ->
                producer(system)
            }

        fun <TActor : Actor> fromProducerWithSystemAndContext(producer: (ActorSystem, Context) -> TActor) =
            empty.withProducer { system, context ->
                producer(system, context)
            }

        fun fromFunction(receiver: suspend Context.() -> Unit): Props =
            fromProducer {
                FunctionActor(receiver)
            }
    }
}

