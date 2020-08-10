package controllers.worldRenderer.components

import utils.EventType
import utils.Observable

interface Clickable {
    fun onClick()
}

class ClickableComponent: Clickable {
    var observable: Observable<*>? = null
    var onClickFunc: (() -> Unit)? = null

    override fun onClick() {
        observable?.notifyObservers(EventType.CLICK)
        if (onClickFunc != null) onClickFunc!!()
    }
}