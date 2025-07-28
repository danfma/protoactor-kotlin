package proto.actor

import proto.actor.messages.PID

interface InfoContext : ContextStore, SystemContext {
    val parent: PID
    val self: PID
    val sender: PID?
    val actor: Actor
}
