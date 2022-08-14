package controllers

import controllers.worldRenderer.Renderer
import models.config.ConfigOption
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

class SettingsController(
    owner: JFrame,
    title: String,
    private val renderer: Renderer,
    private val configuration: Configuration
) : JDialog(owner, title) {
    init {
        layout = GridBagLayout()

        val chkBoxLimitFps = JCheckBox("Limit FPS").apply {
            mnemonic = 'F'.code
        }
        val txtFpsCap = NumericTextField.create(60, 1, 9999)

        val chkBoxCheckForUpdates = JCheckBox("Check for updates").apply {
            mnemonic = 'U'.code
        }

        val btnSave = JButton("Save Preferences").apply {
            mnemonic = 'S'.code
        }

        val inset = Insets(4, 4, 4, 4)
        add(
            chkBoxLimitFps,
            GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, LINE_END, NONE, inset, 0, 0)
        )
        add(
            txtFpsCap,
            GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, LINE_START, NONE, inset, 0, 0)
        )
        add(
            chkBoxCheckForUpdates,
            GridBagConstraints(0, 1, 2, 1, 0.0, 0.0, PAGE_START, NONE, inset, 0, 0)
        )
        add(
            btnSave,
            GridBagConstraints(0, 2, 2, 1, 0.0, 0.0, PAGE_START, NONE, inset, 0, 0)
        )

        // load current setting(s)
        val fpsCap = configuration.getProp(ConfigOption.fpsCap)

        if (fpsCap != null) {
            txtFpsCap.value = fpsCap
            chkBoxLimitFps.isSelected = true
        } else {
            txtFpsCap.isEnabled = false
        }

        chkBoxCheckForUpdates.isSelected = configuration.getProp(ConfigOption.checkForUpdates)

        // FPS Limit checkbox handler
        chkBoxLimitFps.addChangeListener {
            txtFpsCap.isEnabled = chkBoxLimitFps.isSelected
        }

        // Save Preferences button handler
        btnSave.addActionListener {
            configuration.saveProp(ConfigOption.fpsCap, if (chkBoxLimitFps.isSelected) txtFpsCap.value as Int else null)
            configuration.saveProp(ConfigOption.checkForUpdates, chkBoxCheckForUpdates.isSelected)

            var fpsCapToSet = txtFpsCap.text.toIntOrNull() ?: 0
            if (!chkBoxLimitFps.isSelected) fpsCapToSet = 0

            renderer.setFpsTarget(fpsCapToSet)

            dispose()
        }

        rootPane.defaultButton = btnSave
        pack()
    }
}
