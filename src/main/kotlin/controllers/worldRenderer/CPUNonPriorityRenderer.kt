package controllers.worldRenderer

import controllers.worldRenderer.Constants.COSINE
import controllers.worldRenderer.Constants.SINE
import controllers.worldRenderer.entities.Renderable
import controllers.worldRenderer.helpers.GpuFloatBuffer
import controllers.worldRenderer.helpers.GpuIntBuffer
import org.lwjgl.opengl.GL11C.GL_CULL_FACE
import org.lwjgl.opengl.GL11C.GL_TRIANGLES
import org.lwjgl.opengl.GL11C.glDisable
import org.lwjgl.opengl.GL11C.glDrawArrays
import org.lwjgl.opengl.GL11C.glEnable
import org.lwjgl.opengl.GL15C.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL15C.GL_STATIC_DRAW
import org.lwjgl.opengl.GL15C.glBindBuffer
import org.lwjgl.opengl.GL15C.glBufferData
import org.lwjgl.opengl.GL30C.glBindVertexArray
import java.nio.FloatBuffer
import java.nio.IntBuffer

class CPUNonPriorityRenderer : AbstractPriorityRenderer() {
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

        val sin = SINE[computeObj.flags and 0x7FF]
        val cos = COSINE[computeObj.flags and 0x7FF]

        for (vertex in 0 until computeObj.size * VERTICES_PER_TRI) {
            val i = vertex * VEC_DIMS + idx
            val x = buffer.get(i)
            val z = buffer.get(i + 2)

            buffer.put(i, ((x * cos + z * sin) shr 16) + xOffset)
            buffer.put(i + 1, buffer.get(i + 1) + yOffset)
            buffer.put(i + 2, ((z * cos - x * sin) shr 16) + zOffset)
        }
    }

    override fun finishPositioning() {
        super.finishPositioning()
        // TODO: test if rewind is necessary
        vertexBuffer.buffer.rewind()
        uvBuffer.buffer.rewind()
        glBindBuffer(GL_ARRAY_BUFFER, vertexOut)
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer.buffer, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, uvOut)
        glBufferData(GL_ARRAY_BUFFER, uvBuffer.buffer, GL_STATIC_DRAW)
        bufferedVertices = vertexBuffer.buffer.limit() / VEC_DIMS
    }

    override fun produceVertices(camera: Camera, currFrame: Int) {
        // already produced
    }

    override fun draw() {
        glEnable(GL_CULL_FACE)

        glBindVertexArray(vaoHandle)
        glDrawArrays(GL_TRIANGLES, 0, bufferedVertices)

        glDisable(GL_CULL_FACE)
    }
}
