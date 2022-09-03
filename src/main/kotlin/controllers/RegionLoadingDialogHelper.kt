package controllers

import models.scene.Scene
import java.awt.Component
import javax.swing.JOptionPane
import kotlin.math.roundToInt

object RegionLoadingDialogHelper {
    fun confirmAndLoadRadius(parent: Component, scene: Scene, regionId: Int, radius: Int): Boolean {
        if (!confirmRegionLoad(parent, radius * radius)) {
            return false
        }
        scene.loadRadius(regionId, radius)
        return true
    }

    fun confirmRegionLoad(parent: Component, regions: Int): Boolean {
        if (regions < 150) return true
        val message = "Are you sure you want to load $regions regions?\n" +
            "This will use a lot of memory. For populated regions, expect 25MB of VRAM per region.\n" +
            "This gives a ballpark estimate of ${(regions * 25 / 1024.0).roundToInt()}GB peak memory usage / VRAM usage.\n" +
            "(Varies wildly)"
        return JOptionPane.showConfirmDialog(parent, message, "Load large region", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION
    }
}
