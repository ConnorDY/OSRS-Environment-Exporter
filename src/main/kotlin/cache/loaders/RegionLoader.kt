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
    private val cacheLibrary: CacheLibrary
) : ThreadsafeLazyLoader<RegionDefinition>() {
    private val logger = LoggerFactory.getLogger(RegionLoader::class.java)

    override fun load(id: Int): RegionDefinition? {
        val regionX = (id shr 8) and 0xFF
        val regionY = id and 0xFF
        val map = cacheLibrary.data(IndexType.MAPS.id, "m${regionX}_$regionY")
        if (map == null) {
            logger.warn("Could not load region (tile) data for $id")
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

        val regionDefinition = RegionDefinition(id, tiles)
        regionDefinition.calculateTerrain()
        return regionDefinition
    }

    fun findRegionForWorldCoordinates(x: Int, y: Int): RegionDefinition? {
        return get(Utils.worldCoordinatesToRegionId(x, y))
    }
}
