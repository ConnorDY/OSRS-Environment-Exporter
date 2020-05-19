package controllers.worldRenderer.entities

import controllers.worldRenderer.SceneUploader
import controllers.worldRenderer.helpers.ModelBuffers

abstract class Renderable(
    var tag: Long = 0,
    var flags: Int = 0,
    var orientation: Int = 0,
    var sceneX: Int = 0,
    var sceneY: Int = 0,
    var x: Int = 0, // 3d world space position
    var y: Int = 0,
    var height: Int = 0
) {

    // opengl buffer offsets
    var bufferOffset = -1
    var uvBufferOffset = -1
    protected var sceneBufferOffset = -1
    protected var bufferLen = -1
    protected var pickerType = -1

    abstract fun draw(modelBuffers: ModelBuffers, sceneX: Int, sceneY: Int, sceneZ: Int)
    abstract fun drawDynamic(modelBuffers: ModelBuffers, sceneUploader: SceneUploader)
}