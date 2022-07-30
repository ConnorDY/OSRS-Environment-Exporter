package controllers.worldRenderer.entities

import cache.definitions.ObjectDefinition

interface Entity {
    val objectDefinition: ObjectDefinition
    val height: Int
    val type: Int
    val orientation: Int
    val model: Model
}
