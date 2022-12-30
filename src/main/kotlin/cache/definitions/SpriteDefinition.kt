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
 */package cache.definitions

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
