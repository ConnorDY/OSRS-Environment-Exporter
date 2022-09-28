package controllers

import ui.JActionLabel
import ui.JLinkLabel
import utils.PackageMetadata
import java.awt.Dimension
import java.awt.Frame
import java.awt.font.TextAttribute
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class AboutController(val owner: Frame, title: String) : JDialog(owner, title) {
    init {
        preferredSize = Dimension(600, 300)
        layout = BoxLayout(contentPane, BoxLayout.PAGE_AXIS)
        defaultCloseOperation = DISPOSE_ON_CLOSE

        Box.createGlue().let(::add)
        JLabel(PackageMetadata.NAME, SwingConstants.CENTER).apply {
            alignmentX = CENTER_ALIGNMENT
            font = font.deriveFont(18f)
        }.let(::add)
        JLabel(PackageMetadata.VERSION, SwingConstants.CENTER).apply {
            alignmentX = CENTER_ALIGNMENT
        }.let(::add)
        JLinkLabel("https://github.com/ConnorDY/OSRS-Environment-Exporter", align = SwingConstants.CENTER).apply {
            alignmentX = CENTER_ALIGNMENT
        }.let(::add)
        Box.createRigidArea(Dimension(20, 20)).let(::add)
        JLabel(
            "<html>This application enables exporting Old School RuneScape environments" +
                " so they can be used in 3D modeling programs like Blender.</html>",
            SwingConstants.CENTER
        ).apply {
            alignmentX = CENTER_ALIGNMENT
            border = BorderFactory.createEmptyBorder(0, 20, 0, 20)
        }.let(::add)

        Box.createRigidArea(Dimension(20, 20)).let(::add)

        headingLabel("Credits").let(::add)
        sideBySide(
            JLabel("Original idea by "),
            JLinkLabel("https://twitter.com/TrillionStudios", "Trillion"),
            JLabel("."),
        ).let(::add)
        sideBySide(
            JLabel("Based on "),
            JLinkLabel("https://github.com/tpetrychyn/osrs-map-editor", "@tpetrychyn's OSRS Map Editor"),
            JLabel("."),
        ).let(::add)
        sideBySide(
            JLabel("Using changes from "),
            JLinkLabel("https://github.com/partyvaper/osrs-map-editor", "@partyvaper's fork"),
            JLabel("."),
        ).let(::add)
        Box.createGlue().let(::add)

        headingLabel("Licenses").let(::add)
        sideBySide(
            JLabel("This application uses code from "),
            JLinkLabel("https://runelite.net/", "RuneLite"),
            JLabel(" licensed under the "),
            JActionLabel("BSD 2-Clause").apply {
                addActionListener { showRuneLiteLicense() }
            },
            JLabel(" license."),
        ).let(::add)
        Box.createGlue().let(::add)

        pack()
    }

    private fun headingLabel(text: String) =
        JLabel(text).apply {
            font = font.deriveFont(font.attributes + (TextAttribute.UNDERLINE to TextAttribute.UNDERLINE_ON))
            alignmentX = CENTER_ALIGNMENT
        }

    private fun sideBySide(vararg items: JLabel) = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.LINE_AXIS)
        items.forEach(::add)
    }

    private fun showRuneLiteLicense() {
        val licenseDialog = RuneLiteLicenseController(owner, "RuneLite License")
        licenseDialog.setLocationRelativeTo(owner)
        licenseDialog.isVisible = true
    }
}
