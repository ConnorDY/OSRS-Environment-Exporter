package controllers.worldRenderer.entities

import controllers.worldRenderer.Constants
import controllers.worldRenderer.helpers.GpuIntBuffer
import controllers.worldRenderer.helpers.ModelBuffers
import controllers.worldRenderer.helpers.ModelBuffers.Companion.FLAG_SCENE_BUFFER

class TilePaint(
    val swHeight: Int,
    val seHeight: Int,
    val neHeight: Int,
    val nwHeight: Int,
    val swColor: Int,
    val seColor: Int,
    val neColor: Int,
    val nwColor: Int,
    val texture: Int,
    val rgb: Int,
) : Renderable {
    internal val computeObj = ComputeObj()

    override fun draw(modelBuffers: ModelBuffers, sceneX: Int, sceneY: Int, height: Int, objType: Int) {
        val x: Int = sceneX * Constants.LOCAL_TILE_SIZE
        val z: Int = sceneY * Constants.LOCAL_TILE_SIZE
        val b: GpuIntBuffer = modelBuffers.modelBufferUnordered
        modelBuffers.incUnorderedModels()
        b.ensureCapacity(13)

        computeObj.idx = modelBuffers.targetBufferOffset
        computeObj.flags = FLAG_SCENE_BUFFER
        computeObj.x = x
        computeObj.y = height
        computeObj.z = z
        computeObj.pickerId = modelBuffers.calcPickerId(sceneX, sceneY, objType)
        b.buffer.put(computeObj.toArray())

        modelBuffers.addTargetBufferOffset(computeObj.size * 3)
    }
}
