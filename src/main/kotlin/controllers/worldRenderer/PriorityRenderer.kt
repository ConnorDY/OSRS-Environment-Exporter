package controllers.worldRenderer

import controllers.worldRenderer.entities.Renderable
import java.nio.FloatBuffer
import java.nio.IntBuffer

typealias GLBuffer = Int

interface PriorityRenderer {
    val needsStrictUVs: Boolean
    fun beginUploading()
    fun getBuffersForRenderable(renderable: Renderable, faces: Int, hasUVs: Boolean): Pair<IntBuffer, FloatBuffer>
    fun finishUploading()
    fun positionRenderable(renderable: Renderable, sceneX: Int, sceneY: Int, height: Int, objType: Int)
    fun finishPositioning()
    fun produceVertices(camera: Camera, currFrame: Int)
    fun draw()
    fun destroy()
    fun bindVao()
}
