package cache

import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException

class ParamsManager {
    private val logger = LoggerFactory.getLogger(ParamsManager::class.java)
    private val paramsMap: HashMap<Int, String> = HashMap()

    fun getParam(type: ParamType): String? {
        return paramsMap[type.id]
    }

    fun loadFromPath(path: String) {
        val paramsFile = File(path, "params.txt")
        val reader = try {
            paramsFile.bufferedReader()
        } catch (e: FileNotFoundException) {
            logger.warn("Missing params.txt in cache directory at $path", e)
            return
        }
        parseParams(reader)
    }

    fun parseParams(reader: BufferedReader) {
        paramsMap.clear()
        reader.forEachLine { line ->
            if (!line.contains("param=")) {
                return@forEachLine
            }
            val id = line.substringAfter("param=").substringBefore("=")
            paramsMap[id.toInt()] = line.substringAfter("param=$id=")
        }
    }
}
