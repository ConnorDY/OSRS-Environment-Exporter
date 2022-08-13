package controllers.worldRenderer

import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2ES3
import com.jogamp.opengl.GL3ES3
import com.jogamp.opengl.util.GLBuffers
import controllers.worldRenderer.entities.Renderable
import controllers.worldRenderer.helpers.GLUtil.glGenBuffers
import controllers.worldRenderer.helpers.GpuIntBuffer
import controllers.worldRenderer.helpers.ModelBuffers
import controllers.worldRenderer.shaders.Shader
import org.slf4j.LoggerFactory
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.math.min

class GLSLPriorityRenderer(override val gl: GL3ES3) : AbstractPriorityRenderer(gl) {
    private val logger = LoggerFactory.getLogger(GLSLPriorityRenderer::class.java)

    private val bufferId = glGenBuffers(gl)
    private val uvBufferId = glGenBuffers(gl)
    private val tmpBufferId = glGenBuffers(gl)
    private val tmpUvBufferId = glGenBuffers(gl)
    private val tmpModelBufferId = glGenBuffers(gl)
    private val tmpModelBufferSmallId = glGenBuffers(gl)
    private val tmpModelBufferUnorderedId = glGenBuffers(gl)

    private val glComputeProgram = Shader.COMPUTE_PROGRAM.value.compile(gl, Shader.createTemplate(1024, 4))
    private val glSmallComputeProgram = Shader.COMPUTE_PROGRAM.value.compile(gl, Shader.createTemplate(512, 1))
    private val glUnorderedComputeProgram = Shader.UNORDERED_COMPUTE_PROGRAM.value.compile(gl, Shader.createTemplate(-1, -1))

    private val uniformBuffer: IntBuffer = GpuIntBuffer.allocateDirect(5 + 3 + 1)
    private val uniformBufferId = initUniformBuffer(uniformBuffer)

    private val uniBlockSmall = gl.glGetUniformBlockIndex(glSmallComputeProgram, "uniforms")
    private val uniBlockLarge = gl.glGetUniformBlockIndex(glComputeProgram, "uniforms")

    private val modelBuffers = ModelBuffers()

    override val needsStrictUVs get() = false

    private fun initUniformBuffer(uniformBuffer: IntBuffer): GLBuffer {
        val uniformBufferId = glGenBuffers(gl)
        gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, uniformBufferId)
        uniformBuffer.clear()
        uniformBuffer.put(IntArray(9))
        uniformBuffer.flip()
        gl.glBufferData(
            GL2ES3.GL_UNIFORM_BUFFER,
            uniformBuffer.limit() * Integer.BYTES.toLong(),
            uniformBuffer,
            GL.GL_DYNAMIC_DRAW
        )
        gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, 0)
        return uniformBufferId
    }

    override fun getBuffersForRenderable(renderable: Renderable, faces: Int, hasUVs: Boolean): Pair<IntBuffer, FloatBuffer> {
        val vertexBuffer = modelBuffers.vertexBuffer
        val uvBuffer = modelBuffers.uvBuffer

        prepareBuffersForRenderable(renderable, faces, hasUVs, vertexBuffer, uvBuffer)
        return Pair(vertexBuffer.buffer, uvBuffer.buffer)
    }

    override fun finishUploading() {
        val vertexBuffer: IntBuffer = modelBuffers.vertexBuffer.buffer
        val uvBuffer: FloatBuffer = modelBuffers.uvBuffer.buffer

        modelBuffers.flipVertUv()
        logger.debug("vertexBuffer size {}", vertexBuffer.limit())

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
        modelBuffers.clearVertUv()
        modelBuffers.clear()
        modelBuffers.clearBufferOffset()
    }

    override fun positionRenderable(
        renderable: Renderable,
        sceneX: Int,
        sceneY: Int,
        height: Int,
        objType: Int
    ) {
        val computeObj = renderable.computeObj
        if (!isUploaded(computeObj)) return

        super.positionRenderable(renderable, sceneX, sceneY, height, objType)
        computeObj.idx = modelBuffers.targetBufferOffset
        modelBuffers.addTargetBufferOffset(computeObj.size * 3)

        val b: GpuIntBuffer =
            if (renderable.renderUnordered) modelBuffers.bufferUnordered()
            else modelBuffers.bufferForTriangles(min(ModelBuffers.MAX_TRIANGLE, renderable.faceCount))
        b.ensureCapacity(13)
        b.buffer.put(computeObj.toArray())
    }

    override fun finishPositioning() {
        super.finishPositioning()
        // allocate enough size in the outputBuffer for the static verts + the dynamic verts -- each vertex is an ivec4, 4 ints
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexOut)
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            ((modelBuffers.targetBufferOffset + MAX_TEMP_VERTICES) * GLBuffers.SIZEOF_INT * 4).toLong(),
            null,
            GL.GL_DYNAMIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, uvOut)
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            ((modelBuffers.targetBufferOffset + MAX_TEMP_VERTICES) * GLBuffers.SIZEOF_FLOAT * 4).toLong(),
            null,
            GL.GL_DYNAMIC_DRAW
        )
//        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, animFrameBufferId)
//        gl.glBufferData(
//            GL.GL_ARRAY_BUFFER,
//            ((modelBuffers.targetBufferOffset + MAX_TEMP_VERTICES) * GLBuffers.SIZEOF_INT * 4).toLong(),
//            null,
//            GL.GL_DYNAMIC_DRAW
//        )
    }

    override fun produceVertices(camera: Camera, currFrame: Int) {
        modelBuffers.flip()
        modelBuffers.flipVertUv()

        // UBO
        gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, uniformBufferId)
        uniformBuffer.clear()
        uniformBuffer
            .put(camera.yaw)
            .put(camera.pitch)
            .put(camera.centerX)
            .put(camera.centerY)
            .put(camera.scale)
            .put(camera.cameraX) // x
            .put(camera.cameraZ) // z
            .put(camera.cameraY) // y
            .put(currFrame)
        uniformBuffer.flip()
        gl.glBufferSubData(
            GL2ES3.GL_UNIFORM_BUFFER,
            0,
            uniformBuffer.limit() * Integer.BYTES.toLong(),
            uniformBuffer
        )
        gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, 0)

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
        gl.glBindBufferBase(GL2ES3.GL_UNIFORM_BUFFER, 0, uniformBufferId)
        gl.glUniformBlockBinding(glSmallComputeProgram, uniBlockSmall, 0)
        gl.glUniformBlockBinding(glComputeProgram, uniBlockLarge, 0)

        /*
         * Compute is split into two separate programs 'small' and 'large' to
         * save on GPU resources. Small will sort <= 512 faces, large will do <= 4096.
         */

        // unordered
        gl.glUseProgram(glUnorderedComputeProgram)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 0, tmpModelBufferUnorderedId)
        bindCommonBuffers()
        gl.glDispatchCompute(modelBuffers.unorderedModelsCount, 1, 1)

        // small
        gl.glUseProgram(glSmallComputeProgram)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 0, tmpModelBufferSmallId)
        bindCommonBuffers()
        gl.glDispatchCompute(modelBuffers.smallModelsCount, 1, 1)

        // large
        gl.glUseProgram(glComputeProgram)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 0, tmpModelBufferId)
        bindCommonBuffers()
        gl.glDispatchCompute(modelBuffers.largeModelsCount, 1, 1)
        gl.glMemoryBarrier(GL3ES3.GL_SHADER_STORAGE_BARRIER_BIT)
    }

    private fun bindCommonBuffers() {
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 1, bufferId)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 2, tmpBufferId)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 3, vertexOut)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 4, uvOut)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 5, uvBufferId)
        gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 6, tmpUvBufferId)
    }

    override fun draw() {
        // We just allow the GL to do face culling. Note this requires the priority renderer
        // to have logic to disregard culled faces in the priority depth testing.
        gl.glEnable(GL.GL_CULL_FACE)

        // Draw output of compute shaders
        gl.glBindVertexArray(vaoHandle)
        gl.glDrawArrays(GL.GL_TRIANGLES, 0, modelBuffers.targetBufferOffset + modelBuffers.tempOffset)

        gl.glDisable(GL.GL_CULL_FACE)

        modelBuffers.clearVertUv()
        modelBuffers.clear()
    }

    override fun destroy() {
        super.destroy()
        val allBuffers = intArrayOf(bufferId, uvBufferId, tmpBufferId, tmpUvBufferId, tmpModelBufferId, tmpModelBufferSmallId, tmpModelBufferUnorderedId)
        gl.glDeleteBuffers(allBuffers.size, allBuffers, 0)

        gl.glDeleteProgram(glComputeProgram)
        gl.glDeleteProgram(glSmallComputeProgram)
        gl.glDeleteProgram(glUnorderedComputeProgram)
    }

    override fun toString() =
        "${javaClass.simpleName}(modelBuffers.vertexBuffer[${modelBuffers.vertexBuffer.buffer.limit()}])"

    companion object {
        private const val MAX_TEMP_VERTICES = 65535
    }
}
