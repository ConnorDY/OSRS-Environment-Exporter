package ui.propertyview

import models.config.ConfigOptionType

class SimpleUIProperty<T, V>(
    override val name: String,
    override val type: ConfigOptionType<V>,
    private val getter: (T) -> V,
    private val setter: (T, V) -> Unit,
    private val editable: Boolean = true,
) : UIProperty<T, V> {
    override fun get(target: T): V = getter(target)
    override fun set(target: T, value: V): Boolean {
        setter(target, value)
        return true
    }

    override fun isEditable(target: T): Boolean = editable
    override fun isApplicable(target: T): Boolean = true
}
