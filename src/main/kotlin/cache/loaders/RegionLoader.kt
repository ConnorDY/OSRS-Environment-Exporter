package cache.loaders

import cache.IndexType
import cache.definitions.RegionDefinition
import cache.definitions.RegionDefinition.Companion.X
import cache.definitions.RegionDefinition.Companion.Y
import cache.definitions.RegionDefinition.Companion.Z
import cache.utils.readUnsignedByte
import com.displee.cache.CacheLibrary
import org.slf4j.LoggerFactory
import utils.Utils
import java.nio.ByteBuffer

class RegionLoader(
    private val cacheLibrary: CacheLibrary,
    private val regionDefinitionCache: HashMap<Int, RegionDefinition?> = HashMap()
) {
    private val logger = LoggerFactory.getLogger(RegionLoader::class.java)

    fun get(regionId: Int): RegionDefinition? {
        val cached = regionDefinitionCache[regionId]
        if (cached != null || regionDefinitionCache.containsKey(regionId)) {
            return cached
        }

        return loadRegion(regionId)
    }

    private fun loadRegion(regionId: Int): RegionDefinition? {
        val regionX = (regionId shr 8) and 0xFF
        val regionY = regionId and 0xFF
        val map = cacheLibrary.data(IndexType.MAPS.id, "m${regionX}_$regionY")
        if (map == null) {
            logger.warn("Could not load region (tile) data for $regionId")
            regionDefinitionCache[regionId] = null // Negative cache entry
            return null
        }

        val inputStream = ByteBuffer.wrap(map)

        val tiles = Array(Z) {
            Array(X) {
                Array(Y) {
                    val tile = RegionDefinition.Tile()
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
                    tile
                }
            }
        }

        val regionDefinition = RegionDefinition(regionId, tiles)
        regionDefinition.calculateTerrain()
        regionDefinitionCache[regionId] = regionDefinition
        return regionDefinition
    }

    fun findRegionForWorldCoordinates(x: Int, y: Int): RegionDefinition? {
        return get(Utils.worldCoordinatesToRegionId(x, y))
    }
}
