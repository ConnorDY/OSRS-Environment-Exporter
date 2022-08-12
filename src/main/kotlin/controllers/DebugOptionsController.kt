package controllers

import models.DebugOptionsModel
import ui.NumericTextField
import utils.ObservableValue
import java.awt.Frame
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JDialog
import javax.swing.SwingUtilities

class DebugOptionsController(owner: Frame, model: DebugOptionsModel): JDialog(owner, "Debug Options", false) {
    init {
        layout = BoxLayout(contentPane, BoxLayout.PAGE_AXIS)
        makeDebugToggle(model.onlyType10Models, "Only type-10 models", '1').let(::add)
        makeDebugToggle(model.resetCameraOnSceneChange, "Reset camera on scene change", 'R').let(::add)
        makeDebugToggle(model.removeProperlyTypedModels, "Remove properly-typed models", 'E').let(::add)
        makeDebugNumEdit(model.modelSubIndex, "Model sub-index", 'S').let(::add)
        makeDebugNumEdit(model.badModelIndexOverride, "Bad model index override", 'B').let(::add)
        pack()
    }

    private fun makeDebugToggle(prop: ObservableValue<Boolean>, name: String, mnemonic: Char) =
        JCheckBox(name).apply {
            this.mnemonic = mnemonic.code
            isSelected = prop.get()
            addActionListener {
                prop.set(isSelected)
            }
            prop.addListener {
                SwingUtilities.invokeLater {
                    isSelected = it
                }
            }
        }

    private fun makeDebugNumEdit(prop: ObservableValue<Int>, name: String, mnemonic: Char) =
        NumericTextField.create(prop.get(), Int.MIN_VALUE, Int.MAX_VALUE).apply {
            this.focusAccelerator = mnemonic
            toolTipText = name
            addActionListener {
                prop.set(value as Int)
            }
            prop.addListener {
                SwingUtilities.invokeLater {
                    value = it
                }
            }
        }
}
