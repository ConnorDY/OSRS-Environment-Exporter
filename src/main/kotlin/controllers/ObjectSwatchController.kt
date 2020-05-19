package controllers

import com.jfoenix.controls.JFXMasonryPane
import javafx.fxml.FXML
import javafx.scene.control.Button

class ObjectSwatchController {
//    @Inject
//    private val model: ObjectSwatchModel? = null

    @FXML
    private val swatchPane: JFXMasonryPane? = null

    @FXML
    private val btnLaunchPicker: Button? = null

    @FXML
    private fun initialize() {
//        btnLaunchPicker!!.onAction = EventHandler { e: ActionEvent? ->
//            try {
//                val fxmlLoader = FXMLLoader(javaClass.getResource("/views/object-picker.fxml"))
//                fxmlLoader.setControllerFactory(BasicModule.injector::getInstance)
//                val root1 = fxmlLoader.load<Parent>()
//                val stage = Stage()
//                stage.initModality(Modality.NONE)
//                stage.title = "Object Picker"
//                stage.scene = Scene(root1)
//                stage.show()
//            } catch (ex: IOException) {
//                ex.printStackTrace()
//            }
//        }
//        model.getObjectList()
//            .addListener(ListChangeListener<ObjectSwatchItem?> { c: ListChangeListener.Change<out ObjectSwatchItem?> ->
//                while (c.next()) {
//                    if (c.wasAdded()) {
//                        c.addedSubList.forEach { o: ObjectSwatchItem? ->
//                            val item: Region = o.toView()
//                            item.onMouseClicked = EventHandler { e: MouseEvent? ->
//                                model.getObjectList().forEach(ObjectSwatchItem::deselect)
//                                model.setSelectedObject(o)
//                            }
//                            item.focusedProperty()
//                                .addListener { obs: ObservableValue<out Boolean>?, oldVal: Boolean?, newVal: Boolean ->
//                                    if (newVal) {
//                                        model.getObjectList().forEach(ObjectSwatchItem::deselect)
//                                        model.setSelectedObject(o)
//                                    }
//                                    if (!newVal) {
//                                        o.deselect()
//                                    }
//                                }
//                            swatchPane!!.children.add(item)
//                        }
//                    }
//                }
//            } as ListChangeListener<in ObjectSwatchItem?>?)
    }
}