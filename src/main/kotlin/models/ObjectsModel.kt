package models

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import models.scene.SceneObject

class ObjectsModel {
    val heldObject: ObjectProperty<SceneObject> = SimpleObjectProperty()
}