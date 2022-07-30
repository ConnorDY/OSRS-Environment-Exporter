package controllers

import cache.XteaManager
import cache.definitions.converters.ObjectToModelConverter
import cache.loaders.LocationsLoader
import cache.loaders.ModelLoader
import cache.loaders.ObjectLoader
import cache.loaders.OverlayLoader
import cache.loaders.RegionLoader
import cache.loaders.SpriteLoader
import cache.loaders.TextureLoader
import cache.loaders.UnderlayLoader
import com.displee.cache.CacheLibrary
import com.jogamp.opengl.GLException
import controllers.worldRenderer.Camera
import controllers.worldRenderer.InputHandler
import controllers.worldRenderer.Renderer
import controllers.worldRenderer.SceneUploader
import controllers.worldRenderer.TextureManager
import controllers.worldRenderer.WorldRendererController
import models.Configuration
import models.DebugModel
import models.scene.Scene
import models.scene.SceneRegionBuilder
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JToolBar
import javax.swing.Timer

class MainController constructor(
    title: String,
    private val configuration: Configuration,
    xteaManager: XteaManager,
    cacheLibrary: CacheLibrary
) : JFrame(title) {
    private val animationTimer: Timer
    private val worldRendererController: WorldRendererController
    val scene: Scene

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        layout = BorderLayout()
        preferredSize = Dimension(1600, 800)

        val camera = Camera()
        val debugModel = DebugModel()
        val objectToModelConverter =
            ObjectToModelConverter(ModelLoader(cacheLibrary))
        val overlayLoader = OverlayLoader(cacheLibrary)
        val regionLoader = RegionLoader(cacheLibrary)
        val textureLoader = TextureLoader(cacheLibrary)
        val underlayLoader = UnderlayLoader(cacheLibrary)
        scene = Scene(
            SceneRegionBuilder(
                regionLoader,
                LocationsLoader(cacheLibrary, xteaManager),
                ObjectLoader(cacheLibrary),
                textureLoader,
                underlayLoader,
                overlayLoader,
                objectToModelConverter
            )
        )
        worldRendererController = WorldRendererController(
            Renderer(
                camera, scene, SceneUploader(),
                InputHandler(camera, scene),
                TextureManager(
                    SpriteLoader(cacheLibrary), textureLoader
                ),
                debugModel
            ),
            configuration
        )

        JMenuBar().apply {
            JMenu("World").apply {
                mnemonic = 'W'.code
                JMenuItem("Change Region").apply {
                    mnemonic = 'R'.code
                    addActionListener(::changeRegionClicked)
                }.let(::add)
                JMenuItem("Location Search").apply {
                    mnemonic = 'S'.code
                    addActionListener(::locationSearchClicked)
                }.let(::add)
            }.let(::add)
            JMenu("Edit").apply {
                mnemonic = 'E'.code
                JMenuItem("Preferences").apply {
                    mnemonic = 'P'.code
                    addActionListener(::preferencesClicked)
                }.let(::add)
            }.let(::add)
            JMenu("Help").apply {
                mnemonic = 'H'.code
                JMenuItem("About").apply {
                    mnemonic = 'A'.code
                    addActionListener(::aboutClicked)
                }.let(::add)
            }.let(::add)
        }.let { jMenuBar = it }

        val lblFps = JLabel("FPS: Unknown")

        JToolBar().apply {
            JButton("Export").apply {
                mnemonic = 'x'.code
                addActionListener(::exportClicked)
            }.let(::add)
            JToolBar.Separator().let(::add)
            Box.createGlue().let(::add)

            JLabel("Z Layers:").let(::add)
            worldRendererController.renderer.zLevelsSelected.forEachIndexed { z, visible ->
                JCheckBox("Z$z").apply {
                    mnemonic = z + '0'.code
                    isSelected = visible
                    addActionListener { onZLevelSelected(z, isSelected) }
                }.let(::add)
            }

            Box.createGlue().let(::add)
            lblFps.let(::add)
        }.let { add(it, BorderLayout.NORTH) }

        worldRendererController.loadScene()
        add(worldRendererController, BorderLayout.CENTER)

        animationTimer = Timer(500) {
            lblFps.text = "FPS: ${debugModel.fps.get()}"
        }

        addWindowListener(object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent?) {
                animationTimer.stop()
                worldRendererController.stopRenderer()
            }
        })

        try {
            pack()
        } catch (e: GLException) {
            e.printStackTrace()

            // Hack: Remove the renderer and retry.
            // Obviously this leaves the user with no visibility, but exiting is worse.
            worldRendererController.isVisible = false
            pack()
        }
    }

    override fun setVisible(visible: Boolean) {
        super.setVisible(visible)
        if (visible) {
            animationTimer.start()
        } else {
            animationTimer.stop()
        }
    }

    private fun changeRegionClicked(event: ActionEvent) {
        RegionChooserController(this, "Region Chooser") { regionId, radius ->
            scene.load(regionId, radius)
        }.display()
    }

    private fun locationSearchClicked(event: ActionEvent) {
        LocationSearchController(this, "Location Search", scene).display()
    }
    private fun preferencesClicked(event: ActionEvent) {
        SettingsController(this, "Preferences", worldRendererController.renderer, configuration).display()
    }

    private fun aboutClicked(event: ActionEvent) {
        AboutController(this, "About").display()
    }

    private fun JDialog.display() {
        setLocationRelativeTo(this@MainController)
        isVisible = true
    }

    private fun exportClicked(event: ActionEvent) {
        worldRendererController.renderer.exportScene()
        JOptionPane.showMessageDialog(
            this,
            "Exported as glTF.",
            "Export Completed",
            JOptionPane.INFORMATION_MESSAGE or JOptionPane.OK_OPTION
        )
    }

    private fun onZLevelSelected(z: Int, isSelected: Boolean) {
        worldRendererController.renderer.zLevelsSelected[z] = isSelected
        worldRendererController.renderer.isSceneUploadRequired = true
    }
}
