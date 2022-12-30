package gpu

import controllers.worldRenderer.shaders.Shader
import org.lwjgl.opengl.GL11C.GL_FLOAT
import org.lwjgl.opengl.GL11C.GL_LINEAR
import org.lwjgl.opengl.GL11C.GL_RGBA
import org.lwjgl.opengl.GL11C.GL_TEXTURE_2D
import org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER
import org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER
import org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_S
import org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_T
import org.lwjgl.opengl.GL11C.GL_TRIANGLE_STRIP
import org.lwjgl.opengl.GL11C.glBindTexture
import org.lwjgl.opengl.GL11C.glDrawArrays
import org.lwjgl.opengl.GL11C.glDrawBuffer
import org.lwjgl.opengl.GL11C.glGenTextures
import org.lwjgl.opengl.GL11C.glTexImage2D
import org.lwjgl.opengl.GL11C.glTexParameteri
import org.lwjgl.opengl.GL11C.glViewport
import org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL12C.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL13C.GL_TEXTURE0
import org.lwjgl.opengl.GL15C.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL15C.GL_STATIC_DRAW
import org.lwjgl.opengl.GL15C.glActiveTexture
import org.lwjgl.opengl.GL15C.glBindBuffer
import org.lwjgl.opengl.GL15C.glBufferData
import org.lwjgl.opengl.GL15C.glGenBuffers
import org.lwjgl.opengl.GL20C.glDeleteProgram
import org.lwjgl.opengl.GL20C.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20C.glGetUniformLocation
import org.lwjgl.opengl.GL20C.glUniform1i
import org.lwjgl.opengl.GL20C.glUseProgram
import org.lwjgl.opengl.GL20C.glVertexAttribPointer
import org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL30C.glBindFramebuffer
import org.lwjgl.opengl.GL30C.glBindVertexArray
import org.lwjgl.opengl.GL30C.glDeleteFramebuffers
import org.lwjgl.opengl.GL30C.glDeleteVertexArrays
import org.lwjgl.opengl.GL30C.glFramebufferTexture2D
import org.lwjgl.opengl.GL30C.glGenFramebuffers
import org.lwjgl.opengl.GL30C.glGenVertexArrays
import org.lwjgl.opengl.GL32C.GL_TEXTURE_2D_MULTISAMPLE

class GraphicsEffects {
    private var quadVAO = 0
    private var quadVBO = 0

    private var texcopyProgram = 0
    private var texcopyUniTex = 0

    private var texcopyUnantialiasProgram = 0
    private var texcopyUnantialiasUniTex = 0

    private var outlineProgram = 0
    private var outlineUniTex = 0

    // Based on OpenGL tutorial code
    // https://github.com/JoeyDeVries/LearnOpenGL/blob/master/src/5.advanced_lighting/6.hdr/hdr.cpp
    private fun initFullscreenQuad() {
        if (quadVAO != 0) return
        quadVAO = glGenVertexArrays()
        quadVBO = glGenBuffers()
        glBindVertexArray(quadVAO)
        glBindBuffer(GL_ARRAY_BUFFER, quadVBO)
        glBufferData(
            GL_ARRAY_BUFFER,
            floatArrayOf(
                -1.0f, 1.0f, 0.0f, 0.0f, 1.0f,
                -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
                1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
                1.0f, -1.0f, 0.0f, 1.0f, 0.0f
            ),
            GL_STATIC_DRAW
        )
        glEnableVertexAttribArray(0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.SIZE_BYTES, 0L)
        glEnableVertexAttribArray(1)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.SIZE_BYTES, 3L * Float.SIZE_BYTES)
    }

    private fun initTexcopyProgram() {
        if (texcopyProgram != 0) return

        texcopyProgram = Shader.TEXCOPY_PROGRAM.value.compile(Shader.createTemplate(0, 0))
        texcopyUniTex = glGetUniformLocation(texcopyProgram, "tex")
    }

    private fun initTexcopyUnantialiasProgram() {
        if (texcopyUnantialiasProgram != 0) return

        texcopyUnantialiasProgram = Shader.TEXCOPY_UNANTIALIAS_PROGRAM.value.compile(Shader.createTemplate(0, 0))
        texcopyUnantialiasUniTex = glGetUniformLocation(texcopyUnantialiasProgram, "tex")
    }

    private fun initOutlineProgram() {
        if (outlineProgram != 0) return

        outlineProgram = Shader.OUTLINE_PROGRAM.value.compile(Shader.createTemplate(0, 0))
        outlineUniTex = glGetUniformLocation(outlineProgram, "tex")
    }

    fun destroy() {
        if (quadVAO != 0) glDeleteVertexArrays(quadVAO)
        if (quadVBO != 0) glDeleteFramebuffers(quadVBO)
        if (texcopyProgram != 0) glDeleteProgram(texcopyProgram)
        if (texcopyUnantialiasProgram != 0) glDeleteProgram(texcopyUnantialiasProgram)
        if (outlineProgram != 0) glDeleteProgram(outlineProgram)
    }

    fun drawFullscreenQuad() {
        initFullscreenQuad()
        glBindVertexArray(quadVAO)
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
        glBindVertexArray(0)
    }

    fun copyAndUnantialias(textureId: Int, width: Int, height: Int): Int {
        initTexcopyUnantialiasProgram()

        val framebuffer = glGenFramebuffers()
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer)

        // create a color attachment texture
        val outTexture = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, outTexture)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0L)

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, outTexture, 0)
        glDrawBuffer(GL_COLOR_ATTACHMENT0)
        glViewport(0, 0, width, height)

        glUseProgram(texcopyUnantialiasProgram)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, textureId)
        glUniform1i(texcopyUnantialiasUniTex, 0)
        drawFullscreenQuad()

        glBindFramebuffer(GL_FRAMEBUFFER, 0)
        glDeleteFramebuffers(framebuffer)

        return outTexture
    }

    fun copyTextureToCurrentFramebuffer(textureId: Int) {
        initTexcopyProgram()

        glUseProgram(texcopyProgram)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, textureId)
        glUniform1i(texcopyUniTex, 0)
        drawFullscreenQuad()
    }

    fun outlineTexture(textureId: Int, width: Int, height: Int): Int {
        initOutlineProgram()

        // Create a framebuffer to render to
        val framebuffer = glGenFramebuffers()
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer)

        // Create a texture to render to
        val renderTexture = glGenTextures()
        setSensibleTextureParams(renderTexture)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0L)

        // Attach the texture to the framebuffer
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, renderTexture, 0)

        // Render to the framebuffer
        glViewport(0, 0, width, height)
        glUseProgram(outlineProgram)
        glActiveTexture(GL_TEXTURE0)
        glDrawBuffer(GL_COLOR_ATTACHMENT0)
        glBindTexture(GL_TEXTURE_2D, textureId)
        glUniform1i(outlineUniTex, 0)
        drawFullscreenQuad()

        glUseProgram(0)

        glDeleteFramebuffers(framebuffer)
        return renderTexture
    }

    private fun setSensibleTextureParams(renderTexture: Int) {
        glBindTexture(GL_TEXTURE_2D, renderTexture)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    }
}
