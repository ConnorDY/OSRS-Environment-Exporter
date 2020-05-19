package cache.loaders

import cache.ConfigType
import cache.IndexType
import cache.definitions.OverlayDefinition
import cache.utils.read24BitInt
import cache.utils.readUnsignedByte
import com.displee.cache.CacheLibrary
import com.google.inject.Inject
import java.nio.ByteBuffer

class OverlayLoader @Inject constructor(
    cacheLibrary: CacheLibrary,
    private val overlayDefinitionCache: HashMap<Int, OverlayDefinition> = HashMap()
) {
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