package proto.actor

import proto.actor.messages.PID

interface SpawnerContext : SystemContext {
    fun spawnNamed(props: Props, name: String, callback: ((Context) -> Unit)? = null): PID

    fun spawn(props: Props) = spawnNamed(props, "")
}
