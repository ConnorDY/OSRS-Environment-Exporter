package controllers.worldRenderer.shaders

import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2ES2
import com.jogamp.opengl.GL4
import controllers.worldRenderer.helpers.GLUtil
import controllers.worldRenderer.helpers.GLUtil.glGetProgramInfoLog
import java.util.*


class Shader {
    private val units: MutableList<Unit> = ArrayList()

    internal class Unit(internal val type: Int, internal val filename: String)

    fun add(type: Int, name: String): Shader {
        units.add(Unit(type, name))
        return this
    }

    @Throws(ShaderException::class)
    fun compile(gl: GL4, template: Template): Int {
        val program = gl.glCreateProgram()
        val shaders = IntArray(units.size)
        var i = 0
        var ok = false
        try {
            while (i < shaders.size) {
                val unit = units[i]
                val shader = gl.glCreateShader(unit.type)
                val source: String = template.load(unit.filename)
                gl.glShaderSource(shader, 1, arrayOf(source), null)
                gl.glCompileShader(shader)
                if (GLUtil.glGetShader(gl, shader, GL2ES2.GL_COMPILE_STATUS) != GL.GL_TRUE) {
                    val err: String = GLUtil.glGetShaderInfoLog(gl, shader)
                    gl.glDeleteShader(shader)
                    throw ShaderException(err)
                }
                gl.glAttachShader(program, shader)
                shaders[i++] = shader
            }
            gl.glLinkProgram(program)
            if (GLUtil.glGetProgram(gl, program, GL2ES2.GL_LINK_STATUS) == GL.GL_FALSE) {
                val err: String = glGetProgramInfoLog(gl, program)
                throw ShaderException(err)
            }
            gl.glValidateProgram(program)
            if (GLUtil.glGetProgram(gl, program, GL2ES2.GL_VALIDATE_STATUS) == GL.GL_FALSE) {
                val err: String = glGetProgramInfoLog(gl, program)
                throw ShaderException(err)
            }
            ok = true
        } finally {
            while (i > 0) {
                val shader = shaders[--i]
                gl.glDetachShader(program, shader)
                gl.glDeleteShader(shader)
            }
            if (!ok) {
                gl.glDeleteProgram(program)
            }
        }
        return program
    }

    companion object {
        const val LINUX_VERSION_HEADER = "#version 420\n" +
                "#extension GL_ARB_compute_shader : require\n" +
                "#extension GL_ARB_shader_storage_buffer_object : require\n" +
                "#extension GL_ARB_explicit_attrib_location : require\n"
        const val WINDOWS_VERSION_HEADER = "#version 430\n"
        val PROGRAM = Shader()
            .add(GL4.GL_VERTEX_SHADER, "/gpu/vert.glsl")
            .add(GL4.GL_GEOMETRY_SHADER, "/gpu/geom.glsl")
            .add(GL4.GL_FRAGMENT_SHADER, "/gpu/frag.glsl")
        val COMPUTE_PROGRAM = Shader()
            .add(GL4.GL_COMPUTE_SHADER, "/gpu/comp.glsl")
        val SMALL_COMPUTE_PROGRAM = Shader()
            .add(GL4.GL_COMPUTE_SHADER, "/gpu/comp_small.glsl")
        val UNORDERED_COMPUTE_PROGRAM = Shader()
            .add(GL4.GL_COMPUTE_SHADER, "/gpu/comp_unordered.glsl")
    }
}