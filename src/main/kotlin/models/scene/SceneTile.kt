package models.scene

import cache.definitions.Location
import cache.definitions.OverlayDefinition
import cache.definitions.RegionDefinition
import cache.definitions.UnderlayDefinition
import controllers.worldRenderer.entities.FloorDecoration
import controllers.worldRenderer.entities.GameObject
import controllers.worldRenderer.entities.TileModel
import controllers.worldRenderer.entities.TilePaint
import controllers.worldRenderer.entities.WallDecoration
import controllers.worldRenderer.entities.WallObject
import kotlin.collections.ArrayList

class SceneTile(val z: Int, var x: Int, var y: Int) {

    var locations: ArrayList<Location> = ArrayList()
    var cacheTile: RegionDefinition.Tile? = null
    var overlayDefinition: OverlayDefinition? = null
    var underlayDefinition: UnderlayDefinition? = null

    var tilePaint: TilePaint? = null
    var tileModel: TileModel? = null

    var floorDecoration: FloorDecoration? = null
    var wallDecoration: WallDecoration? = null
    var wall: WallObject? = null
    var wallDisplacement: Int = 16
    val gameObjects: ArrayList<GameObject> = ArrayList()
}
