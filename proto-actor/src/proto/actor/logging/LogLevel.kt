package proto.actor.logging

import mu.KLogger

enum class LogLevel {
    Trace {
        override fun log(logger: KLogger, error: Throwable?, message: () -> String) {
            logger.trace(error, message)
        }
    },

    Debug {
        override fun log(logger: KLogger, error: Throwable?, message: () -> String) {
            logger.debug(error, message)
        }
    },

    Info {
        override fun log(logger: KLogger, error: Throwable?, message: () -> String) {
            logger.info(error, message)
        }
    },

    Warning {
        override fun log(logger: KLogger, error: Throwable?, message: () -> String) {
            logger.warn(error, message)
        }
    },

    Error {
        override fun log(logger: KLogger, error: Throwable?, message: () -> String) {
            logger.error(error, message)
        }
    },

    Critical {
        override fun log(logger: KLogger, error: Throwable?, message: () -> String) {
            logger.error(error, message)
            logger.exit()
        }
    },

    None {
        override fun log(logger: KLogger, error: Throwable?, message: () -> String) {
            // do nothing
        }
    };

    abstract fun log(logger: KLogger, error: Throwable?, message: () -> String)

    fun log(logger: KLogger, message: () -> String) =
        log(logger, null, message)
}

