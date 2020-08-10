package utils

enum class EventType {
    HOVER,
    SELECT,
    CLICK
}

abstract class Observable<T> {
    private var observers: ArrayList<(Pair<T, EventType>) -> Unit> = ArrayList()

    fun addListener(onChange: (Pair<T, EventType>) -> Unit) {
        observers.add(onChange)
    }

    @Suppress("UNCHECKED_CAST")
    fun notifyObservers(eventType: EventType) {
        observers.forEach { it(Pair(this as T, eventType)) }
    }
}