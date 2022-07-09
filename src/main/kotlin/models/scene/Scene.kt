package models.scene


import cache.definitions.ModelDefinition
import cache.definitions.OverlayDefinition
import cache.definitions.RegionDefinition
import cache.definitions.UnderlayDefinition
import cache.definitions.converters.ObjectToModelConverter
import cache.loaders.OverlayLoader
import cache.loaders.RegionLoader
import cache.loaders.TextureLoader
import cache.loaders.UnderlayLoader
import cache.utils.ColorPalette
import com.google.inject.Inject
import controllers.worldRenderer.entities.Entity
import controllers.worldRenderer.entities.Model
import controllers.worldRenderer.entities.StaticObject
import java.awt.event.ActionListener
import java.util.function.Consumer

const val REGION_SIZE = 64
const val REGION_HEIGHT = 4

class Scene @Inject constructor(
    private val sceneRegionBuilder: SceneRegionBuilder,
    private val underlayLoader: UnderlayLoader,
    private val regionLoader: RegionLoader,
    private val objectToModelConverter: ObjectToModelConverter,
    private val overlayLoader: OverlayLoader,
    private val textureLoader: TextureLoader
) {
    var radius: Int = 1

    // NxM grid of regions to display
    private lateinit var regions: Array<Array<SceneRegion?>>
    val sceneChangeListeners: ArrayList<ActionListener> = ArrayList()

    private fun reload() {
        sceneChangeListeners.forEach(Consumer {
            it.actionPerformed(
                null
            )
        })
    }

    fun load(centerRegionId: Int, radius: Int) {
        this.radius = radius
        regions = Array(radius) { arrayOfNulls<SceneRegion>(radius) }
        var regionId = centerRegionId
        if (radius > 1) {
            regionId = centerRegionId - 256 * (radius - 2) - (radius - 2)
        }
        for (x in 0 until radius) {
            for (y in 0 until radius) {
                System.out.printf("Loading region %d\n", regionId)
                regions[x][y] = sceneRegionBuilder.loadRegion(regionId, true)
                regionId++
            }
            regionId += 256 - radius // move 1 region to the right, reset to lowest y
        }
        reload()
    }

    fun recalculateRegion(sceneRegion: SceneRegion) {
        for (z in 0 until REGION_HEIGHT) {
            for (x in 0 until REGION_SIZE + 1) {
                for (y in 0 until REGION_SIZE + 1) {
                    sceneRegionBuilder.calcTileColor(
                        sceneRegion,
                        x,
                        y,
                        sceneRegion.regionDefinition.baseX,
                        sceneRegion.regionDefinition.baseY
                    )
                }
            }
            calcColor(sceneRegion, z, sceneRegion.regionDefinition.baseX, sceneRegion.regionDefinition.baseY)
        }
    }

    fun getTile(z: Int, x: Int, y: Int): SceneTile? {
        if (x < 0 || y < 0) {
            return null
        }

        // figure which SceneRegion(n, m) the tile exists in
        val gridX: Int = x / REGION_SIZE
        val gridY: Int = y / REGION_SIZE
        if (gridX >= radius || gridY >= radius) {
            return null
        }
        val region = getRegion(gridX, gridY) ?: return null
        val regionX: Int = x % REGION_SIZE
        val regionY: Int = y % REGION_SIZE
        val tile: SceneTile? = region.tiles[z][regionX][regionY]
        if (tile != null) {
            // offset the tile by adding it's scene offset to it's region offset - get scene position
            tile.x = regionX + gridX * REGION_SIZE
            tile.y = regionY + gridY * REGION_SIZE
        }
        return tile
    }

    fun getRegions(): Array<Array<SceneRegion?>> {
        return this.regions
    }

    fun getRegion(gridX: Int, gridY: Int): SceneRegion? {
        return if (gridX >= radius || gridY >= radius) {
            null
        } else regions[gridX][gridY]
    }

    fun getRegionFromSceneCoord(x: Int, y: Int): SceneRegion? {
        if (x < 0 || y < 0) {
            return null
        }

        // figure which SceneRegion(n, m) the tile exists in
        val gridX: Int = x / REGION_SIZE
        val gridY: Int = y / REGION_SIZE
        return if (gridX >= radius || gridY >= radius) {
            null
        } else getRegion(gridX, gridY)
    }

    private val colorPalette = ColorPalette(0.7, 0, 512).colorPalette

    private fun calcColor(region: SceneRegion, z: Int, baseX: Int, baseY: Int) {
        val blend = 5
        val len: Int = REGION_SIZE * blend * 2// 1 + blend * 2
        val hues = IntArray(len)
        val sats = IntArray(len)
        val light = IntArray(len)
        val mul = IntArray(len)
        val num = IntArray(len)

        for (xi in -blend * 2 until REGION_SIZE + blend * 2) {
            for (yi in -blend until REGION_SIZE + blend) {
                val xr = xi + blend
                if (xr >= -blend && xr < REGION_SIZE + blend) {
                    val r: RegionDefinition? = regionLoader.findRegionForWorldCoordinates(baseX + xr, baseY + yi)
                    if (r != null) {
                        val underlayId: Int =
                            r.tiles[z][SceneRegionBuilder.convert(xr)][SceneRegionBuilder.convert(yi)]?.underlayId!!.toInt()
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
                        val underlayId: Int =
                            r.tiles[z][SceneRegionBuilder.convert(xl)][SceneRegionBuilder.convert(yi)]?.underlayId!!.toInt()
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
                        val sr: SceneRegion = region
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
                        val swColor = sr.tileColors[xi][yi]
                        val seColor = sr.tileColors[xi + 1][yi]
                        val neColor = sr.tileColors[xi + 1][yi + 1]
                        val nwColor = sr.tileColors[xi][yi + 1]
                        var rgb = -1
                        var underlayHsl = -1
                        if (underlayId > 0 && runningHues > 0 && runningMultiplier > 0 && runningNumber > 0) {
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
                            val var0 = SceneRegionBuilder.method4220(underlayHsl, 96)
                            underlayRgb = colorPalette[var0]
                        }

                        if (overlayId == 0) {
                            sr.tiles[z][xi][yi]?.tilePaint?.setHeight(swHeight, seHeight, neHeight, nwHeight)
                            sr.tiles[z][xi][yi]?.tilePaint?.setColor(
                                SceneRegionBuilder.method4220(rgb, swColor),
                                SceneRegionBuilder.method4220(rgb, seColor),
                                SceneRegionBuilder.method4220(rgb, neColor),
                                SceneRegionBuilder.method4220(rgb, nwColor)
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
//                            var overlayRgb = 0
//                            if (overlayCol != -2) {
//                                val var0 = SceneRegionBuilder.adjustHSLListness0(overlayCol, 96)
//                                overlayRgb = colorPalette[var0]
//                            }
//                            if (overlayDefinition.secondaryRgbColor != -1) {
//                                val hue: Int = overlayDefinition.otherHue and 255
//                                var lightness: Int = overlayDefinition.otherLightness
//                                if (lightness < 0) {
//                                    lightness = 0
//                                } else if (lightness > 255) {
//                                    lightness = 255
//                                }
//                                overlayCol = hslToRgb(hue, overlayDefinition.otherSaturation, lightness)
//                                val var0 = SceneRegionBuilder.adjustHSLListness0(overlayCol, 96)
//                                overlayRgb = colorPalette[var0]
//                            }
//                            val underlay: UnderlayDefinition? = underlayLoader.get(underlayId - 1)
                            sr.tiles[z][xi][yi]?.tileModel?.setHeight(swHeight, seHeight, neHeight, nwHeight)
//                            sr.tiles[z][xi][yi]?.tileModel?.setColor(
//                                SceneRegionBuilder.method4220(rgb, swColor),
//                                SceneRegionBuilder.method4220(rgb, seColor),
//                                SceneRegionBuilder.method4220(rgb, neColor),
//                                SceneRegionBuilder.method4220(rgb, nwColor)
//                            )
                            sr.tiles[z][xi][yi]?.tileModel?.setBrightnessMaybe(
                                SceneRegionBuilder.adjustHSLListness0(overlayHsl, swColor),
                                SceneRegionBuilder.adjustHSLListness0(overlayHsl, seColor),
                                SceneRegionBuilder.adjustHSLListness0(overlayHsl, neColor),
                                SceneRegionBuilder.adjustHSLListness0(overlayHsl, nwColor)
                            )
                            // sr.tiles[z][xi][yi]?.tilePaint?.setHeight(swHeight, seHeight, neHeight, nwHeight)
                            sr.tiles[z][xi][yi]?.tilePaint?.setColor(
                                SceneRegionBuilder.adjustHSLListness0(overlayHsl, swColor),
                                SceneRegionBuilder.adjustHSLListness0(overlayHsl, seColor),
                                SceneRegionBuilder.adjustHSLListness0(overlayHsl, neColor),
                                SceneRegionBuilder.adjustHSLListness0(overlayHsl, nwColor)
                            )
                        }

                        sr.tiles[z][xi][yi]?.gameObjects?.forEach {
                            stickToGround(it.entity, null, z, xi, yi, baseX, baseY)
                        }

                        stickToGround(sr.tiles[z][xi][yi]?.floorDecoration?.entity, null, z, xi, yi, baseX, baseY)

                        stickToGround(sr.tiles[z][xi][yi]?.wall?.entity, null, z, xi, yi, baseX, baseY)
                    }
                }
            }
        }
    }

    private fun stickToGround(entity: Entity?, entity2: Entity?, z: Int, x: Int, y: Int, baseX: Int, baseY: Int) {
        val objectDefinition = entity?.objectDefinition ?: return
        val width: Int
        val length: Int
        if (entity.getModel().orientation != 1 && entity.getModel().orientation != 3) {
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

        if (entity.height == height) return

        if (entity.objectDefinition.contouredGround >= 0) {
            val modelDefinition: ModelDefinition =
                objectToModelConverter.toModel(objectDefinition, entity.type, entity.orientation)
                    ?: return

            var model = Model(modelDefinition, objectDefinition.ambient, objectDefinition.contrast)

            model = model.contourGround(
                regionLoader,
                xSize,
                height,
                ySize,
                baseX,
                baseY,
                false,
                objectDefinition.contouredGround
            )

            model.computeObj = entity.getModel().computeObj.copy()
            model.computeObj.offset = -1
            model.computeObj.flags = 0
            entity.height = height
            (entity as StaticObject).setModel(model)
        } else {
            entity.height = height
            (entity as StaticObject).setModel(entity.getModel())
        }
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
}