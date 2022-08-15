package models.config

import java.io.File
import java.io.FileWriter
import java.util.Properties

class Configuration {
    private val properties = Properties()
    private var configFile: File = File("config.properties")

    fun <T> setProp(key: ConfigOption<T>, value: T): Boolean {
        val id = key.id
        val valueString = key.type.convToString(value)
        val old = properties.getProperty(id)
        return if (old == valueString) {
            false
        } else {
            properties.setProperty(id, valueString)
            true
        }
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
