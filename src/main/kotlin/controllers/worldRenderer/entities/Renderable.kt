package controllers.worldRenderer.entities

import controllers.worldRenderer.Constants
import controllers.worldRenderer.helpers.GpuIntBuffer
import controllers.worldRenderer.helpers.ModelBuffers
import controllers.worldRenderer.helpers.ModelBuffers.Companion.FLAG_SCENE_BUFFER
import kotlin.math.min

interface Renderable {
    val computeObj: ComputeObj
    val renderFlags: Int get() = FLAG_SCENE_BUFFER
    val renderUnordered: Boolean
    val faceCount: Int
    val renderOffsetX: Int get() = 0
    val renderOffsetY: Int get() = 0
    val renderOffsetZ: Int get() = 0
    fun draw(modelBuffers: ModelBuffers, sceneX: Int, sceneY: Int, height: Int, objType: Int) {
        val b: GpuIntBuffer =
            if (renderUnordered) modelBuffers.modelBufferUnordered
            else modelBuffers.bufferForTriangles(min(ModelBuffers.MAX_TRIANGLE, faceCount))
        if (renderUnordered) modelBuffers.incUnorderedModels()
        b.ensureCapacity(13)

        computeObj.idx = modelBuffers.targetBufferOffset
        computeObj.flags = renderFlags
        computeObj.x = sceneX * Constants.LOCAL_TILE_SIZE + renderOffsetX
        computeObj.y = height + renderOffsetY
        computeObj.z = sceneY * Constants.LOCAL_TILE_SIZE + renderOffsetZ
        computeObj.pickerId = modelBuffers.calcPickerId(sceneX, sceneY, objType)
        b.buffer.put(computeObj.toArray())

        modelBuffers.addTargetBufferOffset(computeObj.size * 3)
    }
}

enum class OrientationType(val id: Int) {
    STRAIGHT(0),
    DIAGONAL(0xFF);
}
