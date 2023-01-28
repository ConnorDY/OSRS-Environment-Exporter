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

import cache.utils.HeightCalc

class RegionDefinition(
    val regionId: Int = 0,
    val tiles: Array<Array<Array<Tile>>>
) {
    val regionX: Int get() = (regionId shr 8) and 0xFF
    val regionY: Int get() = regionId and 0xFF

    val baseX: Int get() = regionId shr 8 and 0xFF shl 6 // local coords are in bottom 6 bits (64*64)
    val baseY: Int get() = (regionId and 0xFF) shl 6

    val tileSettings: Array<Array<IntArray>> = Array(Z) { Array(X) { IntArray(Y) } }
    val overlayIds: Array<Array<IntArray>> = Array(Z) { Array(X) { IntArray(Y) } }
    val overlayPaths: Array<Array<IntArray>> = Array(Z) { Array(X) { IntArray(Y) } }
    val overlayRotations: Array<Array<IntArray>> = Array(Z) { Array(X) { IntArray(Y) } }
    val underlayIds: Array<Array<IntArray>> = Array(Z) { Array(X) { IntArray(Y) } }

    fun calculateTerrain() {
        for (z in 0 until Z) {
            for (x in 0 until X) {
                for (y in 0 until Y) {
                    val tile: Tile = tiles[z][x][y]
                    val cacheHeight = tile.cacheHeight
                    if (cacheHeight == null) {
                        if (z == 0) {
                            tile.height = -HeightCalc.calculate(baseX + x + 0xe3b7b, baseY + y + 0x87cce) * 8
                        } else {
                            tile.height = tiles[z - 1][x][y].height - 240
                        }
                    } else {
                        var height: Int = cacheHeight
                        if (height == 1) {
                            height = 0
                        }

                        if (z == 0) {
                            tiles[z][x][y].height = -height * 8
                        } else {
                            tiles[z][x][y].height = tiles[z - 1][x][y].height - height * 8
                        }
                    }
                    overlayIds[z][x][y] = tile.overlayId.toInt()
                    overlayPaths[z][x][y] = tile.overlayPath.toInt()
                    overlayRotations[z][x][y] = tile.overlayRotation.toInt()
                    tileSettings[z][x][y] = tile.settings.toInt()
                    underlayIds[z][x][y] = tile.underlayId.toInt()
                }
            }
        }
    }

    data class Tile(
        var cacheHeight: Int? = null,
        var height: Int = 0,
        var attrOpcode: Int = 0,
        var settings: Byte = 0,
        var overlayId: Short = 0,
        var overlayPath: Byte = 0,
        var overlayRotation: Byte = 0,
        var underlayId: Short = 0
    )

    companion object {
        const val Z = 4
        const val X = 64
        const val Y = 64
    }
}
