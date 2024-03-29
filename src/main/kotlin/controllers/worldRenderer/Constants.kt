package controllers.worldRenderer

import kotlin.math.cos
import kotlin.math.sin

object Constants {
    const val MAX_DISTANCE = 10000

    const val LOCAL_COORD_BITS = 7
    const val LOCAL_TILE_SIZE = 1 shl LOCAL_COORD_BITS // 128 - size of a tile in local coordinates
    const val LOCAL_HALF_TILE_SIZE = LOCAL_TILE_SIZE / 2

    const val REGION_X_STRIDE = 256 // region ID difference between consecutive regions horizontally

    /** The theoretical maximum length of the sides of the region grid, in cells. */
    const val MAP_LENGTH = 256

    const val UNIT = Math.PI / 1024.0 // How much of the circle each unit of SINE/COSINE is
    val SINE =
        IntArray(2048) { i -> (65536.0 * sin(i * UNIT)).toInt() } // sine angles for each of the 2048 units, * 65536 and stored as an int
    val COSINE = IntArray(2048) { i -> (65536.0 * cos(i * UNIT)).toInt() } // cosine
}
