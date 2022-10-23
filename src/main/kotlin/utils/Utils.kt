package utils

import controllers.worldRenderer.Constants.REGION_X_STRIDE
import java.util.Queue

object Utils {
    /** Convert a distance in blocks to a distance in regions. */
    fun blockAxisToRegionAxis(blockAxis: Int): Int = blockAxis ushr 6

    fun worldCoordinatesToRegionId(x: Int, y: Int) =
        blockAxisToRegionAxis(x) shl 8 or blockAxisToRegionAxis(y)

    fun getRegionIdX(regionId: Int): Int = regionId / REGION_X_STRIDE
    fun getRegionIdY(regionId: Int): Int = regionId % REGION_X_STRIDE
    fun regionCoordinatesToRegionId(x: Int, y: Int): Int = x * REGION_X_STRIDE + y

    fun isMacOS() =
        System.getProperty("os.name").lowercase().startsWith("mac")

    fun Queue<Runnable>.doAllActions() {
        var action: Runnable?
        do {
            action = poll()
            action?.run()
        } while (action != null)
    }
}
