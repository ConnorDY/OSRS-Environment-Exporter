package cache.loaders

import cache.XteaManager
import cache.definitions.Location
import cache.definitions.LocationsDefinition
import cache.utils.readUnsignedShortSmart
import cache.utils.readUnsignedSmartShortExtended
import com.displee.cache.CacheLibrary
import org.slf4j.LoggerFactory
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer

class LocationsLoader(
    private val library: CacheLibrary,
    private val xtea: XteaManager,
    private val locationsDefinitionCache: HashMap<Int, LocationsDefinition?> = HashMap()
) {
    private val logger = LoggerFactory.getLogger(LocationsLoader::class.java)

    fun get(regionId: Int): LocationsDefinition? {
        if (locationsDefinitionCache.containsKey(regionId)) {
            return locationsDefinitionCache[regionId]
        }
        val location = try {
            loadLocations(regionId)
        } catch (e: BufferUnderflowException) {
            e.printStackTrace() // Alert an attentive user that an issue has occurred
            null
        }
        locationsDefinitionCache[regionId] = location
        return location
    }

    private fun loadLocations(regionId: Int): LocationsDefinition? {
        val locationsDefinition = LocationsDefinition(regionId)

        val x = (regionId shr 8) and 0xFF
        val y = regionId and 0xFF
        val xteaKeys = xtea.getKeys(regionId)
        if (xteaKeys == null) {
            logger.warn("Could not get xtea keys for region $regionId ($x, $y)")
            return null
        }

        val landscape = library.data(5, "l${x}_$y", xteaKeys) ?: return null

        val buffer = ByteBuffer.wrap(landscape)

        var objId = -1
        var idOffset = buffer.readUnsignedSmartShortExtended()
        while (idOffset != 0) {
            objId += idOffset

            var position = 0
            var positionOffset = buffer.readUnsignedShortSmart()
            while (positionOffset != 0) {
                position += positionOffset - 1

                val localY = position and 0x3F
                val localX = position shr 6 and 0x3F
                val height = position shr 12 and 0x3

                val attributes = buffer.get().toInt()
                val type = attributes shr 2
                val orientation = attributes and 0x3

                locationsDefinition.locations.add(
                    Location(
                        objId,
                        type,
                        orientation,
                        localX,
                        localY,
                        height
                    )
                )

                positionOffset = buffer.readUnsignedShortSmart()
            }

            idOffset = buffer.readUnsignedShortSmart()
        }

        return locationsDefinition
    }
}
