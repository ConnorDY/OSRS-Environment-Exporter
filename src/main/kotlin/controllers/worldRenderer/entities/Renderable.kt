package controllers.worldRenderer.entities

import controllers.worldRenderer.helpers.ModelBuffers

interface Renderable {
    fun draw(modelBuffers: ModelBuffers, sceneX: Int, sceneY: Int, height: Int, objType: Int)
}

enum class OrientationType(val id: Int) {
    STRAIGHT(0),
    DIAGONAL(0xFF);
}
