package models.math

import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector4f

class Ray private constructor(
    val origin: Vector3fc,
    val direction: Vector3fc,
) {
    companion object {
        fun fromTo(from: Vector3fc, to: Vector3fc): Ray {
            val direction = Vector3f(to).sub(from).normalize()
            return Ray(Vector3f(from), direction)
        }

        fun fromScreenCoordinates(x: Int, y: Int, width: Int, height: Int, invViewProjectionMatrix: Matrix4f): Ray {
            val rayPos1 = Vector4f(
                (2.0f * x) / width - 1.0f,
                1.0f - (2.0f * y) / height,
                -1.0f,
                1.0f,
            )
            val rayPos2 = Vector4f(rayPos1).add(0.0f, 0.0f, 1.0f, 0.0f)
            invViewProjectionMatrix.transform(rayPos1)
            invViewProjectionMatrix.transform(rayPos2)
            rayPos1.div(rayPos1.w)
            rayPos2.div(rayPos2.w)
            return fromTo(
                Vector3f(rayPos1.x(), rayPos1.y(), rayPos1.z()),
                Vector3f(rayPos2.x(), rayPos2.y(), rayPos2.z()),
            )
        }
    }
}
