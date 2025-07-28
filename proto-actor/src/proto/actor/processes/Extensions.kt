package proto.actor.processes

import proto.actor.Process

fun Process.configure() {
    system.config.configureProcess(this)
}
