package controllers.main

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
import controllers.AboutController
import controllers.DebugOptionsController
import controllers.GridRegionChooserController
import controllers.LocationSearchController
import controllers.RegionChooserController
import controllers.SettingsController
import controllers.worldRenderer.Camera
import controllers.worldRenderer.Renderer
import controllers.worldRenderer.SceneExporter
import controllers.worldRenderer.SceneUploader
import controllers.worldRenderer.TextureManager
import controllers.worldRenderer.WorldRendererController
import models.DebugOptionsModel
import models.FrameRateModel
import models.config.ConfigOptions
import models.github.GitHubRelease
import models.scene.Scene
import models.scene.SceneRegionBuilder
import org.slf4j.LoggerFactory
import ui.CancelledException
import ui.JLinkLabel
import utils.PackageMetadata
import java.awt.BorderLayout
import java.awt.Component
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
import javax.swing.SwingUtilities
import javax.swing.Timer

class MainController constructor(
    title: String,
    private val configOptions: ConfigOptions,
    xteaManager: XteaManager,
    cacheLibrary: CacheLibrary
) : JFrame(title) {
    private val animationTimer: Timer
    private val worldRendererController: WorldRendererController
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val debugOptions = DebugOptionsModel()
    private val exporter: SceneExporter
    private val frameRateModel = FrameRateModel(configOptions.powerSavingMode.value)
    private val btnExport: JButton

    val scene: Scene

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        layout = BorderLayout()
        preferredSize = Dimension(1600, 800)

        val camera = Camera()
        val objectToModelConverter =
            ObjectToModelConverter(ModelLoader(cacheLibrary), debugOptions)
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
            ),
            debugOptions,
        )
        val textureManager = TextureManager(SpriteLoader(cacheLibrary), textureLoader)
        exporter = SceneExporter(textureManager, debugOptions)
        val sceneUploader = SceneUploader(debugOptions)
        val renderer = Renderer(
            camera, scene, sceneUploader,
            textureManager,
            configOptions,
            frameRateModel,
            debugOptions,
        )
        worldRendererController = WorldRendererController(renderer)

        SceneLoadProgressDialogSpawner(this).attach(scene, sceneUploader, renderer)
        SceneExportProgressDialogSpawner(this).attach(exporter)

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
        }.let { jMenuBar = it }

        val lblFps = JLabel("FPS: Unknown")

        btnExport = JButton("Export").apply {
            mnemonic = 'X'.code
            addActionListener(::exportClicked)
        }
        JToolBar().apply {
            btnExport.let(::add)
            JToolBar.Separator().let(::add)
            Box.createGlue().let(::add)

            JLabel("Z Layers:").let(::add)
            debugOptions.zLevelsSelected.forEachIndexed { z, visible ->
                JCheckBox("Z$z").apply {
                    mnemonic = z + '0'.code
                    isSelected = visible.get()
                    visible.addListener { isSelected = it }
                    addActionListener { visible.set(isSelected) }
                }.let(::add)
            }

            Box.createRigidArea(Dimension(8, 8)).showInDebugMode().let(::add)

            JButton("Debug").showInDebugMode().apply {
                mnemonic = 'D'.code
                addActionListener { DebugOptionsController(this@MainController, debugOptions).display() }
            }.let(::add)

            Box.createGlue().let(::add)
            lblFps.let(::add)
        }.let { add(it, BorderLayout.NORTH) }

        // load initial scene
        scene.loadRadius(
            configOptions.initialRegionId.value.get(),
            configOptions.initialRadius.value.get()
        )

        add(worldRendererController, BorderLayout.CENTER)

        var lastFrameCount = frameRateModel.frameCount
        var lastFrameCheck = System.nanoTime()
        animationTimer = Timer(500) {
            val time = System.nanoTime()
            val frameCount = frameRateModel.frameCount
            val fps = (frameCount - lastFrameCount) * 1_000_000_000.0 / (time - lastFrameCheck)
            lastFrameCount = frameCount
            lastFrameCheck = time
            lblFps.text = String.format("FPS: %.0f", fps)
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

        val checkForUpdatesEnabled = configOptions.checkForUpdates.value.get()
        if (checkForUpdatesEnabled) checkForUpdates()
    }

    private fun <T : Component> T.showInDebugMode(): T {
        isVisible = configOptions.debug.value.get()
        configOptions.debug.value.addListener {
            isVisible = it
        }
        return this
    }

    override fun setVisible(visible: Boolean) {
        super.setVisible(visible)
        if (visible) {
            animationTimer.start()
        } else {
            animationTimer.stop()
        }
    }

    override fun dispose() {
        super.dispose()
        animationTimer.stop()
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
        SettingsController(this, "Preferences", configOptions).display()
    }

    private fun aboutClicked(event: ActionEvent) {
        AboutController(this, "About").display()
    }

    private fun JDialog.display() {
        setLocationRelativeTo(this@MainController)
        isVisible = true
    }

    private fun exportClicked(event: ActionEvent) {
        btnExport.isEnabled = false
        Thread {
            try {
                try {
                    exporter.exportSceneToFile(scene)
                } catch (_: CancelledException) {
                    return@Thread
                }

                SwingUtilities.invokeLater {
                    JOptionPane.showMessageDialog(
                        this,
                        "Exported as glTF.",
                        "Export Completed",
                        JOptionPane.INFORMATION_MESSAGE or JOptionPane.OK_OPTION
                    )
                }
            } finally {
                SwingUtilities.invokeLater {
                    btnExport.isEnabled = true
                }
            }
        }.start()
    }

    private fun checkForUpdates() {
        val rawCurrentVersion = PackageMetadata.VERSION
        if (rawCurrentVersion.isBlank()) {
            logger.warn("No version info available; cannot auto-update.")
            return
        }

        val now = System.currentTimeMillis() / 1000L
        val lastChecked = configOptions.lastCheckedForUpdates.value.get()

        // see if it's been an hour since the last check
        if ((now - lastChecked) < 3600) {
            logger.info("Checked for updates within the past hour. Skipping check...")
            return
        }

        val releaseInfo = getGitHubReleaseInfo()

        // map the response to an array of sorted GitHubRelease
        val objectMapper = ObjectMapper()
        val releases = objectMapper.readValue<Array<GitHubRelease>>(
            releaseInfo,
            objectMapper.typeFactory.constructArrayType(GitHubRelease::class.java)
        ).sortedWith { a, b ->
            val aVersion = a.tagName.split(".").map { it.toInt() }
            val bVersion = b.tagName.split(".").map { it.toInt() }
            compareVersions(aVersion, bVersion)
        }

        // determine if there is a newer version available
        val currVersion = rawCurrentVersion.split(".").map { it.toInt() }
        val newerVersionUrl = newerReleaseExists(currVersion, releases)

        if (newerVersionUrl != null) {
            // add UI elements to menu bar
            jMenuBar.add(Box.createHorizontalGlue())
            jMenuBar.add(
                JLinkLabel(
                    newerVersionUrl,
                    "Update available! Click here to download."
                )
            )
            jMenuBar.add(Box.createHorizontalStrut(4))
            pack()
        }

        configOptions.lastCheckedForUpdates.value.set(now)
        configOptions.save()
    }

    private fun getGitHubReleaseInfo(): String? {
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
            val buffer = StringBuilder()
            BufferedReader(InputStreamReader(inputStream)).use { bufferedReader ->
                var line: String?
                while (true) {
                    line = bufferedReader.readLine()
                    if (line != null) buffer.append(line)
                    else break
                }
            }
            return buffer.toString()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    private fun newerReleaseExists(currVersion: List<Int>, releases: List<GitHubRelease>): String? {
        for (release in releases) {
            val version = release.tagName.split(".").map { it.toInt() }

            if (compareVersions(version, currVersion) == 1) {
                return release.htmlURL
            }
        }

        return null
    }

    private fun compareVersions(verA: List<Int>, verB: List<Int>): Int {
        verA.zip(verB) { ver1, ver2 ->
            if (ver1 > ver2) return 1
            if (ver1 < ver2) return -1
        }

        return 0
    }
}
