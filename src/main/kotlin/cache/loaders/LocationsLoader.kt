package cache.loaders

import cache.XteaManager
import cache.definitions.Location
import cache.definitions.LocationsDefinition
import cache.utils.readUnsignedShortSmart
import cache.utils.readUnsignedSmartShortExtended
import com.displee.cache.CacheLibrary
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer

class LocationsLoader(
    private val library: CacheLibrary,
    private val xtea: XteaManager,
) : ThreadsafeLazyLoader<LocationsDefinition>() {
    private val logger = LoggerFactory.getLogger(LocationsLoader::class.java)

    override fun load(id: Int): LocationsDefinition? {
        val locationsDefinition = LocationsDefinition(id)

        val x = (id shr 8) and 0xFF
        val y = id and 0xFF
        val xteaKeys = xtea.getKeys(id)
        if (xteaKeys == null) {
            logger.warn("Could not get xtea keys for region $id ($x, $y)")
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
