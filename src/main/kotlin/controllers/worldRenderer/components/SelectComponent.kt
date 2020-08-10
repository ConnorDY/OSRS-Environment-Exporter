package controllers.worldRenderer.components

import utils.EventType
import utils.Observable

interface Selectable {
    var isSelected: Boolean
}

class SelectComponent: Selectable {
    var observable: Observable<*>? = null
    override var isSelected: Boolean = false
        set(value) {
            if (value == isSelected) return
            field = value
            observable?.notifyObservers(EventType.SELECT)
        }
}