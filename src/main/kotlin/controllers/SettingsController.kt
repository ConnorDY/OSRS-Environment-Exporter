package controllers

import models.config.ConfigOptions
import java.awt.GridBagConstraints
import java.awt.GridBagConstraints.NONE
import java.awt.GridBagConstraints.PAGE_START
import javax.swing.JButton
import javax.swing.JFrame

class SettingsController(
    owner: JFrame,
    title: String,
    configOptions: ConfigOptions
) : AbstractSettingsDialog(owner, title, configOptions.all) {
    init {
        val btnSave = JButton("Save Preferences").apply {
            mnemonic = 'S'.code
        }
        add(
            btnSave,
            GridBagConstraints(0, options.size, 2, 1, 0.0, 0.0, PAGE_START, NONE, defaultInset, 0, 0)
        )

        // Save Preferences button handler
        btnSave.addActionListener {
            configOptions.save()
            dispose()
        }

        rootPane.defaultButton = btnSave
        pack()
    }
}
