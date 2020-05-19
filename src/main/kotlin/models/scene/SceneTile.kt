package models.scene

import cache.definitions.Location
import cache.definitions.RegionDefinition
import controllers.worldRenderer.entities.FloorDecoration
import controllers.worldRenderer.entities.TileModel
import controllers.worldRenderer.entities.TilePaint
import controllers.worldRenderer.entities.WallDecoration
import java.util.*

class SceneTile(val z: Int, var x: Int, var y: Int) {
    private lateinit var cacheTile: RegionDefinition.Tile
    var locations: List<Location> = ArrayList<Location>()
    var tilePaint: TilePaint? = null
    var tileModel: TileModel? = null
    var floorDecoration: FloorDecoration? = null
    var wallDecoration: WallDecoration? = null
//    private val gameObjects: List<GameObject> = ArrayList<GameObject>()
//    private val boundaryObjects: List<WallDecoration> = ArrayList<WallDecoration>()

//    override fun equals(other: Any?): Boolean {
//        if (other === this) return true
//        if (other !is SceneTile) return false
//        return x == other.x && y == other.y && z == other.z
//    }
}