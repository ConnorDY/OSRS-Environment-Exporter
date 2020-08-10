package models.scene

import cache.definitions.Location
import cache.definitions.RegionDefinition
import controllers.worldRenderer.entities.*
import utils.Observable
import kotlin.collections.ArrayList

class SceneTile(val z: Int, var x: Int, var y: Int): Observable<SceneTile>() {

    private lateinit var cacheTile: RegionDefinition.Tile
    var locations: ArrayList<Location> = ArrayList<Location>()

    var tilePaint: TilePaint? = null
        set(value) {
            value?.addListener { notifyObservers(it.second) }
            field = value
        }

    var tileModel: TileModel? = null
        set(value) {
            value?.addListener { notifyObservers(it.second) }
            field = value
        }

    var floorDecoration: FloorDecoration? = null
    var wallDecoration: WallDecoration? = null
    var wall: WallObject? = null
    val gameObjects: ArrayList<GameObject> = ArrayList()

}