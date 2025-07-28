package proto.actor

import proto.actor.messages.PID
import kotlin.reflect.KClass

interface SenderContext : InfoContext {
    val message: Any
    val header: MessageHeader

    fun send(target: PID, message: Any)

    fun request(target: PID, message: Any, sender: PID)

    suspend fun <T : Any> requestAndWait(
        responseType: KClass<T>,
        target: PID,
        message: Any
    ): T

    fun getFuture(): Future
}

suspend inline fun <reified T : Any> SenderContext.requestAndWait(
    target: PID,
    message: Any
): T = requestAndWait(T::class, target, message)

