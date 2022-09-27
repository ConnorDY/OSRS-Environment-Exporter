/* Derived from RuneLite source code, which is licensed as follows:
 *
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
