/* Derived from RuneLite source code, which is licensed as follows:
 *
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package controllers.worldRenderer

import controllers.worldRenderer.entities.Renderable
import controllers.worldRenderer.helpers.GpuIntBuffer
import controllers.worldRenderer.helpers.MeshBuffers
import controllers.worldRenderer.helpers.ModelBuffers
import controllers.worldRenderer.shaders.Shader
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11C.GL_CULL_FACE
import org.lwjgl.opengl.GL11C.GL_TRIANGLES
import org.lwjgl.opengl.GL11C.glDisable
import org.lwjgl.opengl.GL11C.glDrawArrays
import org.lwjgl.opengl.GL11C.glEnable
import org.lwjgl.opengl.GL15C.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL15C.GL_DYNAMIC_DRAW
import org.lwjgl.opengl.GL15C.GL_STATIC_COPY
import org.lwjgl.opengl.GL15C.glBindBuffer
import org.lwjgl.opengl.GL15C.glBufferData
import org.lwjgl.opengl.GL15C.glBufferSubData
import org.lwjgl.opengl.GL15C.glDeleteBuffers
import org.lwjgl.opengl.GL15C.glGenBuffers
import org.lwjgl.opengl.GL20C.glDeleteProgram
import org.lwjgl.opengl.GL20C.glUseProgram
import org.lwjgl.opengl.GL30C.glBindBufferBase
import org.lwjgl.opengl.GL30C.glBindBufferRange
import org.lwjgl.opengl.GL30C.glBindVertexArray
import org.lwjgl.opengl.GL30C.glGetIntegeri
import org.lwjgl.opengl.GL31C.GL_UNIFORM_BUFFER
import org.lwjgl.opengl.GL31C.glGetUniformBlockIndex
import org.lwjgl.opengl.GL31C.glUniformBlockBinding
import org.lwjgl.opengl.GL42C.glMemoryBarrier
import org.lwjgl.opengl.GL43C.GL_MAX_COMPUTE_WORK_GROUP_COUNT
import org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BARRIER_BIT
import org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BUFFER
import org.lwjgl.opengl.GL43C.glDispatchCompute
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.math.min

class GLSLPriorityRenderer : AbstractPriorityRenderer() {
    private val logger = LoggerFactory.getLogger(GLSLPriorityRenderer::class.java)

    private val bufferId = glGenBuffers()
    private val uvBufferId = glGenBuffers()
    private val tmpBufferId = glGenBuffers()
    private val tmpUvBufferId = glGenBuffers()
    private val tmpModelBufferId = glGenBuffers()
    private val tmpModelBufferSmallId = glGenBuffers()
    private val tmpModelBufferUnorderedId = glGenBuffers()

    private val glComputeProgram = Shader.COMPUTE_PROGRAM.value.compile(Shader.createTemplate(1024, 4))
    private val glSmallComputeProgram = Shader.COMPUTE_PROGRAM.value.compile(Shader.createTemplate(512, 1))
    private val glUnorderedComputeProgram = Shader.UNORDERED_COMPUTE_PROGRAM.value.compile(Shader.createTemplate(-1, -1))

    private val uniformBuffer: ByteBuffer = BufferUtils.createByteBuffer(5 * Float.SIZE_BYTES + (3 + 1) * Int.SIZE_BYTES)
    private val uniformBufferId = initUniformBuffer(uniformBuffer)

    private val uniBlockSmall = glGetUniformBlockIndex(glSmallComputeProgram, "uniforms")
    private val uniBlockLarge = glGetUniformBlockIndex(glComputeProgram, "uniforms")

    private val maxWorkgroupSize = glGetIntegeri(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 0)

    private val modelBuffers = ModelBuffers()
    private val meshBuffers = MeshBuffers()

    override val needsStrictUVs get() = false

    private fun initUniformBuffer(uniformBuffer: ByteBuffer): GLBuffer {
        val uniformBufferId = glGenBuffers()
        glBindBuffer(GL_UNIFORM_BUFFER, uniformBufferId)
        uniformBuffer.clear()
        glBufferData(GL_UNIFORM_BUFFER, uniformBuffer, GL_DYNAMIC_DRAW)
        glBindBuffer(GL_UNIFORM_BUFFER, 0)
        return uniformBufferId
    }

    override fun beginUploading() {
        super.beginUploading()
        modelBuffers.clear()
        modelBuffers.clearBufferOffset()
    }

    override fun getBuffersForRenderable(renderable: Renderable, faces: Int, hasUVs: Boolean): Pair<IntBuffer, FloatBuffer> {
        val vertexBuffer = meshBuffers.vertexBuffer
        val uvBuffer = meshBuffers.uvBuffer

        prepareBuffersForRenderable(renderable, faces, hasUVs, vertexBuffer, uvBuffer)
        return Pair(vertexBuffer.buffer, uvBuffer.buffer)
    }

    override fun finishUploading() {
        val vertexBuffer: IntBuffer = meshBuffers.vertexBuffer.buffer
        val uvBuffer: FloatBuffer = meshBuffers.uvBuffer.buffer

        meshBuffers.flip()
        logger.debug("vertexBuffer size {}", vertexBuffer.limit())

        glBindBuffer(GL_ARRAY_BUFFER, bufferId)
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_COPY)
        glBindBuffer(GL_ARRAY_BUFFER, uvBufferId)
        glBufferData(GL_ARRAY_BUFFER, uvBuffer, GL_STATIC_COPY)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        meshBuffers.clear()
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
        glBindBuffer(GL_ARRAY_BUFFER, vertexOut)
        glBufferData(
            GL_ARRAY_BUFFER,
            ((modelBuffers.targetBufferOffset + MAX_TEMP_VERTICES) * Int.SIZE_BYTES * 4).toLong(),
            GL_DYNAMIC_DRAW
        )
        glBindBuffer(GL_ARRAY_BUFFER, uvOut)
        glBufferData(
            GL_ARRAY_BUFFER,
            ((modelBuffers.targetBufferOffset + MAX_TEMP_VERTICES) * Float.SIZE_BYTES * 4).toLong(),
            GL_DYNAMIC_DRAW
        )
//        glBindBuffer(GL_ARRAY_BUFFER, animFrameBufferId)
//        glBufferData(
//            GL_ARRAY_BUFFER,
//            ((modelBuffers.targetBufferOffset + MAX_TEMP_VERTICES) * Int.SIZE_BYTES * 4).toLong(),
//            GL_DYNAMIC_DRAW
//        )
    }

    override fun produceVertices(camera: Camera, currFrame: Int) {
        modelBuffers.flip()
        meshBuffers.flip()

        // UBO
        glBindBuffer(GL_UNIFORM_BUFFER, uniformBufferId)
        uniformBuffer.clear()
        val doubles = uniformBuffer.asFloatBuffer()
        doubles
            .put(camera.yawRads.toFloat())
            .put(camera.pitchRads.toFloat())
            .put(camera.cameraX.toFloat()) // x
            .put(camera.cameraZ.toFloat()) // z
            .put(camera.cameraY.toFloat()) // y
        val doublesPosition = doubles.position() * Float.SIZE_BYTES
        uniformBuffer.position(doublesPosition)
        val ints = uniformBuffer.asIntBuffer()
        ints
            .put(camera.centerX)
            .put(camera.centerY)
            .put(camera.scale)
            .put(currFrame)
        val intsPosition = ints.position() * Int.SIZE_BYTES
        uniformBuffer.position(doublesPosition + intsPosition)
        uniformBuffer.flip()
        glBufferSubData(GL_UNIFORM_BUFFER, 0, uniformBuffer)
        glBindBuffer(GL_UNIFORM_BUFFER, 0)

        val vertexBuffer: IntBuffer = meshBuffers.vertexBuffer.buffer
        val uvBuffer: FloatBuffer = meshBuffers.uvBuffer.buffer
        val modelBuffer: IntBuffer = modelBuffers.modelBuffer.buffer
        val modelBufferSmall: IntBuffer = modelBuffers.modelBufferSmall.buffer
        val modelBufferUnordered: IntBuffer = modelBuffers.modelBufferUnordered.buffer
        glBindBuffer(GL_ARRAY_BUFFER, tmpBufferId)
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_DYNAMIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, tmpUvBufferId)
        glBufferData(GL_ARRAY_BUFFER, uvBuffer, GL_DYNAMIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, tmpModelBufferId)
        glBufferData(GL_ARRAY_BUFFER, modelBuffer, GL_DYNAMIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, tmpModelBufferSmallId)
        glBufferData(GL_ARRAY_BUFFER, modelBufferSmall, GL_DYNAMIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, tmpModelBufferUnorderedId)
        glBufferData(GL_ARRAY_BUFFER, modelBufferUnordered, GL_DYNAMIC_DRAW)

        // Draw 3d scene
        glBindBufferBase(GL_UNIFORM_BUFFER, 0, uniformBufferId)
        glUniformBlockBinding(glSmallComputeProgram, uniBlockSmall, 0)
        glUniformBlockBinding(glComputeProgram, uniBlockLarge, 0)

        /*
         * Compute is split into two separate programs 'small' and 'large' to
         * save on GPU resources. Small will sort <= 512 faces, large will do <= 4096.
         */

        // unordered
        glUseProgram(glUnorderedComputeProgram)
        bindCommonBuffers()
        chunkedRunShader(tmpModelBufferUnorderedId, modelBuffers.unorderedModelsCount)

        // small
        glUseProgram(glSmallComputeProgram)
        bindCommonBuffers()
        chunkedRunShader(tmpModelBufferSmallId, modelBuffers.smallModelsCount)

        // large
        glUseProgram(glComputeProgram)
        bindCommonBuffers()
        chunkedRunShader(tmpModelBufferId, modelBuffers.largeModelsCount)
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT)
    }

    private fun chunkedRunShader(bufferId: Int, bufferLength: Int) {
        val objectSize = 12 * 4
        var remaining = bufferLength.toLong()
        var offset = 0L
        while (remaining > 0) {
            val chunk = min(remaining, maxWorkgroupSize.toLong())
            glBindBufferRange(GL_SHADER_STORAGE_BUFFER, 0, bufferId, offset * objectSize, chunk * objectSize)
            glDispatchCompute(chunk.toInt(), 1, 1)
            remaining -= chunk
            offset += chunk
        }
    }

    private fun bindCommonBuffers() {
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, bufferId)
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, tmpBufferId)
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, vertexOut)
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, uvOut)
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 5, uvBufferId)
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 6, tmpUvBufferId)
    }

    override fun draw() {
        // We just allow GL to do face culling. Note this requires the priority renderer
        // to have logic to disregard culled faces in the priority depth testing.
        glEnable(GL_CULL_FACE)

        // Draw output of compute shaders
        glBindVertexArray(vaoHandle)
        glDrawArrays(GL_TRIANGLES, 0, modelBuffers.targetBufferOffset)

        glDisable(GL_CULL_FACE)

        meshBuffers.clear()
        modelBuffers.clear()
    }

    override fun destroy() {
        super.destroy()
        val allBuffers = intArrayOf(bufferId, uvBufferId, tmpBufferId, tmpUvBufferId, tmpModelBufferId, tmpModelBufferSmallId, tmpModelBufferUnorderedId)
        glDeleteBuffers(allBuffers)

        glDeleteProgram(glComputeProgram)
        glDeleteProgram(glSmallComputeProgram)
        glDeleteProgram(glUnorderedComputeProgram)
    }

    override fun toString() =
        "${javaClass.simpleName}(modelBuffers.vertexBuffer[${meshBuffers.vertexBuffer.buffer.limit()}])"

    companion object {
        private const val MAX_TEMP_VERTICES = 65535
    }
}
