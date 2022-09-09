package controllers.worldRenderer

import controllers.worldRenderer.entities.ComputeObj
import controllers.worldRenderer.entities.Renderable
import controllers.worldRenderer.helpers.GpuFloatBuffer
import controllers.worldRenderer.helpers.GpuIntBuffer
import org.lwjgl.opengl.GL11C.GL_FLOAT
import org.lwjgl.opengl.GL11C.GL_INT
import org.lwjgl.opengl.GL15C.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL15C.glBindBuffer
import org.lwjgl.opengl.GL15C.glDeleteBuffers
import org.lwjgl.opengl.GL15C.glGenBuffers
import org.lwjgl.opengl.GL20C.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20C.glVertexAttribPointer
import org.lwjgl.opengl.GL30C.glBindVertexArray
import org.lwjgl.opengl.GL30C.glDeleteVertexArrays
import org.lwjgl.opengl.GL30C.glGenVertexArrays
import org.lwjgl.opengl.GL30C.glVertexAttribIPointer

abstract class AbstractPriorityRenderer : PriorityRenderer {
    // We don't use this field at construction time because it may cause
    // problems with half-initialised subclasses not returning the object
    // because they haven't finished construction yet.
    protected val vertexOut = glGenBuffers()
    protected val uvOut = glGenBuffers()
    //    private val animFrameBufferId = glGenBuffers()
    protected val vaoHandle = initVao()
    private var generation = 0

    protected fun initVao(): Int {
        // Create VAO
        val vaoHandle = glGenVertexArrays()
        glBindVertexArray(vaoHandle)
        glEnableVertexAttribArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, vertexOut)
        glVertexAttribIPointer(0, 4, GL_INT, 0, 0)
        glEnableVertexAttribArray(1)
        glBindBuffer(GL_ARRAY_BUFFER, uvOut)
        glVertexAttribPointer(1, 4, GL_FLOAT, false, 0, 0)
//        gl.glEnableVertexAttribArray(2);
//        gl.glBindBuffer(gl.GL_ARRAY_BUFFER, animFrameBufferId);
//        gl.glVertexAttribIPointer(2, 4, gl.GL_INT, 0, 0);

        // unbind VBO
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindVertexArray(0)
        return vaoHandle
    }

    private fun deinitVao() {
        val allBuffers = intArrayOf(vertexOut, uvOut)
        glDeleteBuffers(allBuffers)

        glDeleteVertexArrays(vaoHandle)
    }

    override fun destroy() {
        deinitVao()
    }

    override fun beginUploading() {}

    protected fun prepareBuffersForRenderable(renderable: Renderable, faces: Int, hasUVs: Boolean, vertexBuffer: GpuIntBuffer, uvBuffer: GpuFloatBuffer) {
        val comp = renderable.computeObj
        val strictUVs = needsStrictUVs || hasUVs

        comp.offset = vertexBuffer.buffer.position() / VEC_DIMS
        comp.uvOffset = if (strictUVs) uvBuffer.buffer.position() / VEC_DIMS else -1
        comp.size = faces

        vertexBuffer.ensureCapacity(faces * (VEC_DIMS * VERTICES_PER_TRI))
        if (strictUVs)
            uvBuffer.ensureCapacity(faces * (VEC_DIMS * VERTICES_PER_TRI))

        comp.generation = generation
    }

    protected fun isUploaded(computeObj: ComputeObj): Boolean =
        computeObj.generation == generation

    override fun positionRenderable(
        renderable: Renderable,
        sceneX: Int,
        sceneY: Int,
        height: Int,
    ) {
        val computeObj = renderable.computeObj
        computeObj.flags = renderable.renderFlags
        computeObj.x = sceneX * Constants.LOCAL_TILE_SIZE + renderable.renderOffsetX
        computeObj.y = height + renderable.renderOffsetY
        computeObj.z = sceneY * Constants.LOCAL_TILE_SIZE + renderable.renderOffsetZ
    }

    override fun finishPositioning() {
        generation++ // Mark all compute objects as non-uploaded again
    }

    override fun bindVao() {
        glBindVertexArray(vaoHandle)
    }

    companion object {
        const val VEC_DIMS = 4
        const val VERTICES_PER_TRI = 3
    }
}
