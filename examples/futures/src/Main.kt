package futures

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import proto.actor.ActorSystem
import proto.actor.Props
import proto.actor.requestAndWait

fun main() = runBlocking {
    val logger = KotlinLogging.logger { }

    ActorSystem.create().use { system ->
        val root = system.root

        val pid = root.spawn(
            Props.fromFunction {
                logger.info("Actor $self received $message")

                if (message is String) {
                    respond("hey")
                }
            }
        )

        logger.info("Spawned actor with PID: $pid")
        logger.info("Sending 'hello' to actor...")

        val reply = root.requestAndWait<String>(pid, "hello")

        logger.info("Reply: $reply")

        system.shutdownAndWait()

        logger.info("Actor system has been shut down.")
    }
}
