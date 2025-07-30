package proto.actor

import mu.KotlinLogging
import proto.actor.stream.EventStream


class ActorSystemEventStream(system: ActorSystem) : EventStream<Any>(system.config.dispatcher, logger) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
