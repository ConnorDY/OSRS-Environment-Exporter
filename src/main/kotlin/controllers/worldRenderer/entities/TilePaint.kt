package controllers.worldRenderer.entities

import com.jogamp.opengl.GL
import com.jogamp.opengl.util.GLBuffers
import controllers.worldRenderer.Constants
import controllers.worldRenderer.SceneUploader
import controllers.worldRenderer.helpers.GpuFloatBuffer
import controllers.worldRenderer.helpers.GpuIntBuffer
import controllers.worldRenderer.helpers.ModelBuffers
import controllers.worldRenderer.helpers.ModelBuffers.Companion.FLAG_SCENE_BUFFER

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
    var rgb: Int
) : Renderable() {

    var computeObj: ComputeObj = ComputeObj()

    override fun draw(modelBuffers: ModelBuffers, sceneX: Int, sceneY: Int, height: Int, objType: Int) {
        val x: Int = sceneX * Constants.LOCAL_TILE_SIZE
        val z: Int = sceneY * Constants.LOCAL_TILE_SIZE
        val b: GpuIntBuffer = modelBuffers.modelBufferUnordered
        modelBuffers.incUnorderedModels()
        b.ensureCapacity(13)

        computeObj.idx = modelBuffers.targetBufferOffset
        computeObj.flags = FLAG_SCENE_BUFFER
        computeObj.x = x
        computeObj.z = z
        computeObj.pickerId = modelBuffers.calcPickerId(sceneX, sceneY, 30)
        b.buffer.put(computeObj.toArray())

        modelBuffers.addTargetBufferOffset(computeObj.size * 3)
    }

    override fun drawDynamic(modelBuffers: ModelBuffers, sceneX: Int, sceneY: Int, height: Int) {
        TODO("Not yet implemented")
    }

    fun recompute(modelBuffers: ModelBuffers) {
        val b: GpuIntBuffer = modelBuffers.modelBufferUnordered
        modelBuffers.incUnorderedModels()
        b.ensureCapacity(13)

        b.buffer.put(computeObj.toArray())
    }

    override fun clearDraw(modelBuffers: ModelBuffers) {
        val b: GpuIntBuffer = modelBuffers.modelBufferUnordered
        modelBuffers.incUnorderedModels()
        b.ensureCapacity(13)

        computeObj.x = Int.MAX_VALUE
        computeObj.y = Int.MAX_VALUE
        computeObj.z = Int.MAX_VALUE
        b.buffer.put(computeObj.toArray())
    }
}