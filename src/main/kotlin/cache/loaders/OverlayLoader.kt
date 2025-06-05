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
package cache.loaders

import cache.ConfigType
import cache.IndexType
import cache.definitions.OverlayDefinition
import cache.utils.read24BitInt
import cache.utils.readString
import cache.utils.readUnsignedByte
import com.displee.cache.CacheLibrary
import java.nio.ByteBuffer

class OverlayLoader(cacheLibrary: CacheLibrary) {
    private val overlayDefinitionCache = HashMap<Int, OverlayDefinition>()

    fun get(id: Int): OverlayDefinition? {
        return overlayDefinitionCache[id]
    }

    fun load(id: Int, b: ByteArray?): OverlayDefinition {
        val def = OverlayDefinition(id)
        val inputStream = ByteBuffer.wrap(b)
        while (true) {
            val opcode: Int = inputStream.readUnsignedByte()
            if (opcode == 0) {
                break
            }
            if (opcode == 1) {
                val color: Int = inputStream.read24BitInt()
                def.rgbColor = color
            } else if (opcode == 2) {
                val texture: Int = inputStream.readUnsignedByte()
                def.texture = texture
            } else if (opcode == 5) {
                def.hideUnderlay = false
            } else if (opcode == 7) {
                val secondaryColor: Int = inputStream.read24BitInt()
                def.secondaryRgbColor = secondaryColor
            } else if (opcode == 8) {
                // Handle opcode 8 introduced in newer cache revisions
                // Read string to prevent BufferUnderflowException
                inputStream.readString()
            } else {
                // Handle unknown opcodes by skipping a single byte
                // This prevents BufferUnderflowException when encountering new opcodes
                inputStream.readUnsignedByte()
            }
        }
        def.calculateHsl()
        return def
    }

    init {
        val index = cacheLibrary.index(IndexType.CONFIGS.id)
        val archive = index.archive(ConfigType.OVERLAY.id)
        for (file in archive!!.fileIds()) {
            val data = cacheLibrary.data(IndexType.CONFIGS.id, ConfigType.OVERLAY.id, file)
            val def = load(file, data)
            overlayDefinitionCache[def.id] = def
        }
    }
}