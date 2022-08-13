package controllers.worldRenderer

import controllers.worldRenderer.entities.Renderable
import controllers.worldRenderer.helpers.GpuFloatBuffer
import controllers.worldRenderer.helpers.ModelBuffers

typealias GLBuffer = Int

interface PriorityRenderer {
    fun finishUploading()
    fun positionRenderable(renderable: Renderable, sceneX: Int, sceneY: Int, height: Int, objType: Int)
    fun finishPositioning()
    fun produceVertices(camera: Camera, currFrame: Int)
    fun draw()
    fun destroy()
    fun unsafeGetBuffers(): ModelBuffers
}
