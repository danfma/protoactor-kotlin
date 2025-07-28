package proto.actor.fixture

import proto.actor.Actor
import proto.actor.Context

class DoNothingActor : Actor {
    suspend override fun Context.receive(message: Any) {}
}

