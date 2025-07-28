package proto.actor

interface RootContext : SpawnerContext, SenderContext, StopperContext {
    fun withSenderMiddleware(vararg middlewares: SenderMiddleware): RootContext
}
