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

    private const val defaultCacheRevision = 209
    fun parseCacheRevision(cachePath: String): Int {
        // regex to match 2022-10-19-rev209
        val revisionRegexp = """\d{4}-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])-(rev)([0-9][0-9][0-9])""".toRegex()
        val revision = revisionRegexp.find(cachePath) ?: return defaultCacheRevision
        val revString: String = revision.groups[4]?.value ?: return defaultCacheRevision
        return revString.toInt()
    }
}
