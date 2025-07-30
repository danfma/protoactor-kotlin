package futures

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import proto.actor.*
import proto.actor.messages.PID
import proto.actor.messages.Started
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.Instant

fun main() = runBlocking {
    val logger = KotlinLogging.logger { }

    ActorSystem.create().use { system ->
        val root = system.root

        val gamePid = root.spawnNamed(
            Props.fromProducer {
                GameActor()
            },
            "ping-pong-game"
        )

        root.requestAndWait<BenchmarkCompleted>(gamePid, StartBenchmark)
        logger.info("Benchmark finished")
        system.shutdownAndWait()
        logger.info("Actor system has been shut down.")
    }
}

data object StartBenchmark

data object BenchmarkCompleted

data class SetChallenger(val challenger: PID)

data class PingPong(val start: Instant, var count: Long) {
    val duration = Clock.System.now() - start

    fun expired(now: Instant = Clock.System.now()) = (now - start).inWholeSeconds > 10
    fun next() = copy(count = count + 1)

    companion object {
        fun start() = PingPong(Clock.System.now(), 0L)
    }
}

class GameActor : Actor {
    private lateinit var pingPid: PID
    private lateinit var pongPid: PID
    private var starterPid: PID? = null

    override suspend fun Context.receive(message: Any) {
        when (message) {
            is Started -> {
                pingPid = spawnNamed(
                    Props.fromProducer { PingPongActor() },
                    "ping"
                )

                pongPid = spawnNamed(
                    Props.fromProducer { PingPongActor() },
                    "pong"
                )

                send(pingPid, SetChallenger(pongPid))
                send(pongPid, SetChallenger(pingPid))
            }

            is StartBenchmark -> {
                starterPid = sender
                logger.info("Starting benchmark...")
                send(pingPid, PingPong.start())
            }

            is PingPong -> {
                val duration = message.duration

                logger.info("Messages exchanged: ${message.count} in $duration")
                logger.info("Messages per second: ${message.count / duration.toDouble(DurationUnit.SECONDS)}")

                if (starterPid != null) {
                    send(starterPid!!, BenchmarkCompleted)
                }
            }
        }
    }

    companion object {
        val logger = KotlinLogging.logger { }
    }
}

class PingPongActor : Actor {
    private lateinit var challengerPid: PID

    override suspend fun Context.receive(message: Any) {
        when (message) {
            is SetChallenger -> {
                challengerPid = message.challenger
            }

            is PingPong if (!message.expired()) -> {
                send(challengerPid, message.next())
            }

            is PingPong -> forward(parent)
        }
    }
}
