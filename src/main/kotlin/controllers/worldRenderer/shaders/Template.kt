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
                lineNumbers.add(LineNumber(physicalLines, filename, index + 1))
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
