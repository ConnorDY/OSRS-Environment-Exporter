package controllers.worldRenderer.entities

import cache.LocationType
import utils.Observable

class WallObject(
    val entity: Entity?,
    val entity2: Entity?,
    val type: LocationType
): Observable<WallObject>()