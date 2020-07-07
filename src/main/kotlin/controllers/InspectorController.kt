package controllers

import cache.LocationType
import com.google.inject.Inject
import javafx.animation.AnimationTimer
import javafx.fxml.FXML
import javafx.scene.control.TextArea
import models.HoverModel

class InspectorController @Inject constructor(
    private val hoverModel: HoverModel
) {

    @FXML
    private lateinit var txtArea: TextArea

    private var lastHover: Any? = null

    @FXML
    private fun initialize() {
        txtArea.isWrapText = true
//        object : AnimationTimer() {
//            override fun handle(now: Long) {
//                val hovered = hoverModel.hovered.get()?: return
//                if (hovered == lastHover) return
//                var inspectorText = ""
//
//
//                when (hovered.type) {
//                    LocationType.TILE_PAINT -> {
//                        val tp = hovered.sceneTile.tilePaint!!
//                        inspectorText = "Tile Paint -- nwHeight: ${tp.nwHeight}, neHeight: ${tp.neHeight}, swHeight: ${tp.swHeight}, seHeight: ${tp.seHeight}"
//                    }
//                    LocationType.WALL_CORNER -> {
//                        val wall = hovered.sceneTile.wall!!
//                        val entity1 = wall.entity
//                        val entity2 = wall.entity2
//                        inspectorText = "Wall Corner\n" +
//                                "entity1: $entity1 \n" +
//                                "model1:${entity1?.getModel()} \n" +
//                                "entity2: $entity2 \n" +
//                                "model2:${entity2?.getModel()}"
//                    }
//                }
//
//                inspectorText += "\n" + hovered.toString()
//                txtArea.text = inspectorText
//                lastHover = hovered
//            }
//        }.start()

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