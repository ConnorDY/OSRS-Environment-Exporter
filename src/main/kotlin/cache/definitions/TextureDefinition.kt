/* Derived from RuneLite source code, which is licensed as follows:
 *
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
