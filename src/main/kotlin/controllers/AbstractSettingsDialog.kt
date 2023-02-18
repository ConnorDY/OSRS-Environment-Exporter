package controllers

import models.config.ConfigOption
import models.config.ConfigOptionType
import ui.NumericTextField
import ui.listener.DocumentTextListener
import java.awt.Component
import java.awt.EventQueue.invokeLater
import java.awt.Frame
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.DefaultListCellRenderer
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JTextField
import kotlin.reflect.KClass
import kotlin.reflect.cast

abstract class AbstractSettingsDialog(owner: Frame, title: String, protected val options: List<ConfigOption<*>>) : JDialog(owner, title) {
    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        layout = GridBagLayout()
        options.forEachIndexed { index, option ->
            @Suppress("UNCHECKED_CAST") when (option.type) {
                ConfigOptionType.int -> makeIntControls(option as ConfigOption<Int>, index)
                ConfigOptionType.intToggle -> makeIntToggleControls(option as ConfigOption<Int?>, index)
                ConfigOptionType.double -> makeDoubleControls(option as ConfigOption<Double>, index)
                ConfigOptionType.string -> makeStringControls(option as ConfigOption<String>, index)
                ConfigOptionType.boolean -> makeBooleanControls(option as ConfigOption<Boolean>, index)
                is ConfigOptionType.Enumerated<*> -> makeEnumeratedControls(
                    option as ConfigOption<out Enum<*>>,
                    index
                )
                else -> TODO("Haven't added other types yet")
            }
        }
    }

    override fun removeNotify() {
        super.removeNotify()
        options.forEach {
            it.value.removeListeners(this)
        }
    }

    private fun makeIntControls(option: ConfigOption<Int>, index: Int) =
        makeNumericControls(option, index, Int.MIN_VALUE, Int.MAX_VALUE, String::toIntOrNull, Int::class)

    private fun makeDoubleControls(option: ConfigOption<Double>, index: Int) =
        makeNumericControls(option, index, -Double.MAX_VALUE, Double.MAX_VALUE, String::toDoubleOrNull, Double::class)

    private fun <T : Comparable<T>> makeNumericControls(
        option: ConfigOption<T>,
        index: Int,
        min: T,
        max: T,
        conversion: (String) -> T?,
        classT: KClass<T>,
    ) {
        val editBox = NumericTextField.create(option.value.get(), min, max, conversion)
        add(
            editBox,
            GridBagConstraints(1, index, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, defaultInset, 0, 0)
        )
        val label = JLabel(option.humanReadableName)
        label.displayedMnemonic = option.mnemonic.code
        label.labelFor = editBox
        add(
            label,
            GridBagConstraints(0, index, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, defaultInset, 0, 0)
        )

        editBox.addPropertyChangeListener {
            if (it.propertyName == "value") {
                option.value.set(classT.cast(it.newValue))
            }
        }
        option.value.addListener(this) {
            editBox.value = it
            editBox.isEnabled = true
        }
    }

    private fun makeIntToggleControls(
        option: ConfigOption<Int?>,
        index: Int
    ) {
        val value = option.value.get()
        val editBox = NumericTextField.create(value ?: option.default ?: 1, Int.MIN_VALUE, Int.MAX_VALUE)
        editBox.isEnabled = value != null
        add(
            editBox,
            GridBagConstraints(1, index, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, defaultInset, 0, 0)
        )
        val checkbox = JCheckBox(option.humanReadableName, editBox.isEnabled)
        checkbox.mnemonic = option.mnemonic.code
        add(
            checkbox,
            GridBagConstraints(0, index, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, defaultInset, 0, 0)
        )

        checkbox.addActionListener {
            val selected = checkbox.isSelected
            editBox.isEnabled = selected
            if (selected) {
                option.value.set(editBox.value as Int?)
            } else {
                option.value.set(null)
            }
        }
        editBox.addPropertyChangeListener {
            if (it.propertyName == "value") {
                option.value.set(it.newValue as Int?)
            }
        }
        option.value.addListener(this) {
            if (it == null) {
                checkbox.isSelected = false
                editBox.isEnabled = false
            } else {
                editBox.value = it
                checkbox.isSelected = true
                editBox.isEnabled = true
            }
        }
    }

    private fun makeBooleanControls(
        option: ConfigOption<Boolean>,
        index: Int
    ) {
        val checkbox = JCheckBox(option.humanReadableName, option.value.get())
        checkbox.mnemonic = option.mnemonic.code
        add(
            checkbox,
            GridBagConstraints(0, index, 2, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, defaultInset, 0, 0)
        )

        checkbox.addActionListener {
            option.value.set(checkbox.isSelected)
        }
        option.value.addListener(this) {
            checkbox.isSelected = it
        }
    }

    private fun makeStringControls(
        option: ConfigOption<String>,
        index: Int
    ) {
        val editBox = JTextField(option.value.get())
        add(
            editBox,
            GridBagConstraints(1, index, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, defaultInset, 0, 0)
        )
        val label = JLabel(option.humanReadableName)
        label.displayedMnemonic = option.mnemonic.code
        label.labelFor = editBox
        add(
            label,
            GridBagConstraints(0, index, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, defaultInset, 0, 0)
        )

        editBox.document.addDocumentListener(
            DocumentTextListener {
                option.value.set(editBox.text)
            }
        )
        option.value.addListener(this) {
            invokeLater { if (editBox.text != it) editBox.text = it }
        }
    }

    private fun <E : Enum<E>> makeEnumeratedControls(
        option: ConfigOption<E>,
        index: Int
    ) {
        val type = option.type as ConfigOptionType.Enumerated<E>
        val editBox = JComboBox(type.enumValues)
        editBox.renderer = CustomListCellRenderer(type.convToHumanReadable)
        editBox.selectedItem = option.value.get()
        add(
            editBox,
            GridBagConstraints(1, index, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, defaultInset, 0, 0)
        )
        val label = JLabel(option.humanReadableName)
        label.displayedMnemonic = option.mnemonic.code
        label.labelFor = editBox
        add(
            label,
            GridBagConstraints(0, index, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, defaultInset, 0, 0)
        )

        editBox.addActionListener {
            val selected = editBox.selectedIndex
            if (selected != -1)
                option.value.set(editBox.getItemAt(selected))
        }
        option.value.addListener(this) {
            editBox.selectedItem = it
        }
    }

    private class CustomListCellRenderer<T>(val stringify: (T) -> String) : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val result = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (value != null) {
                @Suppress("UNCHECKED_CAST")
                (result as JLabel).text = stringify(value as T)
            }
            return result
        }
    }

    companion object {
        val defaultInset = Insets(4, 4, 4, 4)
    }
}
