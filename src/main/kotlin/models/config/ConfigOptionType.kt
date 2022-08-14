package models.config

class ConfigOptionType<T>(val convToString: (T) -> String, val convFromString: (String) -> T?) {
    companion object {
        val int = ConfigOptionType({ it.toString() }, { it.toIntOrNull() })
        val long = ConfigOptionType({ it.toString() }, { it.toLongOrNull() })
        val string = ConfigOptionType({ it }, { it })
        val boolean = ConfigOptionType({ it.toString() }, { it.toBooleanStrictOrNull() })
        val intToggle = ConfigOptionType<Int?>({ it?.toString() ?: "off" }, { it.toIntOrNull() })
    }
}
