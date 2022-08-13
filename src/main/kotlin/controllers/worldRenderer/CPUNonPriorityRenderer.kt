package controllers.worldRenderer

import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2ES3
import controllers.worldRenderer.entities.Renderable
import controllers.worldRenderer.helpers.GpuFloatBuffer
import controllers.worldRenderer.helpers.GpuIntBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer

class CPUNonPriorityRenderer(override val gl: GL2ES3) : AbstractPriorityRenderer(gl) {
    private val vertexBuffer = GpuIntBuffer()
    private val uvBuffer = GpuFloatBuffer()
    private var bufferedVertices = 0

    override val needsStrictUVs get() = true

    override fun beginUploading() {
        super.beginUploading()
        vertexBuffer.clear()
        uvBuffer.clear()
    }

    override fun getBuffersForRenderable(renderable: Renderable, faces: Int, hasUVs: Boolean): Pair<IntBuffer, FloatBuffer> {
        prepareBuffersForRenderable(renderable, faces, hasUVs, vertexBuffer, uvBuffer)
        return Pair(vertexBuffer.buffer, uvBuffer.buffer)
    }

    override fun finishUploading() {
        vertexBuffer.flip()
        uvBuffer.flip()
    }

    override fun positionRenderable(renderable: Renderable, sceneX: Int, sceneY: Int, height: Int, objType: Int) {
        val computeObj = renderable.computeObj
        if (!isUploaded(computeObj)) return

        super.positionRenderable(renderable, sceneX, sceneY, height, objType)

        val buffer = vertexBuffer.buffer
        val xOffset = computeObj.x
        val yOffset = computeObj.y
        val zOffset = computeObj.z
        val idx = computeObj.offset * VEC_DIMS

        for (vertex in 0 until computeObj.size * VERTICES_PER_TRI) {
            val i = vertex * VEC_DIMS + idx
            buffer.put(i, buffer.get(i) + xOffset)
            buffer.put(i + 1, buffer.get(i + 1) + yOffset)
            buffer.put(i + 2, buffer.get(i + 2) + zOffset)
        }
    }

    override fun finishPositioning() {
        super.finishPositioning()
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexOut)
        gl.glBufferData(GL.GL_ARRAY_BUFFER, vertexBuffer.buffer.limit() * Int.SIZE_BYTES.toLong(), vertexBuffer.buffer, GL.GL_STATIC_DRAW)
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, uvOut)
        gl.glBufferData(GL.GL_ARRAY_BUFFER, uvBuffer.buffer.limit() * Float.SIZE_BYTES.toLong(), uvBuffer.buffer, GL.GL_STATIC_DRAW)
        bufferedVertices = vertexBuffer.buffer.limit() / VEC_DIMS
    }

    override fun produceVertices(camera: Camera, currFrame: Int) {
        // already produced
    }

    override fun draw() {
        gl.glEnable(GL.GL_CULL_FACE)

        gl.glBindVertexArray(vaoHandle)
        gl.glDrawArrays(GL.GL_TRIANGLES, 0, bufferedVertices)

        gl.glDisable(GL.GL_CULL_FACE)
    }
}
