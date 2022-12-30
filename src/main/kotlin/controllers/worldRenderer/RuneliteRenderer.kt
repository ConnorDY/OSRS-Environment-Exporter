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

import cache.utils.ColorPalette
import controllers.worldRenderer.helpers.AntiAliasingMode
import controllers.worldRenderer.helpers.GpuFloatBuffer
import org.joml.Matrix4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL11C.GL_DEPTH_COMPONENT
import org.lwjgl.opengl.GL11C.GL_NEAREST
import org.lwjgl.opengl.GL11C.GL_RGBA
import org.lwjgl.opengl.GL11C.glBindTexture
import org.lwjgl.opengl.GL11C.glDisable
import org.lwjgl.opengl.GL11C.glEnable
import org.lwjgl.opengl.GL11C.glGenTextures
import org.lwjgl.opengl.GL11C.glGetInteger
import org.lwjgl.opengl.GL13C.GL_MULTISAMPLE
import org.lwjgl.opengl.GL15C.GL_DYNAMIC_DRAW
import org.lwjgl.opengl.GL15C.glBindBuffer
import org.lwjgl.opengl.GL15C.glBufferData
import org.lwjgl.opengl.GL15C.glBufferSubData
import org.lwjgl.opengl.GL15C.glGenBuffers
import org.lwjgl.opengl.GL20C.glGetUniformLocation
import org.lwjgl.opengl.GL20C.glUniform1f
import org.lwjgl.opengl.GL20C.glUniform1i
import org.lwjgl.opengl.GL20C.glUniform2fv
import org.lwjgl.opengl.GL20C.glUniformMatrix4fv
import org.lwjgl.opengl.GL20C.glUseProgram
import org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL30C.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.GL30C.GL_DRAW_FRAMEBUFFER
import org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL30C.GL_MAX_SAMPLES
import org.lwjgl.opengl.GL30C.GL_READ_FRAMEBUFFER
import org.lwjgl.opengl.GL30C.GL_RENDERBUFFER
import org.lwjgl.opengl.GL30C.glBindBufferBase
import org.lwjgl.opengl.GL30C.glBindFramebuffer
import org.lwjgl.opengl.GL30C.glBindRenderbuffer
import org.lwjgl.opengl.GL30C.glBlitFramebuffer
import org.lwjgl.opengl.GL30C.glDeleteFramebuffers
import org.lwjgl.opengl.GL30C.glDeleteRenderbuffers
import org.lwjgl.opengl.GL30C.glFramebufferTexture2D
import org.lwjgl.opengl.GL30C.glGenFramebuffers
import org.lwjgl.opengl.GL30C.glUniform1ui
import org.lwjgl.opengl.GL31C.GL_UNIFORM_BUFFER
import org.lwjgl.opengl.GL31C.glGetUniformBlockIndex
import org.lwjgl.opengl.GL31C.glUniformBlockBinding
import org.lwjgl.opengl.GL32C.GL_TEXTURE_2D_MULTISAMPLE
import org.lwjgl.opengl.GL32C.glTexImage2DMultisample
import java.nio.ByteBuffer
import kotlin.math.min

// Renderer code which appears to have originated with RuneLite
open class RuneliteRenderer(
    private val textureManager: TextureManager,
    var antiAliasingMode: AntiAliasingMode,
) {
    private var lastStretchedCanvasWidth = -1
    private var lastStretchedCanvasHeight = -1
    private var lastAntiAliasingMode: AntiAliasingMode? = null

    protected var fboSceneHandle = -1
    private var texSceneHandle = -1
    private var texSceneDepthBuffer = -1

    private var textureArrayId = -1
    private val textureOffsets = FloatArray(256)

    internal var glProgram = 0

    private var uniDrawDistance = -1
    private var uniViewProjectionMatrix = -1
    private var uniBrightness = -1
    private var uniSmoothBanding = -1
    private var uniHashSeed = -1
    private var uniTextures = -1
    private var uniTextureOffsets = -1
    private var uniBlockMain = -1

    private var uniformBufferId = 0
    private val uniformBuffer: ByteBuffer = BufferUtils.createByteBuffer(5 * Float.SIZE_BYTES + (3 + 1) * Int.SIZE_BYTES)

    protected val viewProjectionMatrix = Matrix4f()

    fun isAntiAliasingEnabled(): Boolean =
        antiAliasingMode !== AntiAliasingMode.DISABLED

    fun setupAntiAliasing(stretchedCanvasWidth: Int, stretchedCanvasHeight: Int) {
        // Setup anti-aliasing
        if (isAntiAliasingEnabled()) {
            glEnable(GL_MULTISAMPLE)

            // Re-create fbo
            if (lastStretchedCanvasWidth != stretchedCanvasWidth || lastStretchedCanvasHeight != stretchedCanvasHeight || lastAntiAliasingMode !== antiAliasingMode
            ) {
                val maxSamples: Int = glGetInteger(GL_MAX_SAMPLES)
                val samples = min(antiAliasingMode.ordinal, maxSamples)
                initAAFbo(stretchedCanvasWidth, stretchedCanvasHeight, samples)
                lastStretchedCanvasWidth = stretchedCanvasWidth
                lastStretchedCanvasHeight = stretchedCanvasHeight
            }
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fboSceneHandle)
        } else {
            glDisable(GL_MULTISAMPLE)
            shutdownAAFbo()
        }
        lastAntiAliasingMode = antiAliasingMode
    }

    fun blitAntiAliasing() {
        if (isAntiAliasingEnabled()) {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, fboSceneHandle)
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0)
            glBlitFramebuffer(
                0, 0, lastStretchedCanvasWidth, lastStretchedCanvasHeight,
                0, 0, lastStretchedCanvasWidth, lastStretchedCanvasHeight,
                GL_COLOR_BUFFER_BIT, GL_NEAREST
            )
        }
    }

    protected open fun initAAFbo(width: Int, height: Int, aaSamples: Int) {
        // Discard old FBO
        shutdownAAFbo()

        // Create and bind the FBO
        fboSceneHandle = glGenFramebuffers()
        glBindFramebuffer(GL_FRAMEBUFFER, fboSceneHandle)

        // Create color texture
        texSceneHandle = glGenTextures()
        glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, texSceneHandle)
        glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, aaSamples, GL_RGBA, width, height, false)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D_MULTISAMPLE, texSceneHandle, 0)

        // Create depth texture
        texSceneDepthBuffer = glGenTextures()
        glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, texSceneDepthBuffer)
        glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, aaSamples, GL_DEPTH_COMPONENT, width, height, false)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D_MULTISAMPLE, texSceneDepthBuffer, 0)

        // Reset
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
        glBindRenderbuffer(GL_RENDERBUFFER, 0)
    }

    private fun shutdownAAFbo() {
        if (fboSceneHandle != -1) {
            glDeleteFramebuffers(fboSceneHandle)
            fboSceneHandle = -1
        }
        if (texSceneHandle != -1) {
            glDeleteRenderbuffers(texSceneHandle)
            texSceneHandle = -1
        }
        if (texSceneDepthBuffer != -1) {
            glDeleteRenderbuffers(texSceneDepthBuffer)
            texSceneDepthBuffer = -1
        }
    }

    open fun stop() {
        shutdownAAFbo()
    }

    private fun makeProjectionMatrix(width: Float, height: Float, near: Float): FloatArray =
        floatArrayOf(
            2 / width, 0f, 0f, 0f,
            0f, 2 / height, 0f, 0f,
            0f, 0f, -1f, -1f,
            0f, 0f, -2 * near, 0f,
        )

    private fun calculateViewProjectionMatrix(camera: Camera, canvasWidth: Int, canvasHeight: Int): FloatArray {
        val projectionMatrix = makeProjectionMatrix(
            canvasWidth.toFloat(),
            canvasHeight.toFloat(),
            50.0f
        )
        return viewProjectionMatrix
            .scaling(camera.scale.toFloat(), camera.scale.toFloat(), 1.0f)
            .mul(Matrix4f(GpuFloatBuffer.allocateDirect(projectionMatrix.size).put(projectionMatrix).flip()))
            .rotateX((-Math.PI + camera.pitchRads).toFloat())
            .rotateY(camera.yawRads.toFloat())
            .translate(-camera.cameraX.toFloat(), -camera.cameraZ.toFloat(), -camera.cameraY.toFloat())
            .get(projectionMatrix)
    }

    internal fun prepareDrawProgram(camera: Camera, canvasWidth: Int, canvasHeight: Int, clientCycle: Int) {
        if (textureArrayId == -1) {
            // lazy init textures as they may not be loaded at plugin start.
            // this will return -1 and retry if not all textures are loaded yet, too.
            textureArrayId = textureManager.initTextureArray()
        }
//        val textures: Array<TextureDefinition> = textureProvider.getTextureDefinitions()

        glUseProgram(glProgram)
        // Brightness happens to also be stored in the texture provider, so we use that
        glUniform1f(
            uniBrightness,
            ColorPalette.BRIGHTNESS_HIGH.toFloat()
        ) // (float) textureProvider.getBrightness());
        glUniform1i(uniDrawDistance, Constants.MAX_DISTANCE * Constants.LOCAL_TILE_SIZE)
        glUniform1f(uniSmoothBanding, 1f)
        glUniform1ui(uniHashSeed, camera.motionTicks)
        glUniformMatrix4fv(
            uniViewProjectionMatrix,
            false,
            calculateViewProjectionMatrix(camera, canvasWidth, canvasHeight)
        )

        // This is just for animating!
        //        for (int id = 0; id < textures.length; ++id) {
        //            TextureDefinition texture = textures[id];
        //            if (texture == null) {
        //                continue;
        //            }
        //
        //            textureProvider.load(id); // trips the texture load flag which lets textures animate
        //
        //            textureOffsets[id * 2] = texture.field1782;
        //            textureOffsets[id * 2 + 1] = texture.field1783;
        //        }

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
        // Ints
        val doublesPosition = doubles.position() * Float.SIZE_BYTES
        uniformBuffer.position(doublesPosition)
        val ints = uniformBuffer.asIntBuffer()
        ints
            .put(camera.centerX)
            .put(camera.centerY)
            .put(camera.scale)
            .put(clientCycle) // currFrame
        val intsPosition = ints.position() * Int.SIZE_BYTES
        uniformBuffer.position(doublesPosition + intsPosition)
        uniformBuffer.flip()
        glBufferSubData(
            GL_UNIFORM_BUFFER,
            0,
            uniformBuffer
        )
        glBindBuffer(GL_UNIFORM_BUFFER, 0)

        // Bind uniforms
        glBindBufferBase(GL_UNIFORM_BUFFER, 0, uniformBufferId)
        glUniformBlockBinding(glProgram, uniBlockMain, 0)
        glUniform1i(uniTextures, 1) // texture sampler array is bound to texture1
        glUniform2fv(uniTextureOffsets, textureOffsets)
    }

    open fun initUniforms() {
        uniViewProjectionMatrix = glGetUniformLocation(glProgram, "viewProjectionMatrix")
        uniBrightness = glGetUniformLocation(glProgram, "brightness")
        uniSmoothBanding = glGetUniformLocation(glProgram, "smoothBanding")
        uniDrawDistance = glGetUniformLocation(glProgram, "drawDistance")
        uniTextures = glGetUniformLocation(glProgram, "textures")
        uniTextureOffsets = glGetUniformLocation(glProgram, "textureOffsets")
        uniBlockMain = glGetUniformBlockIndex(glProgram, "uniforms")
        uniHashSeed = glGetUniformLocation(glProgram, "hashSeed")
    }

    fun initUniformBuffer() {
        uniformBufferId = glGenBuffers()
        glBindBuffer(GL_UNIFORM_BUFFER, uniformBufferId)
        uniformBuffer.clear()
        glBufferData(GL_UNIFORM_BUFFER, uniformBuffer, GL_DYNAMIC_DRAW)
        glBindBuffer(GL_UNIFORM_BUFFER, 0)
    }

    open fun clearScene() {
        // Clear scene
        val sky = 9493480
        GL11C.glClearColor((sky shr 16 and 0xFF) / 255f, (sky shr 8 and 0xFF) / 255f, (sky and 0xFF) / 255f, 1f)
        GL11C.glClear(GL_COLOR_BUFFER_BIT or GL11C.GL_DEPTH_BUFFER_BIT)
    }
}
