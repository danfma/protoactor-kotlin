package proto.actor.messages

import proto.actor.Context

interface AutoRespond {
    fun getAutoResponse(context: Context): Any
}
