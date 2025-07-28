package proto.actor

abstract class ActorContextDecorator(private val context: Context) : Context by context
