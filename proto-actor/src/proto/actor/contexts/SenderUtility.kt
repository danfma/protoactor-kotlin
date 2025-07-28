package proto.actor.contexts

import mu.KotlinLogging
import proto.actor.MessageEnvelope
import proto.actor.SenderContext
import proto.actor.exceptions.DeadLetterException
import proto.actor.messages.DeadLetterResponse
import proto.actor.messages.PID
import kotlin.reflect.KClass

object SenderUtility {
    private val logger = KotlinLogging.logger { }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> requestAndWait(
        sender: SenderContext,
        responseType: KClass<T>,
        target: PID,
        message: Any
    ): T {
        return sender.getFuture().use { future ->
            val envelope = when (message) {
                is MessageEnvelope -> message.copy(sender = future.pid)
                else -> MessageEnvelope(message, future.pid)
            }

            sender.send(target, envelope)

            val result = future.getDeferred()
            val messageResult = MessageEnvelope.unwrapMessage(result)

            if (messageResult is DeadLetterResponse) {
                if (sender.system.config.deadLetterRequestLogging) {
                    logger.warn { "${sender.self} received dead letter response for request to $target with message $message" }
                }

                throw DeadLetterException(target)
            }

            if (responseType.isInstance(messageResult)) {
                messageResult as T
            } else if (responseType == MessageEnvelope::class) {
                MessageEnvelope.wrap(result) as T
            } else {
                throw IllegalStateException("Unexpected message. Expected: $responseType, actual: ${messageResult::class}")
            }
        }
    }
}
