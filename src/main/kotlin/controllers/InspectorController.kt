package controllers

import cache.LocationType
import cache.loaders.RegionLoader
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

    fun onClose() {
        animationTimer.stop()
    }

    private lateinit var animationTimer: AnimationTimer
    @FXML
    private fun initialize() {
        txtArea.isWrapText = true
        animationTimer = object: AnimationTimer() {
            override fun handle(now: Long) {
                val hovered = hoverModel.hovered.get() ?: return
                if (hovered == lastHover) return

                val sb = StringBuilder()
                when (hovered.type) {
                    LocationType.TILE_PAINT -> {
                        val st = hovered.sceneTile?: return
                        val t = st.tilePaint?: return
                        sb.append("TILE_PAINT:\n")
                        sb.append("  nwHeight: ${t.nwHeight}, neHeight: ${t.neHeight}, swHeight: ${t.swHeight}, seHeight: ${t.seHeight}\n")
                        sb.append("  nwColor: ${t.nwColor}, neColor: ${t.neColor}, swColor: ${t.swColor}, seColor: ${t.seColor}\n")
                        sb.append("  X: ${st.x} Y: ${st.y} cacheTile.cacheHeight: ${st.cacheTile?.cacheHeight} height: ${st.cacheTile?.height}\n")
                        sb.append("  overlay: id: ${st.overlayDefinition?.id} rgbColor: ${st.overlayDefinition?.rgbColor} texture: ${st.overlayDefinition?.texture} secondaryRgbColor: ${st.overlayDefinition?.secondaryRgbColor} hideUnderlay: ${st.overlayDefinition?.hideUnderlay}\n")
                        sb.append("  underlay: id: ${st.underlayDefinition?.id} color: ${st.underlayDefinition?.color} hue: ${st.underlayDefinition?.hue} saturation: ${st.underlayDefinition?.saturation} lightness: ${st.underlayDefinition?.lightness}\n")
                    }
                    LocationType.TILE_MODEL -> {
                        val st = hovered.sceneTile?: return
                        val t = st.tileModel?: return
                        sb.append("TILE_MODEL:\n")
                        sb.append("  nwHeight: ${t.nwHeight}, neHeight: ${t.neHeight}, swHeight: ${t.swHeight}, seHeight: ${t.seHeight}\n")
                        sb.append("  nwColor: ${t.nwColor}, neColor: ${t.neColor}, swColor: ${t.swColor}, seColor: ${t.seColor}\n")
                        sb.append("  nwColorB: ${t.nwColorB}, neColorB: ${t.neColorB}, swColorB: ${t.swColorB}, seColorB: ${t.seColorB}\n")
                        sb.append("  X: ${st.x} Y: ${st.y} cacheTile.cacheHeight: ${st.cacheTile?.cacheHeight} height: ${st.cacheTile?.height}\n")
                        sb.append("  overlayPath: ${t.overlayPath} overlayRotation: ${t.overlayRotation} overlayTexture: ${t.overlayTexture} underlayRgb: ${t.underlayRgb} overlayRgb: ${t.overlayRgb}\n");
                        sb.append("  overlay: id: ${st.overlayDefinition?.id} rgbColor: ${st.overlayDefinition?.rgbColor} texture: ${st.overlayDefinition?.texture} secondaryRgbColor: ${st.overlayDefinition?.secondaryRgbColor} hideUnderlay: ${st.overlayDefinition?.hideUnderlay}\n")
                        sb.append("  underlay: id: ${st.underlayDefinition?.id} color: ${st.underlayDefinition?.color} hue: ${st.underlayDefinition?.hue} saturation: ${st.underlayDefinition?.saturation} lightness: ${st.underlayDefinition?.lightness}\n")
                    }
                    LocationType.WALL_CORNER -> {
                        val wall = hovered.sceneTile.wall?: return
                        val entity1 = wall.entity
                        val entity2 = wall.entity2
                        sb.append("Wall Corner\n" +
                                "entity1: $entity1 \n" +
                                "model1:${entity1?.getModel()} \n" +
                                "entity2: $entity2 \n" +
                                "model2:${entity2?.getModel()}")
                    }
                    LocationType.INTERACTABLE -> {
                        sb.append("entities: \n")
                        for (i in 0 until hovered.sceneTile.gameObjects.size) {
                            sb.append("entity[$i]: ${hovered.sceneTile.gameObjects[i].entity?.height}")
                        }

                    }
                }

                sb.append("\n" + hovered.toString())

                val out = sb.toString()
                if (txtArea.text == out) return

                txtArea.text = out
                lastHover = hovered
            }
        }

        animationTimer.start()

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