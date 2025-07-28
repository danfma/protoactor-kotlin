package proto.actor.stream

import kotlin.uuid.Uuid

@JvmInline
value class EventStreamSubscriptionId(val id: Uuid) {
    companion object {
        fun random() = EventStreamSubscriptionId(Uuid.random())
    }
}
