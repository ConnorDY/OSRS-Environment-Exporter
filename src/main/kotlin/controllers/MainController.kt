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
import com.fasterxml.jackson.databind.ObjectMapper
import controllers.worldRenderer.Camera
import controllers.worldRenderer.InputHandler
import controllers.worldRenderer.Renderer
import controllers.worldRenderer.SceneUploader
import controllers.worldRenderer.TextureManager
import controllers.worldRenderer.WorldRendererController
import models.Configuration
import models.DebugModel
import models.github.GitHubRelease
import models.scene.Scene
import models.scene.SceneRegionBuilder
import org.slf4j.LoggerFactory
import ui.JLinkLabel
import utils.PackageMetadata
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection
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
    private val logger = LoggerFactory.getLogger(Scene::class.java)

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
                JMenuItem("Load Custom Grid").apply {
                    mnemonic = 'G'.code
                    addActionListener(::chooseGridRegionClicked)
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

            Box.createHorizontalGlue().let(::add)
        }.let { jMenuBar = it }

        val lblFps = JLabel("FPS: Unknown")

        JToolBar().apply {
            JButton("Export").apply {
                mnemonic = 'X'.code
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

        // load initial scene
        scene.loadRadius(
            configuration.getProp("initial-region-id").toIntOrNull() ?: 15256,
            configuration.getProp("initial-radius").toIntOrNull() ?: 1,
        )

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
        } catch (e: RuntimeException) {
            if (!e.toString().contains("GLException")) { throw e }
            e.printStackTrace()

            // Hack: Remove the renderer and retry.
            // Obviously this leaves the user with no visibility, but exiting is worse.
            worldRendererController.isVisible = false
            pack()
        }

        val checkForUpdatesEnabled = configuration.getProp(SettingsController.CHECK_FOR_UPDATES_PROP).toBooleanStrictOrNull() ?: true
        if (checkForUpdatesEnabled) checkForUpdates()
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
        RegionChooserController(
            this, "Region Chooser"
        ) { regionId, radius ->
            scene.loadRadius(regionId, radius)
        }.display()
    }

    private fun locationSearchClicked(event: ActionEvent) {
        LocationSearchController(this, "Location Search", scene).display()
    }

    private fun chooseGridRegionClicked(event: ActionEvent) {
        GridRegionChooserController(this, "Load Custom Grid") { regionIds ->
            scene.loadRegions(regionIds)
        }.display()
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

    private fun checkForUpdates() {
        val now = System.currentTimeMillis() / 1000L
        val lastChecked = configuration.getProp(SettingsController.LAST_CHECKED_FOR_UPDATES_PROP).toLongOrNull()

        // see if it's been an hour since the last check
        if (lastChecked != null && (now - lastChecked) < 3600) {
            logger.info("Checked for updates within the past hour. Skipping check...")
            return
        }

        try {
            val url = URL("https://api.github.com/repos/ConnorDY/OSRS-Environment-Exporter/releases")

            // create request
            val conn = url.openConnection() as HttpsURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github.manifold-preview+json")
            conn.useCaches = false
            conn.doOutput = true

            // capture the response
            val inputStream = conn.inputStream
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            val buffer = StringBuilder()

            var line: String?
            while (true) {
                line = bufferedReader.readLine()
                if (line != null) buffer.append(line)
                else break
            }
            val data = buffer.toString()

            // map the response to an array of GitHubRelease
            val objectMapper = ObjectMapper()
            val releases = objectMapper.readValue<Array<GitHubRelease>>(
                data,
                objectMapper.typeFactory.constructArrayType(GitHubRelease::class.java)
            )

            // determine if there is a newer version available
            val currVersion = PackageMetadata.VERSION.split(".").map { it.toInt() }
            var newerVersionUrl: String? = null

            for (release in releases) {
                val version = release.tagName.split(".").map { it.toInt() }

                if (isVersionNewer(version, currVersion)) {
                    newerVersionUrl = release.htmlURL
                    break
                }
            }

            if (newerVersionUrl != null) {
                jMenuBar.add(
                    JLinkLabel(
                        newerVersionUrl,
                        "Update available! Click here to download."
                    )
                )
                jMenuBar.add(Box.createHorizontalStrut(4))
                pack()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        configuration.saveProp(SettingsController.LAST_CHECKED_FOR_UPDATES_PROP, now.toString())
    }

    private fun isVersionNewer(verA: List<Int>, verB: List<Int>): Boolean {
        for ((ver1, ver2) in verA zip verB) {
            if (ver1 != ver2) {
                return ver1 > ver2
            }
        }

        return false
    }
}
