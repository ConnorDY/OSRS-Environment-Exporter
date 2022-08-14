package models.config

import java.io.File
import java.io.FileWriter
import java.util.Properties

class Configuration {
    private val properties = Properties()
    private var configFile: File = File("config.properties")

    fun <T> saveProp(key: ConfigOption<T>, value: T) {
        setProp(key, value)
        save()
    }

    fun <T> setProp(key: ConfigOption<T>, value: T) {
        properties.setProperty(key.id, key.type.convToString(value))
    }

    fun save() {
        val writer = FileWriter(configFile)
        properties.store(writer, "")
    }

    fun <T> getProp(key: ConfigOption<T>): T {
        val value = properties.getProperty(key.id) ?: return key.default
        return try {
            key.type.convFromString(value)
        } catch (iae: IllegalArgumentException) {
            key.default
        }
    }

    init {
        configFile.createNewFile()
        properties.load(configFile.inputStream())
    }
}
