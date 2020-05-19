package controllers.worldRenderer.entities

import cache.definitions.UnderlayDefinition
import controllers.worldRenderer.Constants
import controllers.worldRenderer.helpers.GpuIntBuffer
import controllers.worldRenderer.helpers.ModelBuffers
import controllers.worldRenderer.helpers.ModelBuffers.Companion.FLAG_SCENE_BUFFER
import java.nio.IntBuffer

class TilePaint(
    var swHeight: Int = 0,
    var seHeight: Int = 0,
    var neHeight: Int = 0,
    var nwHeight: Int = 0,
    var swColor: Int,
    var seColor: Int,
    var neColor: Int,
    var nwColor: Int,
    var texture: Int,
    var rgb: Int,
    var underlayDefinition: UnderlayDefinition? = null
) {
    var bufferOffset: Int = 0
    var bufferLen: Int = 0
    var uvBufferOffset: Int = 0

    fun draw(modelBuffers: ModelBuffers, sceneX: Int, sceneY: Int) {
        val x: Int = sceneX * Constants.LOCAL_TILE_SIZE
        val y = 0
        val z: Int = sceneY * Constants.LOCAL_TILE_SIZE
        val b: GpuIntBuffer = modelBuffers.modelBufferUnordered
        modelBuffers.incUnorderedModels()
        b.ensureCapacity(13)
        val buffer: IntBuffer = b.buffer
        buffer.put(bufferOffset)
        buffer.put(uvBufferOffset)
        buffer.put(2)
        buffer.put(modelBuffers.targetBufferOffset)
        buffer.put(FLAG_SCENE_BUFFER)
        buffer.put(x).put(y).put(z)
        buffer.put(modelBuffers.calcPickerId(sceneX, sceneY, 30))
        buffer.put(-1).put(-1).put(-1).put(-1)

//        setSceneBufferOffset(modelBuffers.getTargetBufferOffset())
        modelBuffers.addTargetBufferOffset(2 * 3)
    }
}