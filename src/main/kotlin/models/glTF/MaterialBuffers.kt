package models.glTF

import cache.utils.ColorPalette.Companion.rs2hsbToColor

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
            val color = rs2hsbToColor(rs2color)

            colors.add(color.red.toFloat() / 255f)
            colors.add(color.green.toFloat() / 255f)
            colors.add(color.blue.toFloat() / 255f)
        }

        if (texcoords != null) {
            texcoords.add(texcoordU)
            texcoords.add(texcoordV)
        }
    }
}
