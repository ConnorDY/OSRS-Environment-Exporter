package cache.loaders

import cache.IndexType
import cache.definitions.TextureDefinition
import cache.utils.readUnsignedByte
import cache.utils.readUnsignedShort
import com.displee.cache.CacheLibrary
import java.nio.ByteBuffer

class TextureLoader(
    cacheLibrary: CacheLibrary,
    private val textureDefinitionCache: HashMap<Int, TextureDefinition> = HashMap()
) {
    fun getAll(): Array<TextureDefinition> {
        return textureDefinitionCache.map { it.value }.toTypedArray()
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