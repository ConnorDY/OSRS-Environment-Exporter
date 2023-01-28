package ui

import java.awt.Desktop
import java.net.URI
import javax.swing.SwingConstants

class JLinkLabel(uri: String, display: String = uri, align: Int = SwingConstants.LEADING) : JActionLabel(display, align) {
    init {
        val realURI = if (uri.startsWith("http")) uri else "https://$uri"
        addActionListener { visitLink(realURI) }
    }

    private fun visitLink(link: String) {
        try {
            Desktop.getDesktop().browse(URI(link))
        } catch (e: UnsupportedOperationException) {
            // Linux, which Java doesn't usually support
            Runtime.getRuntime().exec(arrayOf("xdg-open", link))
        }
    }
}
