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
package controllers.worldRenderer.shaders

import org.lwjgl.opengl.GL11C.GL_FALSE
import org.lwjgl.opengl.GL11C.GL_TRUE
import org.lwjgl.opengl.GL20C.GL_COMPILE_STATUS
import org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER
import org.lwjgl.opengl.GL20C.GL_LINK_STATUS
import org.lwjgl.opengl.GL20C.GL_VALIDATE_STATUS
import org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER
import org.lwjgl.opengl.GL20C.glAttachShader
import org.lwjgl.opengl.GL20C.glCompileShader
import org.lwjgl.opengl.GL20C.glCreateProgram
import org.lwjgl.opengl.GL20C.glCreateShader
import org.lwjgl.opengl.GL20C.glDeleteProgram
import org.lwjgl.opengl.GL20C.glDeleteShader
import org.lwjgl.opengl.GL20C.glDetachShader
import org.lwjgl.opengl.GL20C.glGetProgramInfoLog
import org.lwjgl.opengl.GL20C.glGetProgrami
import org.lwjgl.opengl.GL20C.glGetShaderInfoLog
import org.lwjgl.opengl.GL20C.glGetShaderi
import org.lwjgl.opengl.GL20C.glLinkProgram
import org.lwjgl.opengl.GL20C.glShaderSource
import org.lwjgl.opengl.GL20C.glValidateProgram
import org.lwjgl.opengl.GL43C.GL_COMPUTE_SHADER

class Shader {
    private val units: MutableList<Unit> = ArrayList()

    internal class Unit(internal val type: Int, internal val filename: String)

    fun add(type: Int, name: String): Shader {
        units.add(Unit(type, name))
        return this
    }

    @Throws(ShaderException::class)
    fun compile(template: Template): Int {
        val program = glCreateProgram()
        val shaders = IntArray(units.size)
        var i = 0
        var ok = false
        try {
            while (i < shaders.size) {
                val unit = units[i]
                val shader = glCreateShader(unit.type)
                if (shader == 0)
                    throw ShaderException("Unable to create shader of type ${unit.type}")
                val processedFile = template.load(unit.filename)
                val source = processedFile.contents
                glShaderSource(shader, source)
                glCompileShader(shader)
                if (glGetShaderi(shader, GL_COMPILE_STATUS) != GL_TRUE) {
                    val err: String = glGetShaderInfoLog(shader)
                    glDeleteShader(shader)
                    throw ShaderException(reformatErrorString(err, processedFile))
                }
                glAttachShader(program, shader)
                shaders[i++] = shader
            }
            glLinkProgram(program)
            if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
                val err: String = glGetProgramInfoLog(program)
                throw ShaderException(err)
            }
            glValidateProgram(program)
            if (glGetProgrami(program, GL_VALIDATE_STATUS) == GL_FALSE) {
                val err: String = glGetProgramInfoLog(program)
                throw ShaderException(err)
            }
            ok = true
        } finally {
            while (i > 0) {
                val shader = shaders[--i]
                glDetachShader(program, shader)
                glDeleteShader(shader)
            }
            if (!ok) {
                glDeleteProgram(program)
            }
        }
        return program
    }

    private fun reformatErrorString(
        err: String,
        processedFile: Template.ProcessedFile
    ): String =
        // Extract and transform line numbers from error
        err.split('\n').joinToString("\n") { errorLine ->
            val matchTriplet = run {
                val mesaMatch = ERROR_LINE_REGEX_MESA.matchEntire(errorLine)
                if (mesaMatch != null) return@run Triple(mesaMatch.groupValues[1], mesaMatch.groupValues[2], mesaMatch.groupValues[3])
                val nvidiaMatch = ERROR_LINE_REGEX_NVIDIA.matchEntire(errorLine)
                if (nvidiaMatch != null) return@run Triple(nvidiaMatch.groupValues[1], "", nvidiaMatch.groupValues[2])
                null
            } ?: return@joinToString errorLine
            val (lineNumStr, columnNumStr, message) = matchTriplet
            val lineNumber = lineNumStr.toInt()
            if (lineNumber < 1) return@joinToString errorLine

            val columnStr = if (columnNumStr.isNotEmpty()) "($columnNumStr)" else ""
            val lineNumInfo = processedFile.getLineNumber(lineNumber)
            "$lineNumInfo$columnStr:$message"
        }

    companion object {
        // Line format: `0:123(10): error: ...`
        private val ERROR_LINE_REGEX_MESA = Regex("(?:ERROR:\\s*)?\\d+:(\\d+)(?:\\((\\d+)\\))?\\s*: (.*)")
        // Line format: `123(10) : error : ...`
        private val ERROR_LINE_REGEX_NVIDIA = Regex("\\d+\\((\\d+)\\)\\s*: (.*)")
        const val VERSION_HEADER = "#version 430\n"
        val PROGRAM = lazy {
            Shader()
                .add(GL_VERTEX_SHADER, "/gpu/vert.glsl")
                .add(GL_FRAGMENT_SHADER, "/gpu/frag.glsl")
        }
        val COMPUTE_PROGRAM = lazy {
            Shader()
                .add(GL_COMPUTE_SHADER, "/gpu/comp.glsl")
        }
        val UNORDERED_COMPUTE_PROGRAM = lazy {
            Shader()
                .add(GL_COMPUTE_SHADER, "/gpu/comp_unordered.glsl")
        }

        fun createTemplate(threadCount: Int, facesPerThread: Int): Template {
            val versionHeader: String = VERSION_HEADER
            val template = Template()
            template.add { key ->
                if ("version_header" == key) {
                    return@add versionHeader
                }
                if ("thread_config" == key) {
                    return@add """
                #define THREAD_COUNT $threadCount
                #define FACES_PER_THREAD $facesPerThread
                
                    """.trimIndent()
                }
                null
            }
            template.addInclude(Shader::class.java)
            return template
        }
    }
}
