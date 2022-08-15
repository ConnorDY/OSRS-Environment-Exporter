package controllers.worldRenderer.entities

import cache.definitions.ObjectDefinition

class StaticObject(
    override val objectDefinition: ObjectDefinition,
    override val model: Model,
    override val height: Int,
    override val type: Int,
) : Entity {
    override fun toString(): String {
        return "$javaClass, Height: $height, type $type, model $model, objectDefinition: $objectDefinition"
    }
}
