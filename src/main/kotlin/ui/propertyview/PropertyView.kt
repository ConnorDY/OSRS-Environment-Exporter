package ui.propertyview

import models.config.ConfigOptionType
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner

class PropertyView<T>(private val properties: List<UIProperty<T, *>>) : JPanel(GridBagLayout()) {
    var target: T? = null
        set(value) {
            field = value
            reloadProperties()
        }

    private fun reloadProperties() {
        removeAll()
        val target = this.target ?: return
        val gridBagConstraints = GridBagConstraints()
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL
        gridBagConstraints.gridy = 0
        gridBagConstraints.ipadx = 4
        gridBagConstraints.ipady = 4

        properties.forEach { property ->
            if (!property.isApplicable(target)) return@forEach

            gridBagConstraints.gridx = 0
            gridBagConstraints.weightx = 0.0
            add(JLabel(property.name), gridBagConstraints)

            gridBagConstraints.gridx = 1
            gridBagConstraints.weightx = 1.0

            @Suppress("UNCHECKED_CAST") // property types imply cast safety (but kotlin can't infer it)
            val propertyEditor = when (property.type) {
                ConfigOptionType.int -> makeIntPropertyEditor(target, property as UIProperty<T, Int>)
                ConfigOptionType.boolean -> makeBooleanPropertyEditor(target, property as UIProperty<T, Boolean>)
                else -> makeOtherPropertyViewer(target, property)
            }

            add(propertyEditor, gridBagConstraints)

            gridBagConstraints.gridy++
        }

        revalidate()
    }

    private fun makeIntPropertyEditor(target: T, property: UIProperty<T, Int>): JSpinner =
        JSpinner().apply {
            value = property.get(target)
            if (property.isEditable(target)) {
                addChangeListener {
                    property.set(target, value as Int)
                }
            } else {
                isEnabled = false
            }
        }

    private fun makeBooleanPropertyEditor(target: T, property: UIProperty<T, Boolean>): JCheckBox =
        JCheckBox().apply {
            isSelected = property.get(target)
            if (property.isEditable(target)) {
                addActionListener {
                    property.set(target, isSelected)
                }
            } else {
                isEnabled = false
            }
        }

    private fun makeOtherPropertyViewer(target: T, property: UIProperty<T, *>): JLabel =
        JLabel().apply {
            text = property.get(target).toString()
        }
}
