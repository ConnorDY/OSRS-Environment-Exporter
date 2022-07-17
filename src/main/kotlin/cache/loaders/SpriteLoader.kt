package cache.loaders

import cache.IndexType
import cache.definitions.SpriteDefinition
import cache.utils.read24BitInt
import cache.utils.readUnsignedByte
import cache.utils.readUnsignedShort
import com.displee.cache.CacheLibrary
import java.nio.ByteBuffer

class SpriteLoader(
    cacheLibrary: CacheLibrary,
    private val spriteDefinitionCache: HashMap<Int, SpriteDefinition> = HashMap()
) {
    init {
        for (archive in cacheLibrary.index(IndexType.SPRITES.id).archiveIds()) {
            val data = cacheLibrary.data(IndexType.SPRITES.id, archive) ?: continue
            val defs = load(archive, data)
            for (def in defs) {
                spriteDefinitionCache[def!!.id] = def
            }
        }
        cacheLibrary.index(IndexType.SPRITES.id).unCache() // free memory
    }

    fun get(id: Int): SpriteDefinition? {
        return spriteDefinitionCache[id]
    }

    private fun load(id: Int, b: ByteArray): Array<SpriteDefinition?> {
        val inputStream = ByteBuffer.wrap(b)
        inputStream.position(inputStream.limit() - 2)
        val spriteCount: Int = inputStream.readUnsignedShort()
        val sprites: Array<SpriteDefinition?> = arrayOfNulls(spriteCount)

        // 2 for size
        // 5 for width, height, palette length
        // + 8 bytes per sprite for offset x/y, width, and height
        inputStream.position(inputStream.limit() - 7 - spriteCount * 8)

        // max width and height
        val width: Int = inputStream.readUnsignedShort()
        val height: Int = inputStream.readUnsignedShort()
        val paletteLength: Int = inputStream.readUnsignedByte() + 1
        for (i in 0 until spriteCount) {
            sprites[i] = SpriteDefinition()
            sprites[i]!!.id = id
            sprites[i]!!.frame = i
            sprites[i]!!.maxWidth = width
            sprites[i]!!.maxHeight = height
        }
        for (i in 0 until spriteCount) {
            sprites[i]!!.offsetX = inputStream.readUnsignedShort()
        }
        for (i in 0 until spriteCount) {
            sprites[i]!!.offsetY = inputStream.readUnsignedShort()
        }
        for (i in 0 until spriteCount) {
            sprites[i]!!.width = inputStream.readUnsignedShort()
        }
        for (i in 0 until spriteCount) {
            sprites[i]!!.height = inputStream.readUnsignedShort()
        }

        // same as above + 3 bytes for each palette entry, except for the first one (which is transparent)
        inputStream.position(inputStream.limit() - 7 - spriteCount * 8 - (paletteLength - 1) * 3)
        val palette = IntArray(paletteLength)
        for (i in 1 until paletteLength) {
            palette[i] = inputStream.read24BitInt()
            if (palette[i] == 0) {
                palette[i] = 1
            }
        }
        inputStream.position(0)
        for (i in 0 until spriteCount) {
            val def: SpriteDefinition = sprites[i]!!
            val spriteWidth: Int = def.width
            val spriteHeight: Int = def.height
            val dimension = spriteWidth * spriteHeight
            val pixelPaletteIndicies = ByteArray(dimension)
            val pixelAlphas = ByteArray(dimension)
            def.pixelIdx = pixelPaletteIndicies
            def.palette = palette
            val flags: Int = inputStream.readUnsignedByte()
            if (flags and FLAG_VERTICAL == 0) {
                // read horizontally
                for (j in 0 until dimension) {
                    pixelPaletteIndicies[j] = inputStream.get()
                }
            } else {
                // read vertically
                for (j in 0 until spriteWidth) {
                    for (k in 0 until spriteHeight) {
                        pixelPaletteIndicies[spriteWidth * k + j] = inputStream.get()
                    }
                }
            }

            // read alphas
            if (flags and FLAG_ALPHA != 0) {
                if (flags and FLAG_VERTICAL == 0) {
                    // read horizontally
                    for (j in 0 until dimension) {
                        pixelAlphas[j] = inputStream.get()
                    }
                } else {
                    // read vertically
                    for (j in 0 until spriteWidth) {
                        for (k in 0 until spriteHeight) {
                            pixelAlphas[spriteWidth * k + j] = inputStream.get()
                        }
                    }
                }
            } else {
                // everything non-zero is opaque
                for (j in 0 until dimension) {
                    val index = pixelPaletteIndicies[j].toInt()
                    if (index != 0) pixelAlphas[j] = 0xFF.toByte()
                }
            }
            val pixels = IntArray(dimension)

            // build argb pixels from palette/alphas
            for (j in 0 until dimension) {
                val index: Int = pixelPaletteIndicies[j].toInt() and 0xFF
                pixels[j] = palette[index] or (pixelAlphas[j].toInt() shl 24)
            }
            def.pixels = pixels
        }
        return sprites
    }

    companion object {
        const val FLAG_VERTICAL = 1
        const val FLAG_ALPHA = 2
    }
}
