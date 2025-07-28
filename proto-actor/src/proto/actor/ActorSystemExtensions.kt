package proto.actor

class ActorSystemExtensions(private val system: ActorSystem) {
    private val extensions = mutableMapOf<String, ActorSystemExtension>()

    @Suppress("UNCHECKED_CAST")
    fun <T : ActorSystemExtension> get(name: String): T? {
        return extensions[name] as? T
    }

    fun <T : ActorSystemExtension> getRequired(name: String, notFoundMessage: String? = null): T {
        return get(name) ?: throw IllegalStateException(
            notFoundMessage ?: "ActorSystem extension '$name' not found"
        )
    }

    fun register(name: String, extension: ActorSystemExtension) {
        if (extensions.containsKey(name)) {
            throw IllegalStateException("ActorSystem extension '$name' is already registered")
        }

        extensions[name] = extension
    }

    fun getAll(): Iterable<ActorSystemExtension> {
        return extensions.values
    }
}

