package cache.loaders

import cache.IndexType
import cache.definitions.RegionDefinition
import cache.definitions.RegionDefinition.Companion.X
import cache.definitions.RegionDefinition.Companion.Y
import cache.definitions.RegionDefinition.Companion.Z
import cache.utils.readUnsignedByte
import com.displee.cache.CacheLibrary
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class RegionLoader(
    private val cacheLibrary: CacheLibrary,
    private val regionDefinitionCache: HashMap<Int, RegionDefinition?> = HashMap()
) {
    fun get(regionId: Int): RegionDefinition? {
        if (regionDefinitionCache.containsKey(regionId)) {
            return regionDefinitionCache[regionId]
        }

        return loadRegion(regionId)
    }

    private fun loadRegion(regionId: Int): RegionDefinition? {
        val regionX = (regionId shr 8) and 0xFF
        val regionY = regionId and 0xFF
        val map = cacheLibrary.data(IndexType.MAPS.id, "m${regionX}_${regionY}")
        if (map == null) {
            regionDefinitionCache[regionId] = null  // Negative cache entry
            return null
        }

        val regionDefinition = RegionDefinition(regionId)
        val inputStream = ByteBuffer.wrap(map)

        for (z in 0 until Z) {
            for (x in 0 until X) {
                for (y in 0 until Y) {
                    val tile = RegionDefinition.Tile()
                    regionDefinition.tiles[z][x][y] = tile
                    while (true) {
                        val attribute: Int = inputStream.readUnsignedByte()
                        if (attribute == 0) {
                            break
                        } else if (attribute == 1) {
                            val height: Int = inputStream.readUnsignedByte()
                            tile.cacheHeight = height
                            tile.height = height
                            break
                        } else if (attribute <= 49) {
                            tile.attrOpcode = attribute
                            tile.overlayId = inputStream.get()
                            tile.overlayPath = ((attribute - 2) / 4).toByte()
                            tile.overlayRotation = (attribute - 2 and 3).toByte()
                        } else if (attribute <= 81) {
                            tile.settings = (attribute - 49).toByte()
                        } else {
                            tile.underlayId = (attribute - 81).toByte()
                        }
                    }
                }
            }
        }

        regionDefinition.calculateTerrain()
        regionDefinitionCache[regionId] = regionDefinition
        return regionDefinition
    }

    fun findRegionForWorldCoordinates(x: Int, y: Int): RegionDefinition? {
        return get((x ushr 6) shl 8 or (y ushr 6))
    }

}