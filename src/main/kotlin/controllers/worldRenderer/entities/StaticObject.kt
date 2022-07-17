package controllers.worldRenderer.entities

import cache.definitions.ObjectDefinition

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
}
