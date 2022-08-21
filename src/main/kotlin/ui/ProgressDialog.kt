package ui

import org.pushingpixels.radiance.theming.internal.ui.RadianceButtonUI
import utils.ProgressContainer
import java.awt.Component
import java.awt.Container
import java.awt.Frame
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.util.LinkedList
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JProgressBar
import javax.swing.JRootPane
import javax.swing.Timer

class ProgressDialog(owner: Frame?, title: String, message: String) : JDialog(owner, title), ProgressContainer {
    private val progressBar: JProgressBar
    private val btnCancel: JButton

    override var progress: Int
        get() = progressBar.value
        set(value) {
            progressBar.value = value
        }
    override var progressMax: Int
        get() = progressBar.maximum
        set(value) {
            progressBar.maximum = value
        }
    override var status: String = "Initialising operation, please wait..."
        set(value) {
            field = value
            progressBar.string = value
        }
    override var isCancelled: Boolean = false

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        layout = GridBagLayout()
        isUndecorated = true
        rootPane.windowDecorationStyle = JRootPane.PLAIN_DIALOG
        removeCloseButton()

        val mainLabel = JLabel(message)
        progressBar = JProgressBar(0, 100).also {
            it.isStringPainted = true
            it.string = status
        }
        btnCancel = JButton("Cancel").also {
            it.mnemonic = 'C'.code
        }

        val defaultInsets = Insets(10, 10, 10, 10)
        add(
            progressBar,
            GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                gridwidth = 2
                fill = GridBagConstraints.HORIZONTAL
                weightx = 1.0
                insets = defaultInsets
            }
        )
        add(
            mainLabel,
            GridBagConstraints().apply {
                gridx = 0
                gridy = 1
                gridwidth = 1
                fill = GridBagConstraints.HORIZONTAL
                weightx = 1.0
                insets = defaultInsets
            }
        )
        add(
            btnCancel,
            GridBagConstraints().apply {
                gridx = 1
                gridy = 1
                fill = GridBagConstraints.HORIZONTAL
                insets = defaultInsets
            }
        )

        btnCancel.addActionListener {
            isCancelled = true
            btnCancel.isEnabled = false
        }

        pack()
    }

    private fun removeCloseButton() {
        rootPane.findSubComponent {
            it is JComponent && it.getClientProperty(RadianceButtonUI.IS_TITLE_CLOSE_BUTTON) == true
        }?.isVisible = false
    }

    private fun Container.findSubComponent(action: (Component) -> Boolean): Component? {
        val queue = LinkedList<Component>()
        queue.add(this)

        while (queue.isNotEmpty()) {
            val component = queue.poll()

            if (action(component)) {
                return component
            }

            if (component is Container) {
                queue.addAll(component.components)
            }
        }

        return null
    }

    override fun complete() {
        btnCancel.isEnabled = false

        Timer(300) {
            dispose()
        }.apply {
            isRepeats = false
            start()
        }
    }
}
