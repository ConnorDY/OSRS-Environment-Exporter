package cache.loaders

import cache.XteaManager
import cache.definitions.Location
import cache.definitions.LocationsDefinition
import cache.utils.readUnsignedShortSmart
import com.displee.cache.CacheLibrary
import java.nio.ByteBuffer

class LocationsLoader(
    private val library: CacheLibrary,
    private val xtea: XteaManager,
    private val locationsDefinitionCache: HashMap<Int, LocationsDefinition?> = HashMap()
) {
    fun get(regionId: Int): LocationsDefinition? {
        if (locationsDefinitionCache.containsKey(regionId)) {
            return locationsDefinitionCache[regionId]
        }
        return loadLocations(regionId)
    }

    private fun loadLocations(regionId: Int): LocationsDefinition? {
        val locationsDefinition = LocationsDefinition(regionId)

        val x = (regionId shr 8) and 0xFF
        val y = regionId and 0xFF
        val landscape = library.data(5, "l${x}_$y", xtea.getKeys(regionId))
        if (landscape == null) {
            locationsDefinitionCache[regionId] = null
            return null
        }

        val buffer = ByteBuffer.wrap(landscape)

        var objId = -1
        var idOffset = buffer.readUnsignedShortSmart()
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

        locationsDefinitionCache[regionId] = locationsDefinition
        return locationsDefinition
    }
}
