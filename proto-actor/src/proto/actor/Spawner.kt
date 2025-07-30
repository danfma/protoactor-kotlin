package proto.actor

import proto.actor.contexts.ActorContext
import proto.actor.exceptions.ProcessNameExistException
import proto.actor.messages.PID
import proto.actor.messages.Started
import proto.actor.processes.ActorProcess

interface Spawner {
    fun spawn(
        system: ActorSystem,
        name: String,
        props: Props,
        parent: PID,
        callback: (Context.() -> Unit)? = null
    ): PID

    companion object {
        val default = object : Spawner {
            override fun spawn(
                system: ActorSystem,
                name: String,
                props: Props,
                parent: PID,
                callback: (Context.() -> Unit)?
            ): PID {
                if (system.isShuttingDown) {
                    return system.deadLetterPid
                }

                // Ordering is important here
                // first we create a mailbox and attach it to a process
                val props = system.configureProps(props)
                val mailbox = props.createMailbox()
                val dispatcher = props.dispatcher
                val process = ActorProcess(system, mailbox)

                // then we register it to the process registry
                val (self, absent) = system.processRegistry.tryAdd(name, process)

                // if this fails we exit and the process and mailbox is Garbage Collected
                if (!absent) {
                    throw ProcessNameExistException(name)
                }

                //if successful, we create the actor and attach it to the mailbox
                val context = ActorContext.setup(system, props, parent, self)

                mailbox.registerHandlers(context, dispatcher)
                callback?.invoke(context)
                mailbox.postSystemMessage(Started)
                mailbox.start()

                return self
            }
        }
    }
}
