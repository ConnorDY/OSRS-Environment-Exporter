package models

import cache.definitions.ObjectDefinition
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.layout.*
import javafx.scene.paint.Color

class ObjectSwatchItem(
    private val objectDefinition: ObjectDefinition,
    private val image: WritableImage,
    private val label: String,
    private val modelType: LocationType
) {
    private val container = VBox()
    private var isSelected = false
    fun toView(): Region {
        val iv = ImageView(image)
        iv.fitWidth = 125.0
        iv.fitHeight = 75.0
        iv.isPreserveRatio = true
        iv.isPickOnBounds = true
        container.alignment = Pos.CENTER
        container.children.addAll(
            iv,
            Label(label),
            Label(
                LocationType.valueOf(modelType.name).toString().toLowerCase().replace("_", " ").capitalize()
            )
        )
        container.isFocusTraversable = true
        return container
    }

    fun select() {
        isSelected = true
        container.background = Background(
            BackgroundFill(
                Color.GREY,
                CornerRadii.EMPTY,
                Insets.EMPTY
            )
        )
    }

    fun deselect() {
        isSelected = false
        container.background = Background.EMPTY
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is ObjectSwatchItem) return false
        return objectDefinition.id == other.objectDefinition.id
    }
}