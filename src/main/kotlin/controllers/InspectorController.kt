package controllers

import cache.LocationType
import com.google.inject.Inject
import controllers.worldRenderer.entities.StaticObject
import javafx.fxml.FXML
import javafx.scene.control.TextArea
import models.HoverModel

class InspectorController @Inject constructor(
    private val hoverModel: HoverModel
) {

    @FXML
    private lateinit var txtArea: TextArea

    @FXML
    private fun initialize() {
        // FIXME: Something is causing the textArea layout bounds to nullpointer sometimes
//        txtArea.isWrapText = true
//        hoverModel.hovered.addListener { observable, oldValue, newValue ->
//            if (newValue == null) {
//                return@addListener
//            }
//            txtArea.text = newValue.toString()
//
//            when (newValue.type) {
//                LocationType.INTERACTABLE -> {
//                    if (newValue.sceneTile.locations.size > 0) {
//                        val obj = newValue.sceneTile.locations[0]
//                        txtArea.text += obj.toString()
//                    }
//
////                    val obj = newValue.sceneTile.gameObjects.first { (it.entity as StaticObject). }
//                }
//            }
//        }
    }
}