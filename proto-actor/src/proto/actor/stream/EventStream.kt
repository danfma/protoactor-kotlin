package proto.actor.stream

import kotlinx.coroutines.channels.Channel
import mu.KLogger
import proto.actor.Dispatcher
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

open class EventStream<T : Any>(
    private val dispatcher: Dispatcher,
    private val logger: KLogger
) {
    private val subscriptions = ConcurrentHashMap<EventStreamSubscriptionId, EventStreamSubscription<T>>()

    fun subscribe(name: String, action: (T) -> Unit, dispatcher: Dispatcher? = null): EventStreamSubscription<T> {
        val subscription = EventStreamSubscription<T>(
            this,
            name,
            dispatcher ?: this.dispatcher,
        ) { action(it) }

        subscriptions.putIfAbsent(subscription.id, subscription)

        return subscription
    }

    fun subscribe(name: String, channel: Channel<T>, dispatcher: Dispatcher? = null): EventStreamSubscription<T> {
        val subscription = EventStreamSubscription<T>(
            this,
            name,
            dispatcher ?: this.dispatcher
        ) { message -> channel.send(message) }

        subscriptions.putIfAbsent(subscription.id, subscription)

        return subscription
    }

    @Suppress("UNCHECKED_CAST")
    fun <TMessage : T> subscribe(
        name: String,
        channel: Channel<TMessage>,
        messageType: KClass<TMessage>,
        dispatcher: Dispatcher? = null
    ): EventStreamSubscription<T> {
        val subscription = EventStreamSubscription<T>(
            this,
            name,
            dispatcher ?: this.dispatcher
        ) { message ->
            if (messageType.isInstance(message)) {
                channel.send(message as TMessage)
            }
        }

        subscriptions.putIfAbsent(subscription.id, subscription)

        return subscription
    }

    @Suppress("UNCHECKED_CAST")
    fun <TMessage : T> subscribe(
        name: String,
        action: (TMessage) -> Unit,
        messageType: KClass<TMessage>,
        dispatcher: Dispatcher? = null
    ): EventStreamSubscription<T> {
        val subscription = EventStreamSubscription<T>(
            this,
            name,
            dispatcher ?: this.dispatcher
        ) { message ->
            if (messageType.isInstance(message)) {
                action(message as TMessage)
            }
        }

        subscriptions.putIfAbsent(subscription.id, subscription)

        return subscription
    }

    /* PENDING CONVERSION TO KOTLIN

    /// <summary>
    ///     Subscribe to a message type, which is a derived type from <see cref="T" />
    /// </summary>
    /// <param name="predicate">Additional filter upon the typed message</param>
    /// <param name="action">Synchronous message handler</param>
    /// <param name="dispatcher">Optional: the dispatcher, will use <see cref="Dispatchers.SynchronousDispatcher" /> by default</param>
    /// <returns>A new subscription that can be used to unsubscribe</returns>
    public EventStreamSubscription<T> Subscribe<TMsg>(
        Func<TMsg, bool> predicate,
        Action<TMsg> action,
        IDispatcher? dispatcher = null
        , [CallerMemberName] string? caller = null) where TMsg : T
    {
        var sub = new EventStreamSubscription<T>(
            this,
            dispatcher ?? Dispatchers.SynchronousDispatcher,
            msg =>
            {
                if (msg is TMsg typed && predicate(typed))
                {
                    action(typed);
                }

                return Task.CompletedTask;
            }
            ,caller ?? "Unknown");

        _subscriptions.TryAdd(sub.Id, sub);

        return sub;
    }

    /// <summary>
    ///     Subscribe to the specified message type, which is a derived type from <see cref="T" />
    /// </summary>
    /// <param name="context">The sender context to send from</param>
    /// <param name="pids">The target PIDs the message will be sent to</param>
    /// <returns>A new subscription that can be used to unsubscribe</returns>
    public EventStreamSubscription<T> Subscribe<TMsg>(ISenderContext context, params PID[] pids) where TMsg : T
    {
        var caller = pids.First().ToDiagnosticString().Split("/").Last();
        var sub = new EventStreamSubscription<T>(
            this,
            Dispatchers.SynchronousDispatcher,
            msg =>
            {
                if (msg is TMsg)
                {
                    foreach (var pid in pids)
                    {
                        context.Send(pid, msg);
                    }
                }

                return Task.CompletedTask;
            }
            ,caller ?? "Unknown");

        _subscriptions.TryAdd(sub.Id, sub);

        return sub;
    }

    /// <summary>
    ///     Subscribe to the specified message type, which is a derived type from <see cref="T" />
    /// </summary>
    /// <param name="action">Asynchronous message handler</param>
    /// <param name="dispatcher">Optional: the dispatcher, will use <see cref="Dispatchers.SynchronousDispatcher" /> by default</param>
    /// <param name="caller"></param>
    /// <returns>A new subscription that can be used to unsubscribe</returns>
    public EventStreamSubscription<T> Subscribe<TMsg>(Func<TMsg, Task> action, IDispatcher? dispatcher = null, [CallerMemberName] string? caller = null)
        where TMsg : T
    {
        var sub = new EventStreamSubscription<T>(
            this,
            dispatcher ?? Dispatchers.SynchronousDispatcher,
            msg => msg is TMsg typed ? action(typed) : Task.CompletedTask
            ,caller ?? "Unknown");

        _subscriptions.TryAdd(sub.Id, sub);

        return sub;
    }
     */

    suspend fun publish(message: T) {
        subscriptions.values.forEach { subscription ->
            try {
                subscription.action(message)
            } catch (e: Exception) {
                logger.error(e) { "Error while processing message in subscription '${subscription.name}'" }
                throw e
            }
        }
    }

    fun unsubscribe(subscriptionId: EventStreamSubscriptionId) {
        subscriptions.remove(subscriptionId)
    }

    fun unsubscribe(subscription: EventStreamSubscription<T>) {
        subscriptions.remove(subscription.id)
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified TMessage : T, T : Any> EventStream<T>.subscribe(
    name: String,
    channel: Channel<TMessage>,
    dispatcher: Dispatcher? = null
): EventStreamSubscription<T> = subscribe(name, channel, TMessage::class, dispatcher)

@Suppress("UNCHECKED_CAST")
inline fun <reified TMessage : T, T : Any> EventStream<T>.subscribe(
    name: String,
    noinline action: (TMessage) -> Unit,
    dispatcher: Dispatcher? = null
): EventStreamSubscription<T> = subscribe(name, action, TMessage::class, dispatcher)
