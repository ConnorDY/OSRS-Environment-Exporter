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

import cache.IndexType
import cache.definitions.TextureDefinition
import cache.utils.readUnsignedByte
import cache.utils.readUnsignedShort
import com.displee.cache.CacheLibrary
import java.nio.ByteBuffer

class TextureLoader(cacheLibrary: CacheLibrary) {
    private val textureDefinitionCache = HashMap<Int, TextureDefinition>()

    fun getAll(): Array<TextureDefinition?> {
        val maxId = textureDefinitionCache.maxBy { it.key }!!.key
        // Jagex skipped texture id 54 so now we have to do this?
        val texArray = arrayOfNulls<TextureDefinition>(maxId + 1)
        for (tex in textureDefinitionCache) {
            texArray[tex.key] = tex.value
        }
        return texArray
    }

    fun get(id: Int): TextureDefinition? {
        return textureDefinitionCache[id]
    }

    private fun load(id: Int, b: ByteArray?): TextureDefinition {
        val def = TextureDefinition()
        val inputStream = ByteBuffer.wrap(b)
        def.field1777 = inputStream.readUnsignedShort()
        def.field1778 = inputStream.get().toInt() != 0
        def.id = id
        val count: Int = inputStream.readUnsignedByte()
        val files = IntArray(count)
        for (i in 0 until count) files[i] = inputStream.readUnsignedShort()
        def.fileIds = files
        if (count > 1) {
            def.field1780 = IntArray(count - 1)
            for (var3 in 0 until count - 1) {
                def.field1780[var3] = inputStream.readUnsignedByte()
            }
        }
        if (count > 1) {
            def.field1781 = IntArray(count - 1)
            for (var3 in 0 until count - 1) {
                def.field1781[var3] = inputStream.readUnsignedByte()
            }
        }
        def.field1786 = IntArray(count)
        for (var3 in 0 until count) {
            def.field1786[var3] = inputStream.int
        }
        def.field1783 = inputStream.readUnsignedByte()
        def.field1782 = inputStream.readUnsignedByte()
        return def
    }

    fun getAverageTextureRGB(id: Int): Int {
        val texture = get(id)
        return texture?.field1777 ?: 0
    }

    init {
        val index = cacheLibrary.index(IndexType.TEXTURES.id)
        val archive = index.archive(0)
        for (file in archive!!.fileIds()) {
            val data = cacheLibrary.data(IndexType.TEXTURES.id, 0, file)
            val def = load(file, data)
            textureDefinitionCache[def.id] = def
        }
        cacheLibrary.index(IndexType.TEXTURES.id).unCache() // free memory
    }
}
