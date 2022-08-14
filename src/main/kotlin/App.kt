import controllers.CacheChooserController
import models.config.Configuration
import org.pushingpixels.radiance.theming.api.skin.RadianceGraphiteAquaLookAndFeel
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities
import javax.swing.ToolTipManager
import javax.swing.UIManager
import javax.swing.UnsupportedLookAndFeelException

private fun setSysProperty(key: String, value: String) {
    if (System.getProperty(key) == null) {
        System.setProperty(key, value)
    }
}

fun main() {
    setSysProperty("awt.useSystemAAFontSettings", "on")
    setSysProperty("swing.aatext", "true")

    SwingUtilities.invokeLater {
        try {
            UIManager.setLookAndFeel(RadianceGraphiteAquaLookAndFeel())
        } catch (_: UnsupportedLookAndFeelException) {
        } catch (_: ClassNotFoundException) {
        } catch (_: InstantiationException) {
        } catch (_: IllegalAccessException) {
        }

        // These may be occluded by the GL canvas
        JPopupMenu.setDefaultLightWeightPopupEnabled(false)
        ToolTipManager.sharedInstance().isLightWeightPopupEnabled = false

        CacheChooserController(
            "Choose game cache version",
            Configuration()
        ).isVisible = true
    }
}
