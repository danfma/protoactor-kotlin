package proto.actor

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import mu.KotlinLogging
import kotlin.time.Duration.Companion.milliseconds

typealias ContextDecorator = (Context) -> Context

@Serializable
data class ActorSystemConfig(
    val dispatcher: Dispatcher = Dispatcher.default,
    val deadLetterThrottleInterval: Long = 0,
    val deadLetterThrottleCount: UInt = 0u,
    val deadLetterRequestLogging: Boolean = false,
    val metricsEnabled: Boolean = false,
    val sharedFutures: Boolean = true,
    val sharedFutureSize: UInt = 5000u,
    val threadPoolStatsTimeout: Long = 1000,
    val developerSupervisionLogging: Boolean = false,
    @Transient val configureRootContext: (RootContext) -> RootContext = { it },
    @Transient val configureProps: (Props) -> Props = { it },
    @Transient val configureSystemProps: (String, Props) -> Props = defaultConfigureSystemProps,
    @Transient var configureProcess: (Process) -> Process = { it }
) {
    companion object {
        val default
            get() = ActorSystemConfig()

        private val defaultConfigureSystemProps: (String, Props) -> Props = { _, props ->
            val logger = KotlinLogging.logger("Proto.SystemActors")

            props
                .withDeadlineDecorator(100.milliseconds, logger)
                .withLoggingContextDecorator(logger)
                .withGuardianSupervisorStrategy(Supervision.defaultStrategy)
        }
    }
}
