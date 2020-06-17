package controllers

import cache.LocationType
import cache.definitions.ModelDefinition
import cache.definitions.ObjectDefinition
import cache.definitions.converters.ObjectToModelConverter
import cache.loaders.ObjectLoader
import cache.loaders.SpriteLoader
import cache.loaders.TextureLoader
import controllers.worldRenderer.Renderer
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.*
import javafx.scene.control.*
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.shape.MeshView
import javafx.scene.transform.Rotate
import models.ObjectsModel
import models.scene.SceneObject
import utils.JavaFxHelpers
import javax.inject.Inject

class ObjectPickerController @Inject constructor(
    private val objectLoader: ObjectLoader,
    private val textureLoader: TextureLoader,
    private val spriteLoader: SpriteLoader,
    private val objectToModelConverter: ObjectToModelConverter,
    private val objectsModel: ObjectsModel
) {

    private lateinit var renderer: Renderer
    fun setRenderer(renderer: Renderer) {
        this.renderer = renderer
    }

    @FXML
    private lateinit var stackPane: StackPane

    @FXML
    private lateinit var listView: ListView<ObjectListItem>
    private var entries: ObservableList<ObjectListItem> = FXCollections.observableArrayList()

    @FXML
    private lateinit var searchBox: TextField
    private var selectedObject: ObjectDefinition? = null
    private var selectedModelType: LocationType? = null

    private lateinit var subScene: SubScene

    @FXML
    private fun initialize() {
        val g = Group()
        val p = StackPane(g)
        val camera: Camera = PerspectiveCamera(true)
        camera.nearClip = 1.0
        camera.farClip = 1000000.0
        // in order for depth to work, camera and model have to stay in positive Z
        // by setting them to a very large +z at the start it gives lots of room to zoom in and out
        g.translateZ = 10000.0
        camera.translateZ = 9000.0
        g.translateY = -10.0 // offset from controls on bottom
        p.children.add(camera)

        subScene = SubScene(p, 200.0, 400.0, true, SceneAntialiasing.BALANCED)
        subScene.camera = camera
        stackPane.children.add(subScene)
        subScene.heightProperty().bind(stackPane.heightProperty())
        subScene.widthProperty().bind(stackPane.widthProperty())
        initMouseControl(g, stackPane, camera)

        entries.addAll(objectLoader.getAll().values.map { ObjectListItem(it.id, String.format("%s (%d)", it.name, it.id)) })
        val filterableEntries = FilteredList(entries)
        listView.items = filterableEntries

        searchBox.textProperty()
            .addListener { _: ObservableValue<out String>?, _: String?, newVal: String ->
                filterableEntries.setPredicate { obj ->
                    newVal.isEmpty() || obj.toString().toLowerCase().contains(newVal.toLowerCase())
                }
            }

        val typeToggleGroup = ToggleGroup()
        listView.selectionModel.selectedItemProperty()
            .addListener { _: ObservableValue<out ObjectListItem?>?, oldVal: ObjectListItem?, newVal: ObjectListItem? ->
                if (newVal == null || newVal === oldVal) {
                    return@addListener
                }
                val objectDefinition = objectLoader.get(newVal.id) ?: return@addListener
//            paneTypeList.children.clear()
                val modelDefinitionMap: Map<Int, ModelDefinition?> =
                    objectToModelConverter.toModelTypesMap(objectDefinition)
                if (modelDefinitionMap.isEmpty()) {
                    return@addListener
                }
                var first = true
                for (t in modelDefinitionMap.keys) {
                    if (modelDefinitionMap[t] == null) {
                        continue
                    }
                    val b = ToggleButton()
                    b.text = LocationType.fromId(t).toString().toLowerCase().replace("_", " ").capitalize()
                    b.toggleGroup = typeToggleGroup
//                paneTypeList.children.add(b)
                    b.onAction = EventHandler {
                        g.children.clear()
                        val mv: Array<MeshView?> =
                            JavaFxHelpers.modelToMeshViews(modelDefinitionMap[t]!!, textureLoader, spriteLoader)
                        g.children.addAll(*mv)
                        selectedModelType = LocationType.fromId(t)
                    }
                    if (first) {
                        b.fire()
                        first = false
                    }
                }
                objectsModel.heldObject.set(
                    SceneObject(
                        objectDefinition = objectDefinition,
                        type = 10,
                        orientation = 0
                    )
                )
                selectedObject = objectDefinition
            }
    }

    //Tracks drag starting point for x and y
    private var anchorX = 0.0
    private var anchorY = 0.0

    //Keep track of current angle for x and y
    private var anchorAngleX = 0.0
    private var anchorAngleY = 0.0

    //We will update these after drag. Using JavaFX property to bind with object
    private val angleX: DoubleProperty = SimpleDoubleProperty(0.0)
    private val angleY: DoubleProperty = SimpleDoubleProperty(0.0)
    private fun initMouseControl(group: Group, scene: Pane?, camera: Camera) {
        var xRotate: Rotate
        var yRotate: Rotate
        group.transforms.addAll(
            Rotate(0.0, Rotate.X_AXIS).also {
                xRotate = it
            },
            Rotate(0.0, Rotate.Y_AXIS).also {
                yRotate = it
            }
        )
        xRotate.angleProperty().bind(angleX)
        yRotate.angleProperty().bind(angleY)
        scene!!.onMousePressed = EventHandler { event: MouseEvent ->
            anchorX = event.sceneX
            anchorY = event.sceneY
            anchorAngleX = angleX.get()
            anchorAngleY = angleY.get()
        }
        scene.onMouseDragged = EventHandler { event: MouseEvent ->
            angleX.set(anchorAngleX - (anchorY - event.sceneY))
            angleY.set(anchorAngleY + anchorX - event.sceneX)
        }

        //Attach a scroll listener
        scene.addEventHandler(
            ScrollEvent.SCROLL
        ) { event: ScrollEvent ->
            //Get how much scroll was done in Y axis.
            val delta = event.deltaY
            //Add it to the Z-axis location.
            camera.translateZ = camera.translateZ + delta
        }
    }

    class ObjectListItem(val id: Int, private val text: String) {
        override fun toString(): String {
            return text
        }
    }
}