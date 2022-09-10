package models.config

open class ConfigOptionType<T>(val convToString: (T) -> String, val convFromString: (String) -> T) {
    companion object {
        val int = ConfigOptionType({ it.toString() }, { it.toInt() })
        val long = ConfigOptionType({ it.toString() }, { it.toLong() })
        val double = ConfigOptionType({ it.toString() }, { it.toDouble() })
        val string = ConfigOptionType({ it }, { it })
        val boolean = ConfigOptionType({ it.toString() }, { it.toBooleanStrict() })
        val intToggle = ConfigOptionType({ it?.toString() ?: "off" }, { it.toIntOrNull() })
    }

    class Enumerated<T : Enum<T>>(enumValueOf: (String) -> T, val enumValues: Array<T>, val convToHumanReadable: (T) -> String) : ConfigOptionType<T>({ it.name }, enumValueOf)
}
