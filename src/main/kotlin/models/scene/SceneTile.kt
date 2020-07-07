package models.scene

import cache.definitions.Location
import cache.definitions.RegionDefinition
import controllers.worldRenderer.entities.*
import java.util.*
import kotlin.collections.ArrayList

class SceneTile(val z: Int, var x: Int, var y: Int) {
    private lateinit var cacheTile: RegionDefinition.Tile
    var locations: ArrayList<Location> = ArrayList<Location>()
    var tilePaint: TilePaint? = null
    var tileModel: TileModel? = null
    var floorDecoration: FloorDecoration? = null
    var wallDecoration: WallDecoration? = null
    var wall: WallObject? = null
    val gameObjects: ArrayList<GameObject> = ArrayList()
//    private val boundaryObjects: List<WallDecoration> = ArrayList<WallDecoration>()

//    override fun equals(other: Any?): Boolean {
//        if (other === this) return true
//        if (other !is SceneTile) return false
//        return x == other.x && y == other.y && z == other.z
//    }
}