package controllers.worldRenderer.entities

import controllers.worldRenderer.helpers.ModelBuffers.Companion.FLAG_SCENE_BUFFER

interface Renderable {
    val computeObj: ComputeObj
    val renderFlags: Int get() = FLAG_SCENE_BUFFER
    val renderUnordered: Boolean
    val faceCount: Int
    val renderOffsetX: Int get() = 0
    val renderOffsetY: Int get() = 0
    val renderOffsetZ: Int get() = 0
}

enum class OrientationType(val id: Int) {
    STRAIGHT(0),
    DIAGONAL(0x100);
}
