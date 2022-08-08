package cache.utils

import kotlin.math.sqrt
import kotlin.math.truncate

class Vec3F(val x: Float, val y: Float, val z: Float) {
    fun magnitude() = sqrt(x * x + y * y + z * z)
    fun magnitudeInt() = magnitude().toInt()
    fun dot(other: Vec3F): Float = x * other.x + y * other.y + z * other.z

    /** Normalise this vector, including inaccuracies introduced by the original code */
    fun normalizedAsInts(): Vec3F {
        val magnitude = magnitudeInt()
        return Vec3F(crunch(x / magnitude), crunch(y / magnitude), crunch(z / magnitude))
    }
    private fun crunch(x: Float): Float = truncate(256.0f * x) / 256.0f
}
