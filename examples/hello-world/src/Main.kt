package hello.world

import proto.actor.Actor
import proto.actor.ActorSystem
import proto.actor.Context
import proto.actor.Props

fun main() {
    val system = ActorSystem.create()
    val props = Props.fromProducer { HelloActor() }
    val pid = system.root.spawn(props)

    system.root.send(pid, Hello("ProtoActor"))
    readln()
}

data class Hello(val who: String)

class HelloActor : Actor {
    override suspend fun Context.receive(message: Any) {
        if (message is Hello) {
            println("Hello ${message.who}")
        }
    }
}
