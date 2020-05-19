package models

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList

class ObjectSwatchModel {
    val objectList: ObservableList<ObjectSwatchItem> = FXCollections.observableArrayList()
    private val selectedObject: ObjectProperty<ObjectSwatchItem?> = SimpleObjectProperty(null)
    fun selectedObjectProperty(): ObjectProperty<ObjectSwatchItem?> {
        return selectedObject
    }

    fun getSelectedObject(): ObjectSwatchItem? {
        return selectedObject.get()
    }

    fun setSelectedObject(o: ObjectSwatchItem) {
        o.select()
        selectedObject.set(o)
    }
}