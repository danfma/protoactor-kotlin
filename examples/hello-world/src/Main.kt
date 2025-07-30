package hello.world

import kotlinx.coroutines.runBlocking
import proto.actor.Actor
import proto.actor.ActorSystem
import proto.actor.Context
import proto.actor.Props

fun main() = runBlocking {
    ActorSystem.create().use { system ->
        val props = Props.fromProducer { HelloActor() }
        val pid = system.root.spawn(props)

        system.root.send(pid, Hello("ProtoActor"))
        system.root.waitStopOf(pid) // not necessary, but ensures the actor stops before the program exits

        println("Actor $pid has stopped")
    }
}

data class Hello(val who: String)

class HelloActor : Actor {
    override suspend fun Context.receive(message: Any) {
        println("Received message: $message")

        if (message is Hello) {
            println("Hello ${message.who}")
        }
    }
}
