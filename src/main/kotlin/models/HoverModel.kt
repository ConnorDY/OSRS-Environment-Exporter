package models

import cache.LocationType
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import models.scene.SceneTile

class HoverModel {
    val hovered: ObjectProperty<HoverObject> = SimpleObjectProperty()
}

data class HoverObject(
    val x: Int,
    val y: Int,
    val type: LocationType,
    val sceneTile: SceneTile
) {
    override fun toString(): String {
        return "sceneTile: ($x,$y) $type ${sceneTile.gameObjects}, ${sceneTile.tileModel}"
    }
}