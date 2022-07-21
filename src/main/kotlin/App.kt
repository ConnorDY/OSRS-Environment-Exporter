import controllers.CacheChooserController
import models.Configuration
import org.pushingpixels.radiance.theming.api.skin.RadianceNightShadeLookAndFeel
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities
import javax.swing.ToolTipManager
import javax.swing.UIManager
import javax.swing.UnsupportedLookAndFeelException

fun main() {
    SwingUtilities.invokeLater {
        try {
            UIManager.setLookAndFeel(RadianceNightShadeLookAndFeel())
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
