package controllers.worldRenderer.entities

import cache.definitions.ObjectDefinition

interface Entity {
    val objectDefinition: ObjectDefinition
    val height: Int
    val type: Int
    val model: Model
}
