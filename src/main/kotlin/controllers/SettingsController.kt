package controllers

import models.config.ConfigOption
import models.config.ConfigOptionType
import models.config.ConfigOptions
import ui.NumericTextField
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagConstraints.LINE_START
import java.awt.GridBagConstraints.NONE
import java.awt.GridBagConstraints.PAGE_START
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList

class SettingsController(
    owner: JFrame,
    title: String,
    configOptions: ConfigOptions
) : JDialog(owner, title) {
    private val visibleOptions: List<ConfigOption<*>>

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        layout = GridBagLayout()

        val btnSave = JButton("Save Preferences").apply {
            mnemonic = 'S'.code
        }

        visibleOptions = configOptions.all
            .filter { !it.hidden }
        visibleOptions
            .forEachIndexed { index, option ->
                @Suppress("UNCHECKED_CAST") when (option.type) {
                    ConfigOptionType.int -> makeIntControls(option as ConfigOption<Int>, index)
                    ConfigOptionType.intToggle -> makeIntToggleControls(option as ConfigOption<Int?>, index)
                    ConfigOptionType.boolean -> makeBooleanControls(option as ConfigOption<Boolean>, index)
                    is ConfigOptionType.Enumerated<*> -> makeEnumeratedControls(option as ConfigOption<out Enum<*>>, index)
                    else -> TODO("Haven't added other types yet")
                }
            }
        add(
            btnSave,
            GridBagConstraints(0, visibleOptions.size, 2, 1, 0.0, 0.0, PAGE_START, NONE, defaultInset, 0, 0)
        )

        // Save Preferences button handler
        btnSave.addActionListener {
            configOptions.save()
            visibleOptions.forEach {
                it.value.removeListeners(this@SettingsController)
            }
            dispose()
        }

        rootPane.defaultButton = btnSave
        pack()
    }

    override fun removeNotify() {
        super.removeNotify()
        visibleOptions.forEach {
            it.value.removeListeners(this)
        }
    }

    private fun makeIntControls(
        option: ConfigOption<Int>,
        index: Int
    ) {
        val editBox = NumericTextField.create(
            option.value.get(),
            Int.MIN_VALUE,
            Int.MAX_VALUE
        )
        add(
            editBox,
            GridBagConstraints(1, index, 1, 1, 0.0, 0.0, LINE_START, NONE, defaultInset, 0, 0)
        )
        val label = JLabel(option.humanReadableName)
        label.displayedMnemonic = option.mnemonic.code
        label.labelFor = editBox
        add(
            label,
            GridBagConstraints(0, index, 1, 1, 0.0, 0.0, LINE_START, NONE, defaultInset, 0, 0)
        )

        editBox.addPropertyChangeListener {
            if (it.propertyName == "value") {
                option.value.set(it.newValue as Int)
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
            GridBagConstraints(1, index, 1, 1, 0.0, 0.0, LINE_START, NONE, defaultInset, 0, 0)
        )
        val checkbox = JCheckBox(option.humanReadableName, editBox.isEnabled)
        checkbox.mnemonic = option.mnemonic.code
        add(
            checkbox,
            GridBagConstraints(0, index, 1, 1, 0.0, 0.0, LINE_START, NONE, defaultInset, 0, 0)
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
            GridBagConstraints(0, index, 2, 1, 0.0, 0.0, LINE_START, NONE, defaultInset, 0, 0)
        )

        checkbox.addActionListener {
            option.value.set(checkbox.isSelected)
        }
        option.value.addListener(this) {
            checkbox.isSelected = it
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
            GridBagConstraints(1, index, 1, 1, 0.0, 0.0, LINE_START, NONE, defaultInset, 0, 0)
        )
        val label = JLabel(option.humanReadableName)
        label.displayedMnemonic = option.mnemonic.code
        label.labelFor = editBox
        add(
            label,
            GridBagConstraints(0, index, 1, 1, 0.0, 0.0, LINE_START, NONE, defaultInset, 0, 0)
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
