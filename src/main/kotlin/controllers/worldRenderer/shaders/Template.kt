package controllers.worldRenderer.shaders

import java.io.InputStream
import java.util.*
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
        return add(Function { f: String? ->
            val inputStream = clazz.getResourceAsStream(f!!)
            if (inputStream != null) {
                inputStreamToString(inputStream)
            } else {
                null
            }
        })
    }

    companion object {
        private fun inputStreamToString(inputStream: InputStream): String {
            val scanner = Scanner(inputStream).useDelimiter("\\A")
            return scanner.next()
        }
    }

    init {
        add(Function { key: String? ->
            if ("version_header" == key) {
                Shader.WINDOWS_VERSION_HEADER
            } else {
                null
            }
        })
    }
}