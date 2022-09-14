package utils

import controllers.worldRenderer.Constants.REGION_X_STRIDE
import java.util.Queue

object Utils {
    fun worldCoordinatesToRegionId(x: Int, y: Int): Int {
        return (x ushr 6) shl 8 or (y ushr 6)
    }

    fun getRegionIdX(regionId: Int): Int = regionId / REGION_X_STRIDE
    fun getRegionIdY(regionId: Int): Int = regionId % REGION_X_STRIDE
    fun regionCoordinatesToRegionId(x: Int, y: Int): Int = x * REGION_X_STRIDE + y

    fun isMacOS() =
        System.getProperty("os.name").lowercase().startsWith("mac")

    fun isWindows() =
        System.getProperty("os.name").lowercase().startsWith("windows")

    fun Queue<Runnable>.doAllActions() {
        var action: Runnable?
        do {
            action = poll()
            action?.run()
        } while (action != null)
    }
}
