package proto.actor.fixture

import proto.actor.Context


val EmptyReceive: suspend Context.(msg: Any) -> Unit = { }

