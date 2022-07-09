package models.scene

import cache.LocationType
import cache.definitions.*
import cache.definitions.converters.ObjectToModelConverter
import cache.loaders.*
import cache.utils.ColorPalette
import controllers.worldRenderer.entities.Entity
import controllers.worldRenderer.entities.Model
import controllers.worldRenderer.entities.OrientationType
import controllers.worldRenderer.entities.StaticObject
import javax.inject.Inject
import kotlin.math.sqrt

class SceneRegionBuilder @Inject constructor(
    private val regionLoader: RegionLoader,
    private val locationsLoader: LocationsLoader,
    private val objectLoader: ObjectLoader,
    private val textureLoader: TextureLoader,
    private val underlayLoader: UnderlayLoader,
    private val overlayLoader: OverlayLoader,
    private val objectToModelConverter: ObjectToModelConverter
) {

    private val colorPalette = ColorPalette(0.7, 0, 512).colorPalette

    fun calcTileColor(sceneRegion: SceneRegion, x: Int, y: Int, baseX: Int, baseY: Int) {
        val var9 = sqrt(5100.0).toInt()
        val var10 = var9 * 768 shr 8
        val z = 0

        val worldX = baseX + x
        val worldY = baseY + y
        val xHeightDiff = getTileHeight(z, worldX + 1, worldY) - getTileHeight(z, worldX - 1, worldY)
        val yHeightDiff = getTileHeight(z, worldX, worldY + 1) - getTileHeight(z, worldX, worldY - 1)
        val diff = sqrt(xHeightDiff * xHeightDiff + yHeightDiff * yHeightDiff + 65536.toDouble()).toInt()
        val var16 = (xHeightDiff shl 8) / diff
        val var17 = 65536 / diff
        val var18 = (yHeightDiff shl 8) / diff
        val var19 = (var16 * -50 + var18 * -50 + var17 * -10) / var10 + 96
        val color = (getTileSettings(z, worldX - 1, worldY) shr 2) +
                (getTileSettings(z, worldX, worldY - 1) shr 2) +
                (getTileSettings(z, worldX + 1, worldY) shr 3) +
                (getTileSettings(z, worldX, worldY + 1) shr 3) +
                (getTileSettings(z, worldX, worldY) shr 1)
        sceneRegion.tileColors[x][y] = var19 - color
    }

    // Loads a single region(rs size 64), not a scene(rs size 104)!
    // worldCoords to regionId
    // int regionId = (x >>> 6 << 8) | y >>> 6;
    fun loadRegion(regionId: Int, isAnimationEnabled: Boolean): SceneRegion? {
        val region: RegionDefinition = regionLoader.get(regionId) ?: return null
        val locations: LocationsDefinition = locationsLoader.get(regionId) ?: return null
        val sceneRegion = SceneRegion(region, locations)
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
                    calcTileColor(sceneRegion, x, y, baseX, baseY)
                }
            }
            for (xi in -blend * 2 until REGION_SIZE + blend * 2) {
                for (yi in -blend until REGION_SIZE + blend) {
                    val xr = xi + blend
                    if (xr >= -blend && xr < REGION_SIZE + blend) {
                        val r: RegionDefinition? = regionLoader.findRegionForWorldCoordinates(baseX + xr, baseY + yi)
                        if (r != null) {
                            val underlayId: Int = r.tiles[z][convert(xr)][convert(yi)]?.underlayId!!.toInt()
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
                            val underlayId: Int = r.tiles[z][convert(xl)][convert(yi)]?.underlayId!!.toInt()
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
                            val underlayId: Int = r.tiles[z][xi][yi]?.underlayId!!.toInt() and 0xFF
                            val overlayId: Int = r.tiles[z][xi][yi]?.overlayId!!.toInt() and 0xFF
                            if (underlayId <= 0 && overlayId <= 0) {
                                continue
                            }
                            val swHeight = getTileHeight(z, baseX + xi, baseY + yi)
                            val seHeight = getTileHeight(z, baseX + xi + 1, baseY + yi)
                            val neHeight = getTileHeight(z, baseX + xi + 1, baseY + yi + 1)
                            val nwHeight = getTileHeight(z, baseX + xi, baseY + yi + 1)
                            val swColor = sceneRegion.tileColors[xi][yi]
                            val seColor = sceneRegion.tileColors[xi + 1][yi]
                            val neColor = sceneRegion.tileColors[xi + 1][yi + 1]
                            val nwColor = sceneRegion.tileColors[xi][yi + 1]
                            var rgb = -1
                            var underlayHsl = -1
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
                                val overlayPath: Int = r.tiles[z][xi][yi]?.overlayPath!!.toInt() + 1
                                val overlayRotation: Int = r.tiles[z][xi][yi]?.overlayRotation!!.toInt()
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
            val swHeight = getTileHeight(z, baseX + var12, baseY + var14)
            val seHeight = getTileHeight(z, baseX + var11, baseY + var14)
            val neHeight = getTileHeight(z, baseX + var12, baseY + var13)
            val nwHeight = getTileHeight(z, baseX + var11, baseY + var13)
            val height = swHeight + seHeight + neHeight + nwHeight shr 2
            val orientationTransform = intArrayOf(1, 2, 4, 8)
            val xTransforms = intArrayOf(1, 0, -1, 0)
            val yTransforms = intArrayOf(0, -1, 0, 1)

            val staticObject =
                getEntity(objectDefinition, loc.type, loc.orientation, xSize, height, ySize, baseX, baseY)
                    ?: return@forEach

            if (loc.type == LocationType.LENGTHWISE_WALL.id) {
                sceneRegion.newWall(z, x, y, width, length, staticObject, null, loc)
            }

            if (loc.type == LocationType.WALL_CORNER.id) {
                val entity1 =
                    getEntity(objectDefinition, loc.type, loc.orientation + 1 and 3, xSize, height, ySize, baseX, baseY)
                val entity2 =
                    getEntity(objectDefinition, loc.type, loc.orientation + 4, xSize, height, ySize, baseX, baseY)
                sceneRegion.newWall(z, x, y, width, length, entity1, entity2, loc)
            }

            if (loc.type in LocationType.INTERACTABLE_WALL.id .. LocationType.DIAGONAL_WALL.id) {
                sceneRegion.newGameObject(z, x, y, width, length, staticObject, loc)
            }

            if (loc.type == LocationType.FLOOR_DECORATION.id) {
                sceneRegion.newFloorDecoration(z, x, y, staticObject)
            }

            if (loc.type == LocationType.INTERACTABLE_WALL_DECORATION.id) {
                sceneRegion.newWallDecoration(z, x, y, staticObject)
            }

            if (loc.type == LocationType.INTERACTABLE.id) {
                sceneRegion.newGameObject(z, x, y, width, length, staticObject, loc)
            }

            if (loc.type == LocationType.DIAGONAL_INTERACTABLE.id) {
                staticObject.getModel().orientationType = OrientationType.DIAGONAL
                sceneRegion.newGameObject(z, x, y, width, length, staticObject, loc)
            }

            // Other objects ?
            if (loc.type in 12..21) {
                sceneRegion.newGameObject(z, x, y, width, length, staticObject, loc)
            }
        }

        return sceneRegion
    }

    data class ModelKey(
        val id: Int,
        val type: Int,
        val orientation: Int,
        val ambient: Int,
        val contrast: Int
    )

    private val entityCache: HashMap<ModelKey, Model> = HashMap()
    private fun getEntity(
        objectDefinition: ObjectDefinition,
        type: Int,
        orientation: Int,
        xSize: Int,
        height: Int,
        ySize: Int,
        baseX: Int,
        baseY: Int
    ): Entity? {
        val modelDefinition: ModelDefinition =
            objectToModelConverter.toModel(objectDefinition, type, orientation) ?: return null

        // FIXME: nonFlatShading affects fence doors
        var model = Model(modelDefinition, objectDefinition.ambient, objectDefinition.contrast)

        if (objectDefinition.contouredGround >= 0) {
            model = model.contourGround(
                regionLoader,
                xSize,
                height,
                ySize,
                baseX,
                baseY,
                true,
                objectDefinition.contouredGround
            )
        }

        return StaticObject(objectDefinition, model, height, type, orientation)
    }

    private fun getTileHeight(z: Int, x: Int, y: Int): Int {
        val r: RegionDefinition = regionLoader.findRegionForWorldCoordinates(x, y) ?: return 0
        return r.tiles[z][x % 64][y % 64]?.height ?: return 0
    }

    private fun getTileSettings(z: Int, x: Int, y: Int): Int {
        val r: RegionDefinition = regionLoader.findRegionForWorldCoordinates(x, y) ?: return 0
        return r.tileSettings[z][x % 64][y % 64]
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

        fun method4220(var0: Int, var1: Int): Int {
            var var1 = var1
            return if (var0 == -1) {
                12345678
            } else {
                var1 = (var0 and 127) * var1 / 128
                if (var1 < 2) {
                    var1 = 2
                } else if (var1 > 126) {
                    var1 = 126
                }
                (var0 and 65408) + var1
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