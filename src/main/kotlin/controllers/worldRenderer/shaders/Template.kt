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
    private val resourceLoaders: MutableList<Function<String, String?>> = ArrayList()

    private fun process(str: String): String {
        val sb = StringBuilder()
        for (line in str.split("\r?\n".toRegex()).toTypedArray()) {
            if (line.startsWith("#include ")) {
                var resource = line.substring(9)
                if (line.contains(".glsl")) {
                    resource = "/gpu/$resource"
                }
                val resourceStr = load(resource)
                sb.append(resourceStr)
            } else {
                sb.append(line).append('\n')
            }
        }
        return sb.toString()
    }

    fun load(fileNameOrContents: String): String {
        for (loader in resourceLoaders) {
            val value = loader.apply(fileNameOrContents)
            if (value != null) {
                return process(value)
            }
        }
        return ""
    }

    fun add(fn: Function<String, String?>): Template {
        resourceLoaders.add(fn)
        return this
    }

    fun addInclude(clazz: Class<*>): Template {
        return add(
            Function { f: String? ->
                val inputStream = clazz.getResourceAsStream(f!!)
                if (inputStream != null) {
                    inputStreamToString(inputStream)
                } else {
                    null
                }
            }
        )
    }

    companion object {
        private fun inputStreamToString(inputStream: InputStream): String {
            val scanner = Scanner(inputStream).useDelimiter("\\A")
            return scanner.next()
        }
    }
}
