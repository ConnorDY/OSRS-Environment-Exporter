package controllers.topdownMap

import com.google.inject.Inject
import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.value.ObservableValue
import javafx.fxml.FXML
import javafx.scene.control.CheckBox
import javafx.scene.control.ScrollPane
import javafx.scene.control.Slider
import javafx.scene.image.ImageView
import javafx.scene.image.PixelFormat
import javafx.scene.image.PixelWriter
import javafx.scene.image.WritableImage
import javafx.scene.input.ScrollEvent
import models.scene.REGION_SIZE
import models.scene.Scene
import models.scene.SceneTile
import java.awt.event.ActionListener
import java.util.*

class TopdownMapController @Inject constructor(
    private val scene: Scene
) {
    private val tileSize: IntegerProperty = SimpleIntegerProperty(15)

    @FXML
    private lateinit var imgMiniMap: ImageView
    private lateinit var pw: PixelWriter

    @FXML
    private lateinit var chkHover: CheckBox

    @FXML
    private lateinit var sliderZoom: Slider

    @FXML
    private lateinit var scrollPane: ScrollPane
    var sceneWidth = 0
    var sceneHeight = 0
    var canvasWidth = 0
    var canvasHeight = 0
    var mouseX = 0
    var mouseY = 0

    @FXML
    private fun initialize() {
        sliderZoom.valueProperty().bindBidirectional(tileSize)
        tileSize.addListener { _: ObservableValue<out Number?>?, _: Number?, _: Number? -> drawFull() }
        scrollPane.addEventFilter(
            ScrollEvent.ANY
        ) { e: ScrollEvent ->
            e.consume()
            if (e.deltaY > 0) {
                if (tileSize.get() < 25) {
                    tileSize.set(tileSize.get() + 1)
                }
            } else if (e.deltaY < 0) {
                if (tileSize.get() > 1) {
                    tileSize.set(tileSize.get() - 1)
                }
            }
        }
        scene.sceneChangeListeners.add(ActionListener { Thread(Runnable { drawFull() }).start() })
//        Thread(Runnable { drawFull() }).start()
    }

    private fun drawFull() {
        // return
        sceneHeight = scene.radius * REGION_SIZE
        sceneWidth = sceneHeight
        canvasHeight = sceneWidth * tileSize.get()
        canvasWidth = canvasHeight
        val img = WritableImage(canvasWidth, canvasHeight)
        pw = img.pixelWriter
        imgMiniMap.image = img
        imgMiniMap.fitHeight = img.height
        imgMiniMap.fitWidth = img.width
        imgMiniMap.toBack()
        val pixels = IntArray(canvasWidth * canvasHeight)
        for (x in 0 until sceneWidth) {
            for (y in 0 until sceneHeight) {
                val tile: SceneTile? = scene.getTile(0, x, sceneHeight - 1 - y)
                if (tile?.tilePaint != null) {
                    val argb = 0xFF shl 24 or tile.tilePaint!!.rgb
                    for (i in 0 until tileSize.get()) {
                        for (j in 0 until tileSize.get()) {
                            val xPix = x * tileSize.get() + i
                            val yPix = y * tileSize.get() + j
                            pixels[xPix + yPix * canvasWidth] = argb
                        }
                    }
                }
            }
        }
        pw.setPixels(
            0,
            0,
            canvasWidth,
            canvasHeight,
            PixelFormat.getIntArgbInstance(),
            pixels,
            0,
            canvasWidth
        )
    }

    fun drawTile(x: Int, y: Int, rgb: Int) {
        val argb = 0xFF shl 24 or rgb //argb, full alpha first bits
        val tilePixels = IntArray(tileSize.get() * tileSize.get())
        Arrays.fill(tilePixels, argb)
        val xPix = x * tileSize.get()
        val yPix = y * tileSize.get()
        pw.setPixels(
            xPix,
            yPix,
            tileSize.get(),
            tileSize.get(),
            PixelFormat.getIntArgbInstance(),
            tilePixels,
            0,
            tileSize.get()
        )
    }
}