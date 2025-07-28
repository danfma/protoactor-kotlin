package proto.actor

data class MessageHeader(val map: Map<String, String> = emptyMap()) {
    fun isEmpty(): Boolean = map.isEmpty()

    fun getOrDefault(key: String, default: String): String =
        map.getOrDefault(key, default)

    fun put(key: String, value: String) =
        copy(map = map + (key to value))

    fun remove(key: String) =
        copy(map = map - key)

    fun containsKey(key: String): Boolean =
        map.containsKey(key)

    fun containsValue(value: String): Boolean =
        map.containsValue(value)

    companion object {
        val empty = MessageHeader()
    }
}

