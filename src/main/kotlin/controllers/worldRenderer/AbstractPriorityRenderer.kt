package controllers.worldRenderer

import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2ES2
import com.jogamp.opengl.GL2ES3
import controllers.worldRenderer.entities.ComputeObj
import controllers.worldRenderer.entities.Renderable
import controllers.worldRenderer.helpers.GLUtil
import controllers.worldRenderer.helpers.GpuFloatBuffer
import controllers.worldRenderer.helpers.GpuIntBuffer

abstract class AbstractPriorityRenderer(gl: GL2ES3) : PriorityRenderer {
    // We don't use this field at construction time because it may cause
    // problems with half-initialised subclasses not returning the object
    // because they haven't finished construction yet.
    protected abstract val gl: GL2ES3
    protected val vertexOut = GLUtil.glGenBuffers(gl)
    protected val uvOut = GLUtil.glGenBuffers(gl)
    //    private val animFrameBufferId = glGenBuffers(gl)
    protected val vaoHandle = initVao(gl)
    private var generation = 0

    protected fun initVao(gl: GL2ES3): Int {
        // Create VAO
        val vaoHandle = GLUtil.glGenVertexArrays(gl)
        gl.glBindVertexArray(vaoHandle)
        gl.glEnableVertexAttribArray(0)
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexOut)
        gl.glVertexAttribIPointer(0, 4, GL2ES2.GL_INT, 0, 0)
        gl.glEnableVertexAttribArray(1)
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, uvOut)
        gl.glVertexAttribPointer(1, 4, GL.GL_FLOAT, false, 0, 0)
//        gl.glEnableVertexAttribArray(2);
//        gl.glBindBuffer(gl.GL_ARRAY_BUFFER, animFrameBufferId);
//        gl.glVertexAttribIPointer(2, 4, gl.GL_INT, 0, 0);

        // unbind VBO
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0)
        gl.glBindVertexArray(0)
        return vaoHandle
    }

    private fun deinitVao() {
        val allBuffers = intArrayOf(vertexOut, uvOut)
        gl.glDeleteBuffers(allBuffers.size, allBuffers, 0)

        GLUtil.glDeleteVertexArrays(gl, vaoHandle)
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
        comp.generation = generation

        vertexBuffer.ensureCapacity(faces * (VEC_DIMS * VERTICES_PER_TRI))
        if (strictUVs)
            uvBuffer.ensureCapacity(faces * (VEC_DIMS * VERTICES_PER_TRI))
    }

    protected fun isUploaded(computeObj: ComputeObj): Boolean =
        computeObj.generation == generation

    override fun positionRenderable(
        renderable: Renderable,
        sceneX: Int,
        sceneY: Int,
        height: Int,
        objType: Int
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

    companion object {
        const val VEC_DIMS = 4
        const val VERTICES_PER_TRI = 3
    }
}
