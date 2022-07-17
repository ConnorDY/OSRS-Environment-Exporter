package controllers.worldRenderer.entities

import cache.definitions.ObjectDefinition

abstract class Entity(
    val objectDefinition: ObjectDefinition,
    var height: Int = 0,
    val type: Int,
    val orientation: Int
) {
    abstract fun getModel(): Model

    override fun toString(): String {
        return "$javaClass, Height: $height, type $type, orientation $orientation, model ${getModel()}, objectDefinition: $objectDefinition"
    }
}
