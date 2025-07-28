package proto.actor.exceptions

import proto.actor.messages.PID

class DeadLetterException(pid: PID) : Exception("$pid no longer exists")
