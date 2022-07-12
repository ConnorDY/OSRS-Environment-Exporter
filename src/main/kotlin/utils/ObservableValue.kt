package utils

class ObservableValue<T>(initial: T) {
    private var backing = initial
    private val listeners = ArrayList<(T) -> Unit>()

    fun get() = backing
    fun set(value: T) {
        if (backing !== value) {
            backing = value
            listeners.forEach { it.invoke(value) }
        }
    }

    fun addListener(listener: (T) -> Unit) {
        listeners.add(listener)
    }
}
