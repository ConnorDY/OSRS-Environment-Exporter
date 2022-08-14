package models.config

import java.io.File
import java.io.FileWriter
import java.util.Properties

class Configuration {
    private val properties = Properties()
    private var configFile: File = File("config.properties")

    fun <T> saveProp(key: ConfigOption<T>, value: T) {
        val writer = FileWriter(configFile)
        properties.setProperty(key.id, key.type.convToString(value))
        properties.store(writer, "")
    }

    fun <T> getProp(key: ConfigOption<T>): T {
        val value = properties.getProperty(key.id) ?: return key.default
        return key.type.convFromString(value) ?: key.default
    }

    init {
        configFile.createNewFile()
        properties.load(configFile.inputStream())
    }
}
