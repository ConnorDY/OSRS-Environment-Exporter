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

import java.io.InputStream
import java.util.Scanner
import java.util.function.Function

class Template {
    data class LineNumber(val physicalLine: Int, val filename: String, val logicalLine: Int) {
        override fun toString(): String = "$filename:$logicalLine"
    }

    data class ProcessedFile(val contents: String, val lineNumbers: List<LineNumber>, val physicalLines: Int) {
        fun getLineNumber(physicalLine: Int): LineNumber {
            val index = lineNumbers.binarySearchBy(physicalLine) { it.physicalLine }
            if (index >= 0) {
                return lineNumbers[index]
            }
            val insertionPoint = -index - 1
            if (insertionPoint == 0) {
                throw IllegalArgumentException("Requested unreasonable line number $physicalLine")
            }
            val original = lineNumbers[insertionPoint - 1]
            return LineNumber(physicalLine, original.filename, original.logicalLine + physicalLine - original.physicalLine)
        }
    }

    private val resourceLoaders: MutableList<Function<String, String?>> = ArrayList()

    private fun process(filename: String, str: String): ProcessedFile {
        val sb = StringBuilder()
        val lineNumbers = mutableListOf(LineNumber(1, filename, 1))
        var physicalLines = 0
        for ((index, line) in str.split("\r?\n".toRegex()).toTypedArray().withIndex()) {
            if (line.startsWith("#include ")) {
                var resource = line.substring(9)
                if (line.contains(".glsl")) {
                    resource = "/gpu/$resource"
                }
                val resourceLoaded = load(resource)
                sb.append(resourceLoaded.contents)
                lineNumbers.addAll(
                    resourceLoaded.lineNumbers.map {
                        LineNumber(it.physicalLine + physicalLines, it.filename, it.logicalLine)
                    }
                )
                physicalLines += resourceLoaded.physicalLines
                // Insert a line number at the next line (i.e. index + next line + start at 1) which takes us back to this file
                lineNumbers.add(LineNumber(physicalLines + 1, filename, index + 2))
            } else {
                sb.append(line).append('\n')
                physicalLines++
            }
        }
        return ProcessedFile(sb.toString(), lineNumbers, physicalLines)
    }

    fun load(filename: String): ProcessedFile {
        for (loader in resourceLoaders) {
            val value = loader.apply(filename)
            if (value != null) {
                return process(filename, value)
            }
        }
        return ProcessedFile("", emptyList(), 0)
    }

    fun add(fn: Function<String, String?>): Template {
        resourceLoaders.add(fn)
        return this
    }

    fun addInclude(clazz: Class<*>): Template {
        return add { f: String? ->
            val inputStream = clazz.getResourceAsStream(f!!) ?: return@add null
            inputStreamToString(inputStream)
        }
    }

    companion object {
        private fun inputStreamToString(inputStream: InputStream): String {
            val scanner = Scanner(inputStream).useDelimiter("\\A")
            return scanner.next()
        }
    }
}
