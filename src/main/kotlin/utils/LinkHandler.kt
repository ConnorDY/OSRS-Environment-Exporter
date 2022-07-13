package utils

import org.apache.commons.lang3.SystemUtils
import java.awt.Desktop
import java.net.URI
import java.net.URISyntaxException

class LinkHandler (val url: String) {
    fun openInBrowser() {
        try {
            if (SystemUtils.IS_OS_LINUX) {
                if (Runtime.getRuntime().exec(arrayOf("which", "xdg-open")).getInputStream().read() != 1) {
                    Runtime.getRuntime().exec(arrayOf("xdg-open", url));
                } else {
                    throw Exception("xdg-open not found!")
                }
            } else {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI(url))
                } else {
                    throw Exception("Desktop class not supported!")
                }
            }
        } catch (e: URISyntaxException) {
            throw Exception("Invalid URL: ${url}!")
        }
    }
}
