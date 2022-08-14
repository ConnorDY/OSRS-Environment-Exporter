package utils

import java.util.function.Consumer

class ObservableValue<T>(initial: T) {
    private var backing = initial
    private val earlyListeners = ArrayList<Consumer<in T>>()
    private val listeners = ArrayList<Consumer<in T>>()

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

    fun addListener(listener: Consumer<in T>) {
        listeners.add(listener)
    }
}
