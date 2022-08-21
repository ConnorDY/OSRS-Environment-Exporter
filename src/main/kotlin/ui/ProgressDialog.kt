package ui

import utils.ProgressContainer
import java.awt.Frame
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JButton
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
//        setDefaultLookAndFeelDecorated(true)
        rootPane.windowDecorationStyle = JRootPane.PLAIN_DIALOG
        rootPane.windowDecorationStyle = JRootPane.INFORMATION_DIALOG

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
