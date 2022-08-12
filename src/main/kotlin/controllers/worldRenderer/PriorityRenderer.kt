package controllers.worldRenderer

import controllers.worldRenderer.entities.Renderable
import controllers.worldRenderer.helpers.ModelBuffers

typealias GLBuffer = Int

interface PriorityRenderer {
    fun startAdding(modelBuffers: ModelBuffers)
    fun addRenderable(renderable: Renderable, modelBuffers: ModelBuffers, sceneX: Int, sceneY: Int, height: Int, objType: Int)
    fun finishAdding(modelBuffers: ModelBuffers)
    fun produceVertices(modelBuffers: ModelBuffers, uniformStructIn: GLBuffer, vertexOut: GLBuffer, uvOut: GLBuffer, pickerIdsOut: GLBuffer)
    fun destroy()
}
