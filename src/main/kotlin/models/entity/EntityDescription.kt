package models.entity

import cache.LocationType
import cache.definitions.RegionDefinition.Companion.X
import cache.definitions.RegionDefinition.Companion.Y
import controllers.worldRenderer.entities.Entity
import utils.Utils.worldCoordinatesToRegionId

data class EntityDescription(
    var locationType: LocationType,
    var objectType: Int,
    var orientation: Int,
    var z: Int,
    var x: Int,
    var y: Int,
    var regionId: Int, /* For display to the user */
) {
    companion object {
        fun fromEntity(z: Int, x: Int, y: Int, entity: Entity): EntityDescription {
            return EntityDescription(
                LocationType.fromId(entity.type)!!,
                entity.objectDefinition.id,
                entity.model.orientation,
                z,
                x % X,
                y % Y,
                worldCoordinatesToRegionId(x, y),
            )
        }
    }
}
