package models.scene

import cache.definitions.Location
import cache.definitions.OverlayDefinition
import cache.definitions.RegionDefinition
import cache.definitions.UnderlayDefinition
import controllers.worldRenderer.entities.Entity
import controllers.worldRenderer.entities.GameObject
import controllers.worldRenderer.entities.TileModel
import controllers.worldRenderer.entities.TilePaint

class SceneTile {
    /** How an entity is attached to this tile.
     *  Similar to [cache.LocationType].
     */
    enum class Attachment {
        WALL_OBJECT,
        OFFSET_WALL_DECORATION,
        WALL_DECORATION,
        FLOOR_DECORATION,
        OTHER,
    }

    var locations: ArrayList<Location> = ArrayList()
    var cacheTile: RegionDefinition.Tile? = null
    var overlayDefinition: OverlayDefinition? = null
    var underlayDefinition: UnderlayDefinition? = null

    var tilePaint: TilePaint? = null
    var tileModel: TileModel? = null

    var wallDisplacement: Int = 16
    val attachments: ArrayList<Pair<Attachment, Entity>> = ArrayList()
    val gameObjects: ArrayList<GameObject> = ArrayList()

    val allEntities get() = attachments.map { it.second } + gameObjects.map { it.entity }
}
