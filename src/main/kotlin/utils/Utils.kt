package utils

class Utils {
    companion object {
        fun worldCoordinatesToRegionId(x: Int, y: Int): Int {
            return (x ushr 6) shl 8 or (y ushr 6)
        }
    }
}
