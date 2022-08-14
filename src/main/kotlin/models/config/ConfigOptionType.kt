package models.config

class ConfigOptionType<T>(val convToString: (T) -> String, val convFromString: (String) -> T) {
    companion object {
        val int = ConfigOptionType({ it.toString() }, { it.toInt() })
        val long = ConfigOptionType({ it.toString() }, { it.toLong() })
        val string = ConfigOptionType({ it }, { it })
        val boolean = ConfigOptionType({ it.toString() }, { it.toBooleanStrict() })
        val intToggle = ConfigOptionType({ it?.toString() ?: "off" }, { it.toIntOrNull() })
    }
}
