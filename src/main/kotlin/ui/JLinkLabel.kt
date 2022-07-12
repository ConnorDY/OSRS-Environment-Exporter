package ui

import java.awt.Color
import java.awt.Desktop
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.font.TextAttribute
import java.net.URI
import javax.swing.JLabel
import javax.swing.SwingConstants

class JLinkLabel(uri: String, display: String = uri, align: Int = SwingConstants.LEADING) : JLabel(display, align) {
    init {
        val realURI = if (uri.startsWith("http")) uri else "https://$uri"
        addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    if (e?.button == MouseEvent.BUTTON1) {
                        visitLink(realURI)
                    }
                }
            }
        )
        addKeyListener(
            object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent?) {
                    if (e?.keyCode == KeyEvent.VK_ENTER) {
                        visitLink(realURI)
                    }
                }
            }
        )
        font = font.let {
            val attr = HashMap(it.attributes)
            attr[TextAttribute.UNDERLINE] = TextAttribute.UNDERLINE_ON
            it.deriveFont(attr)
        }
        foreground = Color.BLUE
        isFocusable = true
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
