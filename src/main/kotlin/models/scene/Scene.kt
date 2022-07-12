package models.scene


import com.google.inject.Inject
import java.awt.event.ActionListener
import java.util.function.Consumer

const val REGION_SIZE = 64
const val REGION_HEIGHT = 4

class Scene @Inject constructor(
    private val sceneRegionBuilder: SceneRegionBuilder
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

    fun getRegion(gridX: Int, gridY: Int): SceneRegion? {
        return if (gridX >= radius || gridY >= radius) {
            null
        } else regions[gridX][gridY]
    }
}
