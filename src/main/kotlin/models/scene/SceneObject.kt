package models.scene

import cache.definitions.ObjectDefinition

// Represents an object to be placed in the scene
// contains the 3 fields required to get it's model
data class SceneObject(
    val objectDefinition: ObjectDefinition,
    val type: Int,
    val orientation: Int
)