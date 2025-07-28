package proto.actor.stream

import proto.actor.Dispatcher

class EventStreamSubscription<T : Any>(
    private val eventStream: EventStream<T>,
    val name: String,
    val dispatcher: Dispatcher,
    val action: suspend (T) -> Unit
) {
    val id = EventStreamSubscriptionId.random()

    fun unsubscribe() {
        eventStream.unsubscribe(id)
    }
}
