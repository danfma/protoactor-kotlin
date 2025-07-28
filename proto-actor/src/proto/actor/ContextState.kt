package proto.actor

internal enum class ContextState {
    Alive,
    Restarting,
    Stopping,
    Stopped
}
