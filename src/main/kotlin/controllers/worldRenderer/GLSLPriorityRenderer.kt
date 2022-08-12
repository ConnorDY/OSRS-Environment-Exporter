package controllers.worldRenderer

import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2ES3
import com.jogamp.opengl.GL3ES3
import com.jogamp.opengl.GL4
import com.jogamp.opengl.util.GLBuffers
import controllers.worldRenderer.entities.Renderable
import controllers.worldRenderer.helpers.GLUtil.glGenBuffers
import controllers.worldRenderer.helpers.GpuIntBuffer
import controllers.worldRenderer.helpers.ModelBuffers
import controllers.worldRenderer.shaders.Shader
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.math.min

class GLSLPriorityRenderer(private val gl: GL4) : PriorityRenderer {
    private val bufferId = glGenBuffers(gl)
    private val uvBufferId = glGenBuffers(gl)
    private val tmpBufferId = glGenBuffers(gl)
    private val tmpUvBufferId = glGenBuffers(gl)
    private val tmpModelBufferId = glGenBuffers(gl)
    private val tmpModelBufferSmallId = glGenBuffers(gl)
    private val tmpModelBufferUnorderedId = glGenBuffers(gl)
    private val selectedIdsBufferId = glGenBuffers(gl)

    private val glComputeProgram = Shader.COMPUTE_PROGRAM.value.compile(gl, Shader.createTemplate(1024, 4))
    private val glSmallComputeProgram = Shader.COMPUTE_PROGRAM.value.compile(gl, Shader.createTemplate(512, 1))
    private val glUnorderedComputeProgram = Shader.UNORDERED_COMPUTE_PROGRAM.value.compile(gl, Shader.createTemplate(-1, -1))

    private val uniBlockSmall = gl.glGetUniformBlockIndex(glSmallComputeProgram, "uniforms")
    private val uniBlockLarge = gl.glGetUniformBlockIndex(glComputeProgram, "uniforms")

    override fun startAdding(modelBuffers: ModelBuffers) {
        val vertexBuffer: IntBuffer = modelBuffers.vertexBuffer.buffer
        val uvBuffer: FloatBuffer = modelBuffers.uvBuffer.buffer
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId)
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            vertexBuffer.limit() * GLBuffers.SIZEOF_INT.toLong(),
            vertexBuffer,
            GL2ES3.GL_STATIC_COPY
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, uvBufferId)
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            uvBuffer.limit() * GLBuffers.SIZEOF_FLOAT.toLong(),
            uvBuffer,
            GL2ES3.GL_STATIC_COPY
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0)
    }

    override fun addRenderable(renderable: Renderable, modelBuffers: ModelBuffers, sceneX: Int, sceneY: Int, height: Int, objType: Int) {
        val b: GpuIntBuffer =
            if (renderable.renderUnordered) modelBuffers.modelBufferUnordered
            else modelBuffers.bufferForTriangles(min(ModelBuffers.MAX_TRIANGLE, renderable.faceCount))
        if (renderable.renderUnordered) modelBuffers.incUnorderedModels()
        b.ensureCapacity(13)

        renderable.computeObj.idx = modelBuffers.targetBufferOffset
        renderable.computeObj.flags = renderable.renderFlags
        renderable.computeObj.x = sceneX * Constants.LOCAL_TILE_SIZE + renderable.renderOffsetX
        renderable.computeObj.y = height + renderable.renderOffsetY
        renderable.computeObj.z = sceneY * Constants.LOCAL_TILE_SIZE + renderable.renderOffsetZ
        renderable.computeObj.pickerId = modelBuffers.calcPickerId(sceneX, sceneY, objType)
        b.buffer.put(renderable.computeObj.toArray())

        modelBuffers.addTargetBufferOffset(renderable.computeObj.size * 3)
    }

    override fun finishAdding(modelBuffers: ModelBuffers) {
    }

    override fun produceVertices(modelBuffers: ModelBuffers, uniformStructIn: GLBuffer, vertexOut: GLBuffer, uvOut: GLBuffer, pickerIdsOut: GLBuffer) {
        val vertexBuffer: IntBuffer = modelBuffers.vertexBuffer.buffer
        val uvBuffer: FloatBuffer = modelBuffers.uvBuffer.buffer
        val modelBuffer: IntBuffer = modelBuffers.modelBuffer.buffer
        val modelBufferSmall: IntBuffer = modelBuffers.modelBufferSmall.buffer
        val modelBufferUnordered: IntBuffer = modelBuffers.modelBufferUnordered.buffer
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, tmpBufferId)
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            vertexBuffer.limit() * Integer.BYTES.toLong(),
            vertexBuffer,
            GL.GL_DYNAMIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, tmpUvBufferId)
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            uvBuffer.limit() * java.lang.Float.BYTES.toLong(),
            uvBuffer,
            GL.GL_DYNAMIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, tmpModelBufferId)
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            modelBuffer.limit() * Integer.BYTES.toLong(),
            modelBuffer,
            GL.GL_DYNAMIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, tmpModelBufferSmallId)
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            modelBufferSmall.limit() * Integer.BYTES.toLong(),
            modelBufferSmall,
            GL.GL_DYNAMIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, tmpModelBufferUnorderedId)
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            modelBufferUnordered.limit() * Integer.BYTES.toLong(),
            modelBufferUnordered,
            GL.GL_DYNAMIC_DRAW
        )

        // Draw 3d scene
        gl.glBindBufferBase(GL2ES3.GL_UNIFORM_BUFFER, 0, uniformStructIn)
        gl.glUniformBlockBinding(glSmallComputeProgram, uniBlockSmall, 0)
        gl.glUniformBlockBinding(glComputeProgram, uniBlockLarge, 0)

        /*
         * Compute is split into two separate programs 'small' and 'large' to
         * save on GPU resources. Small will sort <= 512 faces, large will do <= 4096.
         */

        // unordered
        gl.glUseProgram(glUnorderedComputeProgram)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 0, tmpModelBufferUnorderedId)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 1, bufferId)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 2, tmpBufferId)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 3, vertexOut)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 4, uvOut)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 5, uvBufferId)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 6, tmpUvBufferId)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 7, pickerIdsOut)
        gl.glDispatchCompute(modelBuffers.unorderedModelsCount, 1, 1)

        // small
        gl.glUseProgram(glSmallComputeProgram)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 0, tmpModelBufferSmallId)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 1, bufferId)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 2, tmpBufferId)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 3, vertexOut)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 4, uvOut)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 5, uvBufferId)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 6, tmpUvBufferId)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 7, pickerIdsOut)
        gl.glDispatchCompute(modelBuffers.smallModelsCount, 1, 1)

        // large
        gl.glUseProgram(glComputeProgram)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 0, tmpModelBufferId)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 1, bufferId)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 2, tmpBufferId)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 3, vertexOut)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 4, uvOut)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 5, uvBufferId)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 6, tmpUvBufferId)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 7, pickerIdsOut)
        gl.glDispatchCompute(modelBuffers.largeModelsCount, 1, 1)
        gl.glMemoryBarrier(GL3ES3.GL_SHADER_STORAGE_BARRIER_BIT)
    }

    override fun destroy() {
        val allBuffers = intArrayOf(bufferId, uvBufferId, tmpBufferId, tmpUvBufferId, tmpModelBufferId, tmpModelBufferSmallId, tmpModelBufferUnorderedId, selectedIdsBufferId)
        gl.glDeleteBuffers(allBuffers.size, allBuffers, 0)

        gl.glDeleteProgram(glComputeProgram)
        gl.glDeleteProgram(glSmallComputeProgram)
        gl.glDeleteProgram(glUnorderedComputeProgram)
    }
}
