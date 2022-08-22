package models.scene

import models.DebugOptionsModel
import org.slf4j.LoggerFactory
import ui.CancelledException
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
    val sceneLoadProgressListeners = ArrayList<SceneLoadProgressListener>()

    val rows get() = regions.size
    val cols get() = if (regions.isEmpty()) 0 else regions[0].size
    val numRegions get() = regions.sumOf { row -> row.count { it != null } }

    init {
        val listener: (Any?) -> Unit = {
            reloadRegions()
        }
        debugOptionsModel.modelSubIndex.value.addListener(listener)
        debugOptionsModel.badModelIndexOverride.value.addListener(listener)
        debugOptionsModel.removeProperlyTypedModels.value.addListener(listener)
    }

    private fun startLoading(numRegions: Int) {
        logger.info("Loading $numRegions regions")
        sceneLoadProgressListeners.forEach { it.onBeginLoadingRegions(numRegions) }
    }

    private fun finishLoadingRegion() {
        sceneLoadProgressListeners.forEach { it.onRegionLoaded() }
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
        onAnotherThreadCancellable {
            val numRegions = regionIds.sumOf { row -> row.count { it != null } }
            startLoading(numRegions)
            regions = regionIds.map { row ->
                row.map { regionId ->
                    if (regionId == null) null
                    else {
                        logger.info("Loading region {}", regionId)
                        sceneRegionBuilder.loadRegion(regionId).also {
                            finishLoadingRegion()
                        }
                    }
                }.toTypedArray()
            }.toTypedArray()

            reload()
        }
    }

    private fun reloadRegions() {
        onAnotherThreadCancellable {
            startLoading(numRegions)
            regions.forEach { row ->
                row.indices.forEach inner@{
                    val region = row[it] ?: return@inner
                    row[it] = sceneRegionBuilder.loadRegion(region.locationsDefinition.regionId)
                    finishLoadingRegion()
                }
            }

            reload()
        }
    }

    private fun onAnotherThreadCancellable(block: () -> Unit) {
        Thread {
            try {
                block()
            } catch (e: CancelledException) {
                // Do nothing
            } catch (e: Exception) {
                logger.error("Error loading scene", e)
                sceneLoadProgressListeners.forEach(SceneLoadProgressListener::onError)
            }
        }.apply {
            start()
        }
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
        return region.tiles[z][regionX][regionY]
    }

    fun getRegion(gridX: Int, gridY: Int): SceneRegion? {
        return if (gridY >= rows || gridX >= cols) {
            null
        } else regions[gridY][gridX]
    }
}
