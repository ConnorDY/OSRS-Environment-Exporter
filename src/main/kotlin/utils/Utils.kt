package utils

object Utils {
    fun worldCoordinatesToRegionId(x: Int, y: Int): Int {
        return (x ushr 6) shl 8 or (y ushr 6)
    }
}
