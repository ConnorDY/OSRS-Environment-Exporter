package utils

import java.util.function.Consumer

class ObservableValue<T>(initial: T) {
    private var backing = initial
    private val earlyListeners = ArrayList<Consumer<in T>>()
    private val listeners = ArrayList<Consumer<in T>>()
    private val removable = HashMap<Any, HashSet<Consumer<in T>>>()

    fun get() = backing
    fun set(value: T) {
        if (backing !== value) {
            backing = value
            earlyListeners.forEach { it.accept(value) }
            listeners.forEach { it.accept(value) }
        }
    }

    fun addEarlyListener(listener: Consumer<in T>) {
        earlyListeners.add(listener)
    }

    fun addEarlyListener(key: Any, listener: Consumer<in T>) {
        addRemovable(key, listener)
        addEarlyListener(listener)
    }

    fun addListener(listener: Consumer<in T>) {
        listeners.add(listener)
    }

    fun addListener(key: Any, listener: Consumer<in T>) {
        addRemovable(key, listener)
        addListener(listener)
    }

    private fun addRemovable(key: Any, listener: Consumer<in T>) {
        removable.getOrPut(key) { HashSet() }.add(listener)
    }

    fun removeListeners(key: Any) {
        removable.remove(key)?.let { removing ->
            earlyListeners.removeAll(removing)
            listeners.removeAll(removing)
        }
    }
}
