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
import cache.loaders.TextureLoader
import cache.loaders.UnderlayLoader
import cache.loaders.getTileHeight
import cache.loaders.getTileSettings
import cache.utils.ColorPalette
import cache.utils.Vec3F
import controllers.worldRenderer.entities.Entity
import controllers.worldRenderer.entities.Model
import controllers.worldRenderer.entities.OrientationType
import controllers.worldRenderer.entities.StaticObject
import org.slf4j.LoggerFactory

class SceneRegionBuilder constructor(
    private val regionLoader: RegionLoader,
    private val locationsLoader: LocationsLoader,
    private val objectLoader: ObjectLoader,
    private val textureLoader: TextureLoader,
    private val underlayLoader: UnderlayLoader,
    private val overlayLoader: OverlayLoader,
    private val objectToModelConverter: ObjectToModelConverter
) {
    private val logger = LoggerFactory.getLogger(SceneRegionBuilder::class.java)

    private val colorPalette = ColorPalette(0.7).colorPalette

    fun calcTileColor(sceneRegion: SceneRegion, z: Int, x: Int, y: Int, baseX: Int, baseY: Int) {
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
        val slopeColorAdjust = (precision * bias.dot(slopeVec.normalizedAsInts())).toInt() / dotProductMagnitude + ambient
        val color = (regionLoader.getTileSettings(z, worldX - 1, worldY) shr 2) +
            (regionLoader.getTileSettings(z, worldX, worldY - 1) shr 2) +
            (regionLoader.getTileSettings(z, worldX + 1, worldY) shr 3) +
            (regionLoader.getTileSettings(z, worldX, worldY + 1) shr 3) +
            (regionLoader.getTileSettings(z, worldX, worldY) shr 1)
        sceneRegion.tileColors[x][y] = slopeColorAdjust - color
    }

    // Loads a single region(rs size 64), not a scene(rs size 104)!
    // worldCoords to regionId
    // int regionId = (x >>> 6 << 8) | y >>> 6;
    fun loadRegion(regionId: Int, isAnimationEnabled: Boolean): SceneRegion? {
        val region: RegionDefinition = regionLoader.get(regionId) ?: return null
        val locations: LocationsDefinition = locationsLoader.get(regionId) ?: fakeLocationsDefinition(regionId)
        val sceneRegion = SceneRegion(locations)
        val baseX: Int = region.baseX
        val baseY: Int = region.baseY
        val blend = 5
        val len: Int = REGION_SIZE * blend * 2
        val hues = IntArray(len)
        val sats = IntArray(len)
        val light = IntArray(len)
        val mul = IntArray(len)
        val num = IntArray(len)

        sceneRegionCache[regionId] = sceneRegion

        for (z in 0 until RegionDefinition.Z) {
            for (x in 0 until REGION_SIZE + 1) {
                for (y in 0 until REGION_SIZE + 1) {
                    calcTileColor(sceneRegion, z, x, y, baseX, baseY)
                }
            }
            for (xi in -blend * 2 until REGION_SIZE + blend * 2) {
                for (yi in -blend until REGION_SIZE + blend) {
                    val xr = xi + blend
                    if (xr >= -blend && xr < REGION_SIZE + blend) {
                        val r: RegionDefinition? = regionLoader.findRegionForWorldCoordinates(baseX + xr, baseY + yi)
                        if (r != null) {
                            val underlayId: Int = r.tiles[z][convert(xr)][convert(yi)].underlayId.toInt()
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
                    val xl = xi - 5
                    if (xl >= -blend && xl < REGION_SIZE + blend) {
                        val r: RegionDefinition? = regionLoader.findRegionForWorldCoordinates(baseX + xl, baseY + yi)
                        if (r != null) {
                            val underlayId: Int = r.tiles[z][convert(xl)][convert(yi)].underlayId.toInt()
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
                            val swColor = sceneRegion.tileColors[xi][yi]
                            val seColor = sceneRegion.tileColors[xi + 1][yi]
                            val neColor = sceneRegion.tileColors[xi + 1][yi + 1]
                            val nwColor = sceneRegion.tileColors[xi][yi + 1]
                            var rgb = -1
                            var underlayHsl = -1

                            if (runningMultiplier == 0) runningMultiplier = 1
                            if (runningNumber == 0) runningNumber = 1

                            if (underlayId > 0 && runningMultiplier > 0 && runningNumber > 0) {
                                val avgHue = runningHues * 256 / runningMultiplier
                                val avgSat = runningSat / runningNumber
                                var avgLight = runningLight / runningNumber
                                rgb = hslToRgb(avgHue, avgSat, avgLight)
                                if (avgLight < 0) {
                                    avgLight = 0
                                } else if (avgLight > 255) {
                                    avgLight = 255
                                }
                                underlayHsl = hslToRgb(avgHue, avgSat, avgLight)
                            }

                            var underlayRgb = 0
                            if (underlayHsl != -1) {
                                val var0 = method4220(underlayHsl, 96)
                                underlayRgb = colorPalette[var0]
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
                                    method4220(rgb, swColor),
                                    method4220(rgb, seColor),
                                    method4220(rgb, neColor),
                                    method4220(rgb, nwColor),
                                    0,
                                    0,
                                    0,
                                    0,
                                    underlayRgb,
                                    0,
                                    underlay,
                                    null,
                                    r.tiles[z][xi][yi]
                                )
                            } else {
                                val overlayPath: Int = r.tiles[z][xi][yi].overlayPath.toInt() + 1
                                val overlayRotation: Int = r.tiles[z][xi][yi].overlayRotation.toInt()
                                val overlayDefinition: OverlayDefinition? = overlayLoader.get(overlayId - 1)
                                var overlayTexture: Int = overlayDefinition?.texture!!
                                val overlayHsl: Int
                                var overlayCol: Int
                                when {
                                    overlayTexture >= 0 -> {
                                        overlayCol = textureLoader.getAverageTextureRGB(overlayTexture)
                                        overlayHsl = -1
                                    }
                                    overlayDefinition.rgbColor == 0xFF00FF -> {
                                        overlayHsl = -2
                                        overlayTexture = -1
                                        overlayCol = -2
                                    }
                                    else -> {
                                        overlayHsl = hslToRgb(
                                            overlayDefinition.hue,
                                            overlayDefinition.saturation,
                                            overlayDefinition.lightness
                                        )
                                        val hue: Int = overlayDefinition.hue and 255
                                        var lightness: Int = overlayDefinition.lightness
                                        if (lightness < 0) {
                                            lightness = 0
                                        } else if (lightness > 255) {
                                            lightness = 255
                                        }
                                        overlayCol = hslToRgb(hue, overlayDefinition.saturation, lightness)
                                    }
                                }
                                var overlayRgb = 0
                                if (overlayCol != -2) {
                                    val var0 = adjustHSLListness0(overlayCol, 96)
                                    overlayRgb = colorPalette[var0]
                                }
                                if (overlayDefinition.secondaryRgbColor != -1) {
                                    val hue: Int = overlayDefinition.otherHue and 255
                                    var lightness: Int = overlayDefinition.otherLightness
                                    if (lightness < 0) {
                                        lightness = 0
                                    } else if (lightness > 255) {
                                        lightness = 255
                                    }
                                    overlayCol = hslToRgb(hue, overlayDefinition.otherSaturation, lightness)
                                    val var0 = adjustHSLListness0(overlayCol, 96)
                                    overlayRgb = colorPalette[var0]
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
                                    method4220(rgb, swColor),
                                    method4220(rgb, seColor),
                                    method4220(rgb, neColor),
                                    method4220(rgb, nwColor),
                                    adjustHSLListness0(overlayHsl, swColor),
                                    adjustHSLListness0(overlayHsl, seColor),
                                    adjustHSLListness0(overlayHsl, neColor),
                                    adjustHSLListness0(overlayHsl, nwColor),
                                    underlayRgb,
                                    overlayRgb,
                                    underlay,
                                    overlayDefinition,
                                    r.tiles[z][xi][yi]
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
            val orientationTransform = intArrayOf(1, 2, 4, 8)
            val xTransforms = intArrayOf(1, 0, -1, 0)
            val yTransforms = intArrayOf(0, -1, 0, 1)

            val staticObject =
                getEntity(objectDefinition, loc.type, loc.orientation, xSize, height, ySize, z, baseX, baseY)
                    ?: return@forEach

            if (loc.type == LocationType.LENGTHWISE_WALL.id) {
                sceneRegion.newWall(z, x, y, width, length, staticObject, null, loc)
            } else if (loc.type == LocationType.WALL_CORNER.id) {
                val entity1 =
                    getEntity(objectDefinition, loc.type, loc.orientation + 1 and 3, xSize, height, ySize, z, baseX, baseY)!!
                val entity2 =
                    getEntity(objectDefinition, loc.type, loc.orientation + 4, xSize, height, ySize, z, baseX, baseY)
                sceneRegion.newWall(z, x, y, width, length, entity1, entity2, loc)
            } else if (loc.type in LocationType.INTERACTABLE_WALL.id..LocationType.DIAGONAL_WALL.id) {
                sceneRegion.newGameObject(z, x, y, width, length, staticObject, loc)
                return@forEach
            } else if (loc.type == LocationType.FLOOR_DECORATION.id) {
                sceneRegion.newFloorDecoration(z, x, y, staticObject)
            } else if (loc.type == LocationType.INTERACTABLE_WALL_DECORATION.id) {
                sceneRegion.newWallDecoration(z, x, y, staticObject)
            } else if (loc.type == LocationType.INTERACTABLE.id) {
                sceneRegion.newGameObject(z, x, y, width, length, staticObject, loc)
            } else if (loc.type == LocationType.DIAGONAL_INTERACTABLE.id) {
                staticObject.model.orientationType = OrientationType.DIAGONAL
                sceneRegion.newGameObject(z, x, y, width, length, staticObject, loc)
            } else if (loc.type == LocationType.TRIANGULAR_CORNER.id) {
                sceneRegion.newGameObject(z, x, y, width, length, staticObject, loc)
            } else if (loc.type == LocationType.RECTANGULAR_CORNER.id) {
                sceneRegion.newGameObject(z, x, y, width, length, staticObject, loc)
            }

            // Other objects ?
            else if (loc.type in 12..21) {
                sceneRegion.newGameObject(z, x, y, width, length, staticObject, loc)
                logger.debug("Load new object? ${loc.type}")
            } else {
                logger.warn("SceneRegionLoader Loading something new? ${loc.type}")
            }
        }

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

        // FIXME: nonFlatShading affects fence doors
        var model = Model(modelDefinition, objectDefinition.ambient, objectDefinition.contrast)

        model = model.scaleBy(objectDefinition.modelSizeX, objectDefinition.modelSizeHeight, objectDefinition.modelSizeY)

        if (objectDefinition.contouredGround >= 0) {
            model = model.contourGround(
                regionLoader,
                xSize,
                height,
                ySize,
                zPlane,
                baseX,
                baseY,
                true,
                objectDefinition.contouredGround
            )
        }

        return StaticObject(objectDefinition, model, height + objectDefinition.offsetHeight, type, orientation)
    }

    private fun hslToRgb(var0: Int, var1: Int, var2: Int): Int {
        var var1 = var1
        if (var2 > 179) {
            var1 /= 2
        }
        if (var2 > 192) {
            var1 /= 2
        }
        if (var2 > 217) {
            var1 /= 2
        }
        if (var2 > 243) {
            var1 /= 2
        }
        return (var1 / 32 shl 7) + (var0 / 4 shl 10) + var2 / 2
    }

    companion object {
        val sceneRegionCache = hashMapOf<Int, SceneRegion>()

        fun convert(d: Int): Int {
            return if (d >= 0) {
                d % 64
            } else {
                64 - -(d % 64) - 1
            }
        }

        fun method4220(rgb: Int, color: Int): Int {
            return if (rgb == -1) {
                12345678
            } else {
                var var1 = (rgb and 0x7f) * color / 0x80
                if (var1 < 2) {
                    var1 = 2
                } else if (var1 > 0x7e) {
                    var1 = 0x7e
                }
                (rgb and 0xff80) + var1
            }
        }

        fun adjustHSLListness0(var0: Int, var1: Int): Int {
            var v1 = var1
            return when (var0) {
                -2 -> {
                    12345678
                }
                -1 -> {
                    if (v1 < 2) {
                        v1 = 2
                    } else if (v1 > 126) {
                        v1 = 126
                    }
                    v1
                }
                else -> {
                    v1 = (var0 and 0x7f) * v1 / 128
                    if (v1 < 2) {
                        v1 = 2
                    } else if (v1 > 126) {
                        v1 = 126
                    }
                    (var0 and 0xff80) + v1
                }
            }
        }
    }
}
