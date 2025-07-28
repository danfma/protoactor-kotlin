package proto.actor

import mu.KLogger
import proto.actor.extensions.waitUpTo
import kotlin.time.Duration

class DeadlineContextDecorator(
    val context: Context,
    val deadline: Duration,
    val logger: KLogger,
    val props: Props
) : ActorContextDecorator(context) {
    override suspend fun receive(envelope: MessageEnvelope) {
        val deferred = props.dispatcher.defer {
            super.receive(envelope)
        }

        if (deferred.isCompleted) {
            return
        }

        val ok = deferred.waitUpTo(deadline)

        if (!ok) {
            logger.warn { "Actor $self deadline $deadline, exceeded for message: ${envelope.message}" }
        }
    }
}
