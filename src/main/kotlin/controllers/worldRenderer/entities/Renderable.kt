package controllers.worldRenderer.entities

import controllers.worldRenderer.helpers.ModelBuffers

abstract class Renderable(
    var orientation: Int = 0,
    var orientationType: OrientationType = OrientationType.STRAIGHT,
    var x: Int = 0, // 3d world space position
    var y: Int = 0,
    var yOff: Int = 0,
    var xOff: Int = 0,
    var height: Int = 0
) {
    abstract fun draw(modelBuffers: ModelBuffers, sceneX: Int, sceneY: Int, height: Int, objType: Int)
    abstract fun drawDynamic(modelBuffers: ModelBuffers, sceneX: Int, sceneY: Int, height: Int)
    abstract fun clearDraw(modelBuffers: ModelBuffers)
}

enum class OrientationType(val id: Int) {
    STRAIGHT(0),
    DIAGONAL(0xFF);
}