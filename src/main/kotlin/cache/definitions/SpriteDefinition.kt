package cache.definitions

class SpriteDefinition {
    var id = 0
    var frame = 0
    var offsetX = 0
    var offsetY = 0
    var width = 0
    var height = 0
    var maxWidth = 0
    var maxHeight = 0
    lateinit var pixels: IntArray

    @Transient
    lateinit var pixelIdx: ByteArray

    @Transient
    lateinit var palette: IntArray

    fun normalize() {
        if (width != maxWidth || height != maxHeight) {
            val var1 = ByteArray(maxWidth * maxHeight)
            var var2 = 0
            for (var3 in 0 until height) {
                for (var4 in 0 until width) {
                    var1[var4 + (var3 + offsetY) * maxWidth + offsetX] = pixelIdx[var2++]
                }
            }
            pixelIdx = var1
            width = maxWidth
            height = maxHeight
            offsetX = 0
            offsetY = 0
        }
    }
}
