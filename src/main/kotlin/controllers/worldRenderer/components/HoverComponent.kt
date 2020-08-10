package controllers.worldRenderer.components

import utils.EventType
import utils.Observable

interface Hoverable {
    var isHovered: Boolean
}

class HoverComponent: Hoverable {
    var observable: Observable<*>? = null
    override var isHovered: Boolean = false
        set(value) {
            if (value == isHovered) return
            field = value
            observable?.notifyObservers(EventType.HOVER)
        }
}