package cache.definitions

import cache.loaders.SpriteLoader
import cache.utils.ColorPalette

class TextureDefinition {
    var field1777 = 0
    var field1778 = false
    var id = 0
    lateinit var fileIds: IntArray
    lateinit var field1780: IntArray
    lateinit var field1781: IntArray
    lateinit var field1786: IntArray
    var field1782 = 0
    var field1783 = 0

    @Transient
    lateinit var pixels: IntArray
    fun loadPixels(var1: Double, var3: Int, spriteLoader: SpriteLoader): Boolean {
        val var5 = var3 * var3
        pixels = IntArray(var5)
        for (var6 in fileIds.indices) {
            val var7: SpriteDefinition = spriteLoader.get(fileIds[var6]) ?: return false
            var7.normalize()
            val var8: ByteArray = var7.pixelIdx
            val var9: IntArray = var7.palette.clone()
            val var10 = field1786[var6]
            var var11: Int
            var var12: Int
            var var13: Int
            var var14: Int
            if (var10 and -16777216 == 50331648) {
                var11 = var10 and 16711935
                var12 = var10 shr 8 and 255
                var13 = 0
                while (var13 < var9.size) {
                    var14 = var9[var13]
                    if (var14 shr 8 == var14 and 65535) {
                        var14 = var14 and 255
                        var9[var13] = var11 * var14 shr 8 and 16711935 or var12 * var14 and 65280
                    }
                    ++var13
                }
            }
            var11 = 0
            while (var11 < var9.size) {
                var9[var11] = ColorPalette.adjustForBrightness(var9[var11], var1)
                ++var11
            }
            var11 = if (var6 == 0) {
                0
            } else {
                field1780[var6 - 1]
            }
            if (var11 == 0) {
                if (var3 == var7.maxWidth) {
                    var12 = 0
                    while (var12 < var5) {
                        pixels[var12] = var9[var8[var12].toInt() and 255]
                        ++var12
                    }
                } else if (var7.maxWidth == 64 && var3 == 128) {
                    var12 = 0
                    var13 = 0
                    while (var13 < var3) {
                        var14 = 0
                        while (var14 < var3) {
                            pixels[var12++] = var9[var8[(var13 shr 1 shl 6) + (var14 shr 1)].toInt() and 255]
                            ++var14
                        }
                        ++var13
                    }
                } else {
                    if (var7.maxWidth != 128 || var3 != 64) {
                        throw RuntimeException()
                    }
                    var12 = 0
                    var13 = 0
                    while (var13 < var3) {
                        var14 = 0
                        while (var14 < var3) {
                            pixels[var12++] = var9[var8[(var14 shl 1) + (var13 shl 1 shl 7)].toInt() and 255]
                            ++var14
                        }
                        ++var13
                    }
                }
            }
        }
        return true
    }
}
