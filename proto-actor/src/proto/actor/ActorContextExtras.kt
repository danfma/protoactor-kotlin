package proto.actor

import proto.actor.messages.PID

class ActorContextExtras(val context: Context) : AutoCloseable {
    var children: List<PID> = emptyList()
        private set

    var watchers: List<PID> = emptyList()
        private set

    var restartStatistics: RestartStatistics = RestartStatistics(0)
        private set

    var receiveTimeoutTimer: PeriodicTimer? = null
        private set

    override fun close() {
        receiveTimeoutTimer?.close()
        receiveTimeoutTimer = null
    }

    fun setReceiveTimeoutTimer(timer: PeriodicTimer) {
        receiveTimeoutTimer?.close()
        receiveTimeoutTimer = timer
    }

    fun stopReceiveTimeoutTimer() {
        receiveTimeoutTimer?.stop()
    }

    fun resetReceiveTimeoutTimer() {
        receiveTimeoutTimer?.reset()
    }

    fun cancelReceiveTimeoutTimer() {
        receiveTimeoutTimer?.close()
        receiveTimeoutTimer = null
    }

    fun addChild(child: PID) {
        children = children + child
    }

    fun removeChild(child: PID) {
        children = children - child
    }

    fun watch(watcher: PID) {
        watchers = watchers + watcher
    }

    fun unwatch(watcher: PID) {
        watchers = watchers - watcher
    }
}


