package cache.loaders

import cache.XteaManager
import cache.definitions.Location
import cache.definitions.LocationsDefinition
import cache.definitions.RegionDefinition
import com.displee.cache.CacheLibrary
import cache.utils.readUnsignedShortSmart
import cache.utils.writeUnsignedShortSmart
import java.nio.ByteBuffer

class LocationsLoader(
    private val library: CacheLibrary,
    private val xtea: XteaManager,
    private val locationsDefinitionCache: HashMap<Int, LocationsDefinition> = HashMap()
) {
    fun get(regionId: Int): LocationsDefinition? {
        if (locationsDefinitionCache[regionId] != null) {
            return locationsDefinitionCache[regionId]
        }
        return loadLocations(regionId)
    }

    private fun loadLocations(regionId: Int): LocationsDefinition? {
        val locationsDefinition = LocationsDefinition(regionId)

        val x = (regionId shr 8) and 0xFF
        val y = regionId and 0xFF
        val landscape = library.data(5, "l${x}_${y}", xtea.getKeys(regionId)) ?: return null

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

    fun writeLocations(outputLibrary: CacheLibrary, locationsDefinition: LocationsDefinition): Boolean {
        val x = (locationsDefinition.regionId shr 8) and 0xFF
        val y = locationsDefinition.regionId and 0xFF

        // group locations by objectId
        val locMap = HashMap<Int, ArrayList<Location>>()
        for (l in locationsDefinition.locations) {
            if (locMap[l.objId] == null) {
                locMap[l.objId] = ArrayList()
            }
            locMap[l.objId]!!.add(l)
        }

        val outputBuffer = ByteBuffer.allocate(16384)

        var lastId = -1
        for (group in locMap.toSortedMap()) {
            val groupOffset = (group.key - lastId)
            outputBuffer.writeUnsignedShortSmart(groupOffset)
            lastId = group.key

            // must sort by z then x or client crash!
            group.value.sortWith(compareBy ({it.z}, {it.x}))

            var lastPos = 0
            for (l in group.value) {
                val position = ((l.z shl 12) or (l.x shl 6) or l.y)
                val positionOffset = position + 1 - lastPos
                outputBuffer.writeUnsignedShortSmart(positionOffset)

                val attributes = (l.type shl 2) or l.orientation
                outputBuffer.put(attributes.toByte())

                lastPos = position
                if (positionOffset == 0) {
                    lastPos = 0
                }
            }
            outputBuffer.put(0)
        }
        outputBuffer.put(0)

        outputBuffer.flip()
        val out = ByteArray(outputBuffer.limit())
        outputBuffer.get(out)

        outputLibrary.put(5, "l${x}_${y}", out, xtea.getKeys(locationsDefinition.regionId))
        return outputLibrary.index(5).update()
    }
}