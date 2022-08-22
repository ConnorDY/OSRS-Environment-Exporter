package utils

import java.util.Queue

object Utils {
    fun worldCoordinatesToRegionId(x: Int, y: Int): Int {
        return (x ushr 6) shl 8 or (y ushr 6)
    }

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
