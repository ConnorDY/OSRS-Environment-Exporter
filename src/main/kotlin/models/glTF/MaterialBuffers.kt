package models.glTF

import cache.utils.ColorPalette
import java.awt.Color

class MaterialBuffers(isTextured: Boolean) {
    val positions = ArrayList<Float>()
    val texcoords: ArrayList<Float>?
    val colors: ArrayList<Float>?

    init {
        if (isTextured) {
            texcoords = ArrayList()
            colors = null
        } else {
            colors = ArrayList()
            texcoords = null
        }
    }

    fun addVertex(
        positionX: Float,
        positionY: Float,
        positionZ: Float,
        texcoordU: Float,
        texcoordV: Float,
        rs2color: Int
    ) {
        positions.add(positionX)
        positions.add(-positionY)
        positions.add(-positionZ)

        if (colors != null) {
            val color = Color(pal[rs2color and 0xFFFF])

            colors.add(color.red.toFloat() / 255f)
            colors.add(color.green.toFloat() / 255f)
            colors.add(color.blue.toFloat() / 255f)
        }

        if (texcoords != null) {
            texcoords.add(texcoordU)
            texcoords.add(texcoordV)
        }
    }

    companion object {
        val pal = ColorPalette(1.0, 0, 512).colorPalette
    }
}
