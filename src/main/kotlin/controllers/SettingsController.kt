package controllers

import controllers.SettingsController.SaveFunc
import controllers.worldRenderer.Renderer
import models.config.ConfigOption
import models.config.ConfigOption.Companion.fpsCap
import models.config.ConfigOptionType
import models.config.Configuration
import ui.NumericTextField
import java.awt.GridBagConstraints
import java.awt.GridBagConstraints.LINE_END
import java.awt.GridBagConstraints.LINE_START
import java.awt.GridBagConstraints.NONE
import java.awt.GridBagConstraints.PAGE_START
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel

class SettingsController(
    owner: JFrame,
    title: String,
    private val renderer: Renderer,
    private val configuration: Configuration
) : JDialog(owner, title) {
    init {
        layout = GridBagLayout()

        val btnSave = JButton("Save Preferences").apply {
            mnemonic = 'S'.code
        }

        val options = ConfigOption.all
            .filter { !it.hidden }
            .mapIndexed { index, option ->
                @Suppress("UNCHECKED_CAST") when (option.type) {
                    ConfigOptionType.int -> makeIntControls(option as ConfigOption<Int>, index)
                    ConfigOptionType.intToggle -> makeIntToggleControls(option as ConfigOption<Int?>, index)
                    ConfigOptionType.boolean -> makeBooleanControls(option as ConfigOption<Boolean>, index)
                    else -> TODO("Haven't added other types yet")
                }
            }
        add(
            btnSave,
            GridBagConstraints(0, options.size, 2, 1, 0.0, 0.0, PAGE_START, NONE, defaultInset, 0, 0)
        )

        // Save Preferences button handler
        btnSave.addActionListener {
            options.forEach { it.save() }
            configuration.save()

            renderer.setFpsTarget(configuration.getProp(fpsCap) ?: 0)

            dispose()
        }

        rootPane.defaultButton = btnSave
        pack()
    }

    private fun makeIntControls(
        option: ConfigOption<Int>,
        index: Int
    ): SaveFunc {
        val editBox = NumericTextField.create(
            configuration.getProp(option),
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
            GridBagConstraints(0, index, 1, 1, 0.0, 0.0, LINE_END, NONE, defaultInset, 0, 0)
        )
        return SaveFunc { configuration.setProp(option, editBox.value as Int) }
    }

    private fun makeIntToggleControls(
        option: ConfigOption<Int?>,
        index: Int
    ): SaveFunc {
        val value = configuration.getProp(option)
        val editBox = NumericTextField.create(value ?: option.default ?: 1, Int.MIN_VALUE, Int.MAX_VALUE)
        editBox.isEnabled = value != null
        add(
            editBox,
            GridBagConstraints(1, index, 1, 1, 0.0, 0.0, LINE_START, NONE, defaultInset, 0, 0)
        )
        val checkbox = JCheckBox(option.humanReadableName, editBox.isEnabled)
        checkbox.mnemonic = option.mnemonic.code
        checkbox.addChangeListener {
            editBox.isEnabled = checkbox.isSelected
        }
        add(
            checkbox,
            GridBagConstraints(0, index, 1, 1, 0.0, 0.0, LINE_END, NONE, defaultInset, 0, 0)
        )
        return SaveFunc { configuration.setProp(option, if (checkbox.isSelected) editBox.value as Int else null) }
    }

    private fun makeBooleanControls(
        option: ConfigOption<Boolean>,
        index: Int
    ): SaveFunc {
        val value = configuration.getProp(option)
        val checkbox = JCheckBox(option.humanReadableName, value)
        checkbox.mnemonic = option.mnemonic.code
        add(
            checkbox,
            GridBagConstraints(0, index, 2, 1, 0.0, 0.0, LINE_START, NONE, defaultInset, 0, 0)
        )
        return SaveFunc { configuration.setProp(option, checkbox.isSelected) }
    }

    private fun interface SaveFunc {
        fun save()
    }

    companion object {
        val defaultInset = Insets(4, 4, 4, 4)
    }
}
