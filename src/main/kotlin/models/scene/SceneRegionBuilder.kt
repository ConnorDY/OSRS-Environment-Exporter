package models.scene

import cache.LocationType
import cache.definitions.LocationsDefinition
import cache.definitions.ModelDefinition
import cache.definitions.ObjectDefinition
import cache.definitions.OverlayDefinition
import cache.definitions.RegionDefinition
import cache.definitions.UnderlayDefinition
import cache.definitions.converters.ObjectToModelConverter
import cache.loaders.LocationsLoader
import cache.loaders.ObjectLoader
import cache.loaders.OverlayLoader
import cache.loaders.RegionLoader
import cache.loaders.UnderlayLoader
import cache.loaders.getTileHeight
import cache.loaders.getTileSettings
import cache.utils.Vec3F
import controllers.worldRenderer.entities.Entity
import controllers.worldRenderer.entities.Model
import controllers.worldRenderer.entities.StaticObject
import org.slf4j.LoggerFactory
import utils.clamp

class SceneRegionBuilder constructor(
    private val regionLoader: RegionLoader,
    private val locationsLoader: LocationsLoader,
    private val objectLoader: ObjectLoader,
    private val underlayLoader: UnderlayLoader,
    private val overlayLoader: OverlayLoader,
    private val objectToModelConverter: ObjectToModelConverter
) {
    private val logger = LoggerFactory.getLogger(SceneRegionBuilder::class.java)

    private fun calcTileBrightness(sceneRegion: SceneRegion, z: Int, x: Int, y: Int, baseX: Int, baseY: Int) {
        val contrast = 768
        val ambient = 96
        val bias = Vec3F(-50.0f, -50.0f, -10.0f)
        val precision = 256

        val worldX = baseX + x
        val worldY = baseY + y
        val xHeightDiff = regionLoader.getTileHeight(z, worldX + 1, worldY) - regionLoader.getTileHeight(z, worldX - 1, worldY)
        val yHeightDiff = regionLoader.getTileHeight(z, worldX, worldY + 1) - regionLoader.getTileHeight(z, worldX, worldY - 1)

        val slopeVec = Vec3F(xHeightDiff.toFloat(), yHeightDiff.toFloat(), 256.0f)
        val dotProductMagnitude = bias.magnitudeInt() * contrast / precision
        val slopeBrightnessAdjust = (precision * bias.dot(slopeVec.normalizedAsInts())).toInt() / dotProductMagnitude + ambient
        val brightness = (regionLoader.getTileSettings(z, worldX - 1, worldY) shr 2) +
            (regionLoader.getTileSettings(z, worldX, worldY - 1) shr 2) +
            (regionLoader.getTileSettings(z, worldX + 1, worldY) shr 3) +
            (regionLoader.getTileSettings(z, worldX, worldY + 1) shr 3) +
            (regionLoader.getTileSettings(z, worldX, worldY) shr 1)
        sceneRegion.tileBrightness[x][y] = slopeBrightnessAdjust - brightness
    }

    // Loads a single region(rs size 64), not a scene(rs size 104)!
    // worldCoords to regionId
    // int regionId = (x >>> 6 << 8) | y >>> 6;
    fun loadRegion(regionId: Int): SceneRegion? {
        val region: RegionDefinition = regionLoader.get(regionId) ?: return null
        val locations: LocationsDefinition = locationsLoader.get(regionId) ?: fakeLocationsDefinition(regionId)
        val sceneRegion = SceneRegion(locations)
        val baseX: Int = region.baseX
        val baseY: Int = region.baseY
        val blend = 5
        val len: Int = REGION_SIZE + blend * 2
        val hues = IntArray(len)
        val sats = IntArray(len)
        val light = IntArray(len)
        val mul = IntArray(len)
        val num = IntArray(len)

        sceneRegionCache[regionId] = sceneRegion

        for (z in 0 until RegionDefinition.Z) {
            for (x in 0 until REGION_SIZE + 1) {
                for (y in 0 until REGION_SIZE + 1) {
                    calcTileBrightness(sceneRegion, z, x, y, baseX, baseY)
                }
            }
            for (xi in -blend * 2 until REGION_SIZE + blend * 2) {
                for (yi in -blend until REGION_SIZE + blend) {
                    val xr = xi + blend
                    if (xr >= -blend && xr < REGION_SIZE + blend) {
                        val r: RegionDefinition? = regionLoader.findRegionForWorldCoordinates(baseX + xr, baseY + yi)
                        if (r != null) {
                            val underlayId: Int = r.tiles[z][xr and 0x3F][yi and 0x3F].underlayId.toInt() and 0xff
                            if (underlayId > 0) {
                                val underlay: UnderlayDefinition = underlayLoader.get(underlayId - 1) ?: continue
                                hues[yi + blend] += underlay.hue
                                sats[yi + blend] += underlay.saturation
                                light[yi + blend] += underlay.lightness
                                mul[yi + blend] += underlay.hueMultiplier
                                num[yi + blend]++
                            }
                        }
                    }
                    val xl = xi - blend
                    if (xl >= -blend && xl < REGION_SIZE + blend) {
                        val r: RegionDefinition? = regionLoader.findRegionForWorldCoordinates(baseX + xl, baseY + yi)
                        if (r != null) {
                            val underlayId: Int = r.tiles[z][xl and 0x3F][yi and 0x3F].underlayId.toInt() and 0xff
                            if (underlayId > 0) {
                                val underlay: UnderlayDefinition = underlayLoader.get(underlayId - 1) ?: continue
                                hues[yi + blend] -= underlay.hue
                                sats[yi + blend] -= underlay.saturation
                                light[yi + blend] -= underlay.lightness
                                mul[yi + blend] -= underlay.hueMultiplier
                                num[yi + blend]--
                            }
                        }
                    }
                }
                if (xi in 0 until REGION_SIZE) {
                    var runningHues = 0
                    var runningSat = 0
                    var runningLight = 0
                    var runningMultiplier = 0
                    var runningNumber = 0
                    for (yi in -blend * 2 until REGION_SIZE + blend * 2) {
                        val yu = yi + blend
                        if (yu >= -blend && yu < REGION_SIZE + blend) {
                            runningHues += hues[yu + blend]
                            runningSat += sats[yu + blend]
                            runningLight += light[yu + blend]
                            runningMultiplier += mul[yu + blend]
                            runningNumber += num[yu + blend]
                        }
                        val yd = yi - blend
                        if (yd >= -blend && yd < REGION_SIZE + blend) {
                            runningHues -= hues[yd + blend]
                            runningSat -= sats[yd + blend]
                            runningLight -= light[yd + blend]
                            runningMultiplier -= mul[yd + blend]
                            runningNumber -= num[yd + blend]
                        }
                        if (yi in 0 until REGION_SIZE) {
                            val r: RegionDefinition =
                                regionLoader.findRegionForWorldCoordinates(baseX + xi, baseY + yi) ?: continue
                            val underlayId: Int = r.tiles[z][xi][yi].underlayId.toInt() and 0xFF
                            val overlayId: Int = r.tiles[z][xi][yi].overlayId.toInt() and 0xFF
                            if (underlayId <= 0 && overlayId <= 0) {
                                continue
                            }
                            val swHeight = regionLoader.getTileHeight(z, baseX + xi, baseY + yi)
                            val seHeight = regionLoader.getTileHeight(z, baseX + xi + 1, baseY + yi)
                            val neHeight = regionLoader.getTileHeight(z, baseX + xi + 1, baseY + yi + 1)
                            val nwHeight = regionLoader.getTileHeight(z, baseX + xi, baseY + yi + 1)
                            val swBrightness = sceneRegion.tileBrightness[xi][yi]
                            val seBrightness = sceneRegion.tileBrightness[xi + 1][yi]
                            val neBrightness = sceneRegion.tileBrightness[xi + 1][yi + 1]
                            val nwBrightness = sceneRegion.tileBrightness[xi][yi + 1]

                            if (runningMultiplier == 0) runningMultiplier = 1
                            if (runningNumber == 0) runningNumber = 1

                            val hsl: Int =
                                if (underlayId <= 0 || runningMultiplier <= 0 || runningNumber <= 0)
                                    -1
                                else {
                                    val avgHue = runningHues * 256 / runningMultiplier
                                    val avgSat = runningSat / runningNumber
                                    val avgLight = runningLight / runningNumber
                                    packHsl(avgHue, avgSat, avgLight)
                                }

                            if (overlayId == 0) {
                                val underlay: UnderlayDefinition? = underlayLoader.get(underlayId - 1)
                                sceneRegion.addTile(
                                    z,
                                    xi,
                                    yi,
                                    0,
                                    0,
                                    -1,
                                    swHeight,
                                    seHeight,
                                    neHeight,
                                    nwHeight,
                                    adjustUnderlayBrightness(hsl, swBrightness),
                                    adjustUnderlayBrightness(hsl, seBrightness),
                                    adjustUnderlayBrightness(hsl, neBrightness),
                                    adjustUnderlayBrightness(hsl, nwBrightness),
                                    0,
                                    0,
                                    0,
                                    0,
                                    underlay,
                                    null
                                )
                            } else {
                                val overlayPath: Int = r.tiles[z][xi][yi].overlayPath.toInt() + 1
                                val overlayRotation: Int = r.tiles[z][xi][yi].overlayRotation.toInt()
                                val overlayDefinition: OverlayDefinition? = overlayLoader.get(overlayId - 1)
                                var overlayTexture: Int = overlayDefinition?.texture!!
                                val overlayHsl: Int
                                when {
                                    overlayTexture >= 0 -> {
                                        overlayHsl = -1
                                    }
                                    overlayDefinition.rgbColor == 0xFF00FF -> {
                                        overlayHsl = -2
                                        overlayTexture = -1
                                    }
                                    else -> {
                                        overlayHsl = packHsl(
                                            overlayDefinition.hue,
                                            overlayDefinition.saturation,
                                            overlayDefinition.lightness
                                        )
                                    }
                                }
                                val underlay: UnderlayDefinition? = underlayLoader.get(underlayId - 1)
                                sceneRegion.addTile(
                                    z,
                                    xi,
                                    yi,
                                    overlayPath,
                                    overlayRotation,
                                    overlayTexture,
                                    swHeight,
                                    seHeight,
                                    neHeight,
                                    nwHeight,
                                    adjustUnderlayBrightness(hsl, swBrightness),
                                    adjustUnderlayBrightness(hsl, seBrightness),
                                    adjustUnderlayBrightness(hsl, neBrightness),
                                    adjustUnderlayBrightness(hsl, nwBrightness),
                                    adjustOverlayBrightness(overlayHsl, swBrightness),
                                    adjustOverlayBrightness(overlayHsl, seBrightness),
                                    adjustOverlayBrightness(overlayHsl, neBrightness),
                                    adjustOverlayBrightness(overlayHsl, nwBrightness),
                                    underlay,
                                    overlayDefinition
                                )
                            }
                        }
                    }
                }
            }
        }

        sceneRegion.locationsDefinition.locations.forEach { loc ->
            val z: Int = loc.z
            val x: Int = loc.x
            val y: Int = loc.y

            val objectDefinition: ObjectDefinition = objectLoader.get(loc.objId) ?: return@forEach

            val width: Int
            val length: Int
            if (loc.orientation != 1 && loc.orientation != 3) {
                width = objectDefinition.sizeX
                length = objectDefinition.sizeY
            } else {
                width = objectDefinition.sizeY
                length = objectDefinition.sizeX
            }
            val var11: Int
            val var12: Int
            if (width + x <= REGION_SIZE) {
                var11 = (width shr 1) + x
                var12 = (width + 1 shr 1) + x
            } else {
                var11 = x
                var12 = x + 1
            }
            val var13: Int
            val var14: Int
            if (length + y <= REGION_SIZE) {
                var13 = (length shr 1) + y
                var14 = y + (length + 1 shr 1)
            } else {
                var13 = y
                var14 = y + 1
            }
            val xSize = (x shl 7) + (width shl 6)
            val ySize = (y shl 7) + (length shl 6)
            val swHeight = regionLoader.getTileHeight(z, baseX + var12, baseY + var14)
            val seHeight = regionLoader.getTileHeight(z, baseX + var11, baseY + var14)
            val neHeight = regionLoader.getTileHeight(z, baseX + var12, baseY + var13)
            val nwHeight = regionLoader.getTileHeight(z, baseX + var11, baseY + var13)
            val height = swHeight + seHeight + neHeight + nwHeight shr 2

            val firstEntityOrientation = when (loc.type) {
                LocationType.WALL_CORNER.id,
                LocationType.DIAGONAL_OUTSIDE_WALL_DECORATION.id,
                LocationType.DIAGONAL_WALL_DECORATION.id,
                -> loc.orientation + 4

                LocationType.DIAGONAL_INSIDE_WALL_DECORATION.id,
                -> ((loc.orientation + 2) and 3) + 4

                else -> loc.orientation
            }

            val staticObject =
                getEntity(objectDefinition, loc.type, firstEntityOrientation, xSize, height, ySize, z, baseX, baseY)
                    ?: return@forEach

            when (loc.type) {
                LocationType.LENGTHWISE_WALL.id,
                LocationType.TRIANGULAR_CORNER.id,
                LocationType.RECTANGULAR_CORNER.id -> {
                    sceneRegion.newWall(z, x, y, width, length, staticObject, null, loc)
                }
                LocationType.WALL_CORNER.id -> {
                    val entity2 = getEntity(objectDefinition, loc.type, loc.orientation + 1 and 3, xSize, height, ySize, z, baseX, baseY)
                    sceneRegion.newWall(z, x, y, width, length, staticObject, entity2, loc)
                }
                LocationType.INSIDE_WALL_DECORATION.id -> {
                    sceneRegion.newWallDecoration(z, x, y, staticObject)
                }
                LocationType.OUTSIDE_WALL_DECORATION.id -> {
                    val (displacementX, displacementY) = sceneRegion.getWallDisplacement(loc, z, x, y)
                    sceneRegion.newWallDecoration(z, x, y, staticObject, displacementX = displacementX, displacementY = displacementY)
                }
                LocationType.DIAGONAL_OUTSIDE_WALL_DECORATION.id -> {
                    val (displacementX, displacementY) = sceneRegion.getWallDiagonalDisplacement(loc, z, x, y)
                    sceneRegion.newWallDecoration(z, x, y, staticObject, displacementX = displacementX / 2, displacementY = displacementY / 2)
                }
                LocationType.DIAGONAL_INSIDE_WALL_DECORATION.id -> {
                    sceneRegion.newWallDecoration(z, x, y, staticObject)
                }
                LocationType.DIAGONAL_WALL_DECORATION.id -> {
                    val (displacementX, displacementY) = sceneRegion.getWallDiagonalDisplacement(loc, z, x, y)

                    val entity2 = getEntity(objectDefinition, loc.type, ((loc.orientation + 2) and 3) + 4, xSize, height, ySize, z, baseX, baseY)!!

                    sceneRegion.newWallDecoration(z, x, y, staticObject, entity2, displacementX / 2, displacementY / 2)
                }
                LocationType.DIAGONAL_WALL.id -> {
                    sceneRegion.newPseudoWall(z, x, y, width, length, staticObject, loc)
                }
                LocationType.INTERACTABLE.id,
                in LocationType.SLOPED_ROOF.id..LocationType.SLOPED_ROOF_OVERHANG_HARD_OUTER_CORNER.id -> {
                    sceneRegion.newGameObject(z, x, y, width, length, staticObject, loc)
                }
                LocationType.DIAGONAL_INTERACTABLE.id -> {
                    staticObject.model.rotate(0x100)
                    sceneRegion.newGameObject(z, x, y, width, length, staticObject, loc)
                }
                LocationType.FLOOR_DECORATION.id -> {
                    sceneRegion.newFloorDecoration(z, x, y, staticObject)
                }
                else -> {
                    logger.warn("SceneRegionLoader Loading something new? ${loc.type}")
                }
            }
        }

        sceneRegion.applyLighting(regionLoader)
        return sceneRegion
    }

    private fun fakeLocationsDefinition(regionId: Int): LocationsDefinition {
        logger.warn("Could not find location (entity) data for region $regionId")
        return LocationsDefinition(regionId)
    }

    private fun getEntity(
        objectDefinition: ObjectDefinition,
        type: Int,
        orientation: Int,
        xSize: Int,
        height: Int,
        ySize: Int,
        zPlane: Int,
        baseX: Int,
        baseY: Int
    ): Entity? {
        val modelDefinition: ModelDefinition =
            objectToModelConverter.toModel(objectDefinition, type, orientation) ?: return null

        val model = if (objectDefinition.mergeNormals) {
            Model.unlitFromDefinition(modelDefinition, objectDefinition.ambient, objectDefinition.contrast)
        } else {
            Model.lightFromDefinition(modelDefinition, objectDefinition.ambient, objectDefinition.contrast)
        }
        model.debugType = type

        if (objectDefinition.contouredGround >= 0) {
            model.contourGround(
                regionLoader,
                xSize,
                height,
                ySize,
                zPlane,
                baseX,
                baseY,
                objectDefinition.contouredGround
            )
        }

        return StaticObject(objectDefinition, model, height, type)
    }

    private fun packHsl(hue: Int, var1: Int, lightness: Int): Int {
        var saturation = var1
        if (lightness > 179) {
            saturation /= 2
        }
        if (lightness > 192) {
            saturation /= 2
        }
        if (lightness > 217) {
            saturation /= 2
        }
        if (lightness > 243) {
            saturation /= 2
        }
        return ((saturation / 32) shl 7) + ((hue / 4) shl 10) + (lightness / 2)
    }

    companion object {
        val sceneRegionCache = hashMapOf<Int, SceneRegion>()

        fun multiplyHslBrightness(hsl: Int, brightness: Int): Int {
            val adjustedBrightness = ((hsl and 0x7f) * brightness) / 0x80
            return (hsl and 0xff80) + adjustedBrightness.clamp(2, 126)
        }

        fun adjustUnderlayBrightness(hsl: Int, brightness: Int): Int =
            when (hsl) {
                -1 -> 12345678
                else -> multiplyHslBrightness(hsl, brightness)
            }

        fun adjustOverlayBrightness(hsl: Int, brightness: Int): Int =
            when (hsl) {
                -2 -> 12345678
                -1 -> brightness.clamp(2, 126)
                else -> multiplyHslBrightness(hsl, brightness)
            }
    }
}
