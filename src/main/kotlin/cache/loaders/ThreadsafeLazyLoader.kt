package cache.loaders

import java.nio.BufferUnderflowException

abstract class ThreadsafeLazyLoader<T> {
    private val cache: HashMap<Int, T?> = HashMap()

    open operator fun get(id: Int): T? {
        // Try first to retrieve from cache
        synchronized(cache) {
            val value = cache[id]
            if (value != null || cache.containsKey(id)) {
                return value
            }
        }

        // If not cached, try to generate the value
        val value = try {
            load(id)
        } catch (e: BufferUnderflowException) {
            e.printStackTrace() // Alert an attentive user that an issue has occurred
            null
        }

        // ... and cache it
        synchronized(cache) {
            cache[id] = value
        }
        return value
    }

    protected abstract fun load(id: Int): T?
}
