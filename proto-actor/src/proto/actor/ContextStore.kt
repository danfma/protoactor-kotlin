package proto.actor

import kotlin.reflect.KClass

interface ContextStore {
    fun <T : Any> get(clazz: KClass<T>): T?
    fun <T : Any> set(clazz: KClass<T>, value: T?)
    fun <T : Any> remove(clazz: KClass<T>): Boolean

    companion object {
        fun create(): ContextStore = object : ContextStore {
            private val store = mutableMapOf<KClass<*>, Any?>()

            @Suppress("UNCHECKED_CAST")
            override fun <T : Any> get(clazz: KClass<T>): T? {
                return store[clazz] as? T
            }

            override fun <T : Any> set(clazz: KClass<T>, value: T?) {
                store[clazz] = value
            }

            override fun <T : Any> remove(clazz: KClass<T>): Boolean {
                if (!store.contains(clazz)) {
                    return false
                }

                store.remove(clazz)

                return true
            }
        }
    }
}

inline fun <reified T : Any> ContextStore.get(): T? = get(T::class)

inline fun <reified T : Any> ContextStore.put(value: T?) = set(T::class, value)

inline fun <reified T : Any> ContextStore.remove(): Boolean = remove(T::class)

