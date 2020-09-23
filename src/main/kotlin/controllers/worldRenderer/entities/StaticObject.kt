package controllers.worldRenderer.entities

import cache.definitions.ObjectDefinition
import utils.EventType

class StaticObject(
    objectDefinition: ObjectDefinition,
    private var model: Model,
    height: Int,
    type: Int,
    orientation: Int
) : Entity(objectDefinition, height, type, orientation) {

    override fun getModel(): Model {
        return model
    }

    fun setModel(model: Model) {
        this.model = model
        notifyObservers(EventType.SELECT)
    }
}