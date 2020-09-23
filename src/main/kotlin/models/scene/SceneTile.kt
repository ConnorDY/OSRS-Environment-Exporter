package models.scene

import cache.definitions.Location
import cache.definitions.OverlayDefinition
import cache.definitions.RegionDefinition
import cache.definitions.UnderlayDefinition
import controllers.worldRenderer.components.HoverComponent
import controllers.worldRenderer.components.Hoverable
import controllers.worldRenderer.entities.*
import utils.Observable
import kotlin.collections.ArrayList

class SceneTile(val z: Int, var x: Int, var y: Int, hoverComponent: HoverComponent = HoverComponent()): Observable<SceneTile>(), Hoverable by hoverComponent {

    var locations: ArrayList<Location> = ArrayList<Location>()
    var cacheTile: RegionDefinition.Tile? = null
    var overlayDefinition: OverlayDefinition? = null
    var underlayDefinition: UnderlayDefinition? = null

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