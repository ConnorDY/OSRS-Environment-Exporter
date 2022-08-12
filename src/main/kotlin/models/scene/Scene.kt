package models.scene

import models.DebugOptionsModel
import org.slf4j.LoggerFactory
import java.awt.event.ActionListener
import java.util.function.Consumer

const val REGION_SIZE = 64
const val REGION_HEIGHT = 4

class Scene(
    private val sceneRegionBuilder: SceneRegionBuilder,
    debugOptionsModel: DebugOptionsModel,
) {
    private val logger = LoggerFactory.getLogger(Scene::class.java)

    // NxM grid of regions to display
    private var regions: Array<Array<SceneRegion?>> = emptyArray()
    val sceneChangeListeners: ArrayList<ActionListener> = ArrayList()

    val rows get() = regions.size
    val cols get() = regions[0].size

    init {
        val listener: (Any) -> Unit = {
            reloadRegions()
        }
        debugOptionsModel.onlyType10Models.addListener(listener)
        debugOptionsModel.modelSubIndex.addListener(listener)
    }

    private fun reload() {
        sceneChangeListeners.forEach(
            Consumer {
                it.actionPerformed(
                    null
                )
            }
        )
    }

    fun loadRadius(centerRegionId: Int, radius: Int) {
        regions = Array(radius) { arrayOfNulls(radius) }
        var regionId = centerRegionId
        if (radius > 1) {
            regionId = centerRegionId - 256 * (radius - 2) - (radius - 2)
        }

        loadRegions(
            List(radius) { y ->
                List(radius) { x ->
                    regionId + 256 * x + y
                }
            }
        )
    }

    /**
     * Load a list of region IDs & trigger rendering.
     * @param regionIds Non-empty list of rows of region IDs
     */
    fun loadRegions(regionIds: List<List<Int?>>) {
        regions = regionIds.map { row ->
            row.map { regionId ->
                if (regionId == null) null
                else {
                    logger.info("Loading region {}", regionId)
                    sceneRegionBuilder.loadRegion(regionId)
                }
            }.toTypedArray()
        }.toTypedArray()

        reload()
    }

    fun reloadRegions() {
        regions.forEach { row ->
            row.indices.forEach inner@{
                val region = row[it] ?: return@inner
                row[it] = sceneRegionBuilder.loadRegion(region.locationsDefinition.regionId)
            }
        }

        reload()
    }

    fun getTile(z: Int, x: Int, y: Int): SceneTile? {
        if (x < 0 || y < 0) {
            return null
        }

        // figure which SceneRegion(n, m) the tile exists in
        val gridX: Int = x / REGION_SIZE
        val gridY: Int = y / REGION_SIZE
        val region = getRegion(gridX, gridY) ?: return null
        val regionX: Int = x % REGION_SIZE
        val regionY: Int = y % REGION_SIZE
        val tile: SceneTile? = region.tiles[z][regionX][regionY]
        if (tile != null) {
            // offset the tile by adding its scene offset to its region offset - get scene position
            tile.x = regionX + gridX * REGION_SIZE
            tile.y = regionY + gridY * REGION_SIZE
        }
        return tile
    }

    fun getRegion(gridX: Int, gridY: Int): SceneRegion? {
        return if (gridY >= rows || gridX >= cols) {
            null
        } else regions[gridY][gridX]
    }
}
