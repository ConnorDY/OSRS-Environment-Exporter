package utils

class ObservableValue<T>(initial: T) {
    private var backing = initial
    private val earlyListeners = ArrayList<(T) -> Unit>()
    private val listeners = ArrayList<(T) -> Unit>()

    fun get() = backing
    fun set(value: T) {
        if (backing !== value) {
            backing = value
            earlyListeners.forEach { it.invoke(value) }
            listeners.forEach { it.invoke(value) }
        }
    }

    fun addEarlyListener(listener: (T) -> Unit) {
        earlyListeners.add(listener)
    }

    fun addListener(listener: (T) -> Unit) {
        listeners.add(listener)
    }
}
