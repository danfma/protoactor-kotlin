package proto.actor

import mu.KotlinLogging
import proto.actor.stream.EventStream


class ActorSystemEventStream(private val system: ActorSystem) : EventStream<Any>(logger) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }
}


