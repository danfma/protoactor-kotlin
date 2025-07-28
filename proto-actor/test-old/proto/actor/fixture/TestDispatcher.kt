package proto.actor.fixture

import proto.actor.mailbox.Dispatcher
import proto.actor.mailbox.Mailbox

class TestDispatcher : Dispatcher {
    override var throughput: Int = 10
    override fun schedule(mailbox: Mailbox) {
    }
}

