package proto.actor.logging

import kotlinx.coroutines.Deferred
import mu.KLogger
import proto.actor.*
import proto.actor.extensions.describe
import proto.actor.extensions.getClassName
import proto.actor.messages.InfrastructureMessage
import proto.actor.messages.PID
import proto.actor.messages.Terminated
import proto.actor.messages.Touch
import kotlin.reflect.KClass

class ActorLoggingContext(
    val context: Context, val logger: KLogger,
    val logLevel: LogLevel,
    val infrastructureLogLevel: LogLevel,
    val exceptionLogLevel: LogLevel,
) : ActorContextDecorator(context) {
    private val actorType = actor.javaClass.simpleName ?: "UnknownActor"

    override suspend fun receive(envelope: MessageEnvelope) {
        val message = envelope.message
        val logLevel = getLogLevel(message)

        logLevel.log(logger) {
            "Actor $self $actorType received message ${message.getClassName()}:$message from ${sender.describe()}"
        }

        try {
            super.receive(envelope)

            logLevel.log(logger) {
                "Actor $self $actorType completed message ${message.getClassName()}:$message from ${sender.describe()}"
            }

        } catch (e: Exception) {
            logLevel.log(logger, e) {
                "Actor $self $actorType failed during message ${message.getClassName()}:$message from ${sender.describe()}"
            }
        }
    }

    override fun <T> reenterAfter(deferred: Deferred<T>, action: suspend Context.(Deferred<T>) -> Unit) {
        logLevel.log(logger) {
            "Actor $self $actorType reenterAfter $deferred"
        }

        super.reenterAfter(deferred, action)
    }

    override fun request(target: PID, message: Any, sender: PID) {
        logLevel.log(logger) {
            "Actor $self $actorType request $target with message ${message.getClassName()}:$message from ${sender.describe()}"
        }

        super.request(target, message, sender)
    }

    override suspend fun <T : Any> requestAndWait(responseType: KClass<T>, target: PID, message: Any): T {
        logLevel.log(logger) {
            "Actor $self $actorType requestAndWait $target with message ${message.getClassName()}:$message waiting for a message of type ${responseType.simpleName}"
        }

        return try {
            val response = super.requestAndWait(responseType, target, message)

            logLevel.log(logger) {
                "Actor $self $actorType got response of type ${responseType.simpleName} from $target: $response"
            }

            response
        } catch (e: Exception) {
            exceptionLogLevel.log(logger, e) {
                "Actor $self $actorType failed to get response of type ${responseType.simpleName} from $target with message ${message.getClassName()}:$message"
            }

            throw e
        }
    }

    override fun send(target: PID, message: Any) {
        logLevel.log(logger) {
            "Actor $self $actorType sending message ${message.getClassName()}:$message to $target"
        }

        try {
            super.send(target, message)

            logLevel.log(logger) {
                "Actor $self $actorType successfully sent message ${message.getClassName()}:$message to $target"
            }
        } catch (e: Exception) {
            exceptionLogLevel.log(logger, e) {
                "Actor $self $actorType failed to send message ${message.getClassName()}:$message to $target"
            }
        }
    }

    override fun respond(message: Any, header: MessageHeader) {
        val logLevel = getLogLevel(message)

        logLevel.log(logger) {
            "Actor $self $actorType responding with message ${message.getClassName()}:$message to ${sender.describe()}"
        }

        try {
            super.respond(message, header)

            logLevel.log(logger) {
                "Actor $self $actorType successfully responded with message ${message.getClassName()}:$message to ${sender.describe()}"
            }
        } catch (e: Exception) {
            exceptionLogLevel.log(logger, e) {
                "Actor $self $actorType failed to respond with message ${message.getClassName()}:$message to ${sender.describe()}"
            }
        }
    }

    override fun forward(target: PID) {
        logLevel.log(logger) {
            "Actor $self $actorType forwarding message to $target"
        }

        try {
            super.forward(target)

            logLevel.log(logger) {
                "Actor $self $actorType successfully forwarded message ${message.getClassName()} to $target"
            }
        } catch (e: Exception) {
            exceptionLogLevel.log(logger, e) {
                "Actor $self $actorType failed to forward message to $target"
            }
        }
    }

    override fun spawnNamed(
        props: Props,
        name: String,
        callback: ((Context) -> Unit)?
    ): PID {
        try {
            logLevel.log(logger) {
                "Actor $self $actorType spawning named actor with name '$name' and props $props"
            }

            val child = super.spawnNamed(props, name, callback)

            logLevel.log(logger) {
                "Actor $self $actorType spawned named actor with name '$name' with PID: $child"
            }

            return child
        } catch (e: Exception) {
            exceptionLogLevel.log(logger, e) {
                "Actor $self $actorType failed to spawn named actor with name '$name' and props $props"
            }
            throw e
        }
    }

    override fun watch(pid: PID) {
        logLevel.log(logger) {
            "Actor $self $actorType is requesting to watch PID: $pid"
        }

        try {
            super.watch(pid)

            logLevel.log(logger) {
                "Actor $self $actorType is watching PID: $pid"
            }
        } catch (e: Exception) {
            exceptionLogLevel.log(logger, e) {
                "Actor $self $actorType failed to watch PID: $pid"
            }
        }
    }

    override fun unwatch(pid: PID) {
        logLevel.log(logger) {
            "Actor $self $actorType is requesting to unwatch PID: $pid"
        }

        try {
            super.unwatch(pid)

            logLevel.log(logger) {
                "Actor $self $actorType is no longer watching PID: $pid"
            }
        } catch (e: Exception) {
            exceptionLogLevel.log(logger, e) {
                "Actor $self $actorType failed to unwatch PID: $pid"
            }
        }
    }

    private fun getLogLevel(message: Any): LogLevel {
        return when (message) {
            is Terminated, is Touch -> LogLevel.None
            is InfrastructureMessage -> infrastructureLogLevel
            else -> logLevel
        }
    }
}
