package controllers

import java.awt.Dimension
import java.awt.Frame
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingConstants

class LicenseViewerController(owner: Frame, title: String, project: String) : JDialog(owner, title) {
    init {
        preferredSize = Dimension(680, 520)
        layout = BoxLayout(contentPane, BoxLayout.PAGE_AXIS)
        defaultCloseOperation = DISPOSE_ON_CLOSE

        val panel = JPanel()
        val scrollPane = JScrollPane(panel)

        panel.layout = BoxLayout(panel, BoxLayout.PAGE_AXIS)

        Box.createGlue().let(panel::add)
        JLabel("Portions of $project are used in this program, provided to us under the following license:", SwingConstants.CENTER).apply {
            alignmentX = CENTER_ALIGNMENT
        }.let(panel::add)
        JLabel(getPackagedLicense(project).preformat(), SwingConstants.CENTER).apply {
            alignmentX = CENTER_ALIGNMENT
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }.let(panel::add)
        Box.createGlue().let(panel::add)

        scrollPane.let(::add)

        pack()
    }

    private fun getPackagedLicense(licenseName: String) =
        javaClass.getResourceAsStream("/licenses/$licenseName-LICENSE.txt")!!.bufferedReader().use { it.readText() }

    private fun String.preformat() = "<html><pre>${replace("\n", "<br />")}</pre></html>"
}
