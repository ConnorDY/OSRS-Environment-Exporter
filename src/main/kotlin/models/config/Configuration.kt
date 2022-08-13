package models.config

import java.io.File
import java.io.FileWriter
import java.util.Properties

class Configuration {
    private val properties = Properties()
    private var configFile: File = File("config.properties")

    fun saveProp(key: String, value: String) {
        val writer = FileWriter(configFile)
        properties.setProperty(key, value)
        properties.store(writer, "")
    }

    fun getProp(key: String): String {
        return properties.getProperty(key).orEmpty()
    }

    init {
        configFile.createNewFile()
        properties.load(configFile.inputStream())
    }
}
