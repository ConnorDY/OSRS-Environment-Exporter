package models.scene

import cache.definitions.Location
import cache.definitions.OverlayDefinition
import cache.definitions.RegionDefinition
import cache.definitions.UnderlayDefinition
import controllers.worldRenderer.entities.*
import kotlin.collections.ArrayList

class SceneTile(val z: Int, var x: Int, var y: Int) {

    var locations: ArrayList<Location> = ArrayList<Location>()
    var cacheTile: RegionDefinition.Tile? = null
    var overlayDefinition: OverlayDefinition? = null
    var underlayDefinition: UnderlayDefinition? = null

    var tilePaint: TilePaint? = null
    var tileModel: TileModel? = null

    var floorDecoration: FloorDecoration? = null
    var wallDecoration: WallDecoration? = null
    var wall: WallObject? = null
    val gameObjects: ArrayList<GameObject> = ArrayList()
}
