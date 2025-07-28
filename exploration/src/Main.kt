package exploration

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.LinkedBlockingQueue
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

interface SystemMessage

data class Text(val content: String)

data object Ping : SystemMessage

interface Actor {
    suspend fun receive(msg: Any)
}

class MyActor : Actor {
    private var lastThreadId = 0L

    override suspend fun receive(msg: Any) {
        val threadId = Thread.currentThread().id

        if (threadId != lastThreadId) {
            println("MyActor is processing on thread $threadId")
            lastThreadId = threadId
        }

        println("\t- Received message $msg")
    }
}

class Mailbox {
    private val userMessages = LinkedBlockingQueue<Any>()
    private val systemMessages = LinkedBlockingQueue<Any>()

    val messages = sequence {
        while (systemMessages.isNotEmpty() || userMessages.isNotEmpty()) {
            systemMessages.poll()?.let { yield(it) }
            userMessages.poll()?.let { yield(it) }
        }
    }

    fun put(message: Any): Boolean = when (message) {
        is SystemMessage -> systemMessages.offer(message)
        else -> userMessages.offer(message)
    }
}

class ActorProcess(val actor: Actor, val mailbox: Mailbox, val dispatcher: Dispatcher) {
    private val channel = Channel<Any>(capacity = Channel.UNLIMITED)

    init {
        dispatcher.scope.async {
            for (deliver in channel) {
                mailbox.messages.forEach { message ->
                    try {
                        actor.receive(message)
                    } catch (e: Exception) {
                        println("Error processing message: $message, error: ${e.message}")
                        // do something with the error (recovery, logging, etc.)
                    }
                }
            }
        }
    }

    suspend fun execute() {
        channel.send(Deliver)
    }

    data object Deliver
}

interface Dispatcher {
    val scope: CoroutineScope
    fun dispatch(message: Any, process: ActorProcess)
}

class DefaultDispatcher(
    context: CoroutineContext = Dispatchers.Default
) : Dispatcher {
    override val scope: CoroutineScope = CoroutineScope(context) + SupervisorJob()

    override fun dispatch(message: Any, process: ActorProcess) {
        println("Adding message to mailbox: $message")
        process.mailbox.put(message)

        scope.launch {
            process.execute()
        }
    }
}

fun main() = runBlocking {
    val actor = MyActor()
    val mailbox = Mailbox()
    val dispatcher = DefaultDispatcher()
    val actorProcess = ActorProcess(actor, mailbox, dispatcher)

    // NOT THE FINAL USAGE
    launch {
        for (i in 'A'..'Z') {
            dispatcher.dispatch(Text("A$i"), actorProcess)
            delay(1.milliseconds)
        }
    }

    launch {
        for (i in 'A'..'Z') {
            dispatcher.dispatch(Text("B$i"), actorProcess)
            delay(1.milliseconds)
        }
    }

    launch {
        for (i in 1..5) {
            dispatcher.dispatch(Ping, actorProcess)
            delay(4.milliseconds)
        }
    }

    delay(2.seconds)
}
