package models.config

import utils.ObservableValue

data class ConfigOption<T>(
    val id: String,
    val type: ConfigOptionType<T>,
    val default: T,
    val humanReadableName: String = "",
    val mnemonic: Char = 0.toChar(),
) {
    val hidden get() = humanReadableName.isEmpty()
    val value = ObservableValue(default)
}
