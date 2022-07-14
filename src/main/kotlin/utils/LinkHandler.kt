package utils

import java.awt.Desktop
import java.io.IOException
import java.net.URI

class LinkHandler(val url: String) {
    fun openInBrowser() {
        if (!openLinkForLinux()) {
            Desktop.getDesktop().browse(URI(url))
        }
    }

    private fun openLinkForLinux(): Boolean {
        try {
            Runtime.getRuntime().exec(arrayOf("xdg-open", url));
            return true
        } catch (e: SecurityException) {
        } catch (e: IOException) {
        }

        return false
    }
}
