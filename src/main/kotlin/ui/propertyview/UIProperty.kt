package ui.propertyview

import models.config.ConfigOptionType

interface UIProperty<T, V> {
    val name: String
    val type: ConfigOptionType<V>
    fun get(target: T): V
    fun set(target: T, value: V): Boolean
    fun isEditable(target: T): Boolean
    fun isApplicable(target: T): Boolean
}
