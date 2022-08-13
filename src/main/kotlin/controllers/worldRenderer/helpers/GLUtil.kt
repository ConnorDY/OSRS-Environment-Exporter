package controllers.worldRenderer.helpers

import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2ES2
import com.jogamp.opengl.GL2ES3

object GLUtil {
    private const val ERR_LEN = 1024
    private val buf = IntArray(1)
    fun glGetInteger(gl: GL, pname: Int): Int {
        gl.glGetIntegerv(pname, buf, 0)
        return buf[0]
    }

    fun glGetShader(gl: GL2ES2, shader: Int, pname: Int): Int {
        gl.glGetShaderiv(shader, pname, buf, 0)
        assert(buf[0] > -1)
        return buf[0]
    }

    fun glGetProgram(gl: GL2ES2, program: Int, pname: Int): Int {
        gl.glGetProgramiv(program, pname, buf, 0)
        assert(buf[0] > -1)
        return buf[0]
    }

    fun glGetShaderInfoLog(gl: GL2ES2, shader: Int): String {
        val err = ByteArray(ERR_LEN)
        gl.glGetShaderInfoLog(shader, ERR_LEN, buf, 0, err, 0)
        return String(err, 0, buf[0])
    }

    fun glGetProgramInfoLog(gl: GL2ES2, program: Int): String {
        val err = ByteArray(ERR_LEN)
        gl.glGetProgramInfoLog(program, ERR_LEN, buf, 0, err, 0)
        return String(err, 0, buf[0])
    }

    fun glGenVertexArrays(gl: GL2ES3): Int {
        gl.glGenVertexArrays(1, buf, 0)
        return buf[0]
    }

    fun glDeleteVertexArrays(gl: GL2ES3, vertexArray: Int) {
        buf[0] = vertexArray
        gl.glDeleteVertexArrays(1, buf, 0)
    }

    fun glGenBuffers(gl: GL): Int {
        gl.glGenBuffers(1, buf, 0)
        return buf[0]
    }

    fun glDeleteBuffer(gl: GL, buffer: Int) {
        buf[0] = buffer
        gl.glDeleteBuffers(1, buf, 0)
    }

    fun glDeleteBuffers(gl: GL, buffer: IntArray) {
        gl.glDeleteBuffers(1, buffer, 0)
    }

    fun glGenTexture(gl: GL): Int {
        gl.glGenTextures(1, buf, 0)
        return buf[0]
    }

    fun glDeleteTexture(gl: GL, texture: Int) {
        buf[0] = texture
        gl.glDeleteTextures(1, buf, 0)
    }

    fun glGenFrameBuffer(gl: GL): Int {
        gl.glGenFramebuffers(1, buf, 0)
        return buf[0]
    }

    fun glDeleteFrameBuffer(gl: GL, frameBuffer: Int) {
        buf[0] = frameBuffer
        gl.glDeleteFramebuffers(1, buf, 0)
    }

    fun glGenRenderbuffer(gl: GL): Int {
        gl.glGenRenderbuffers(1, buf, 0)
        return buf[0]
    }

    fun glDeleteRenderbuffers(gl: GL, renderBuffer: Int) {
        buf[0] = renderBuffer
        gl.glDeleteRenderbuffers(1, buf, 0)
    }
}
