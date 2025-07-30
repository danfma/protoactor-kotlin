package actor.proto.router

import proto.actor.Props

internal class BroadcastPoolRouterConfig(poolSize: Int, routeeProps: Props) : PoolRouterConfig(poolSize, routeeProps) {
    override fun createRouterState(): RouterState = BroadcastRouterState()
}

