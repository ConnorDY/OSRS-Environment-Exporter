package controllers

import JfxApplication.Companion.injector
import cache.CacheLibraryProvider
import cache.XteaManagerProvider
import com.google.inject.Inject
import javafx.application.Platform
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import javafx.util.Callback
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import models.Configuration
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.jsoup.Jsoup
import java.io.*
import java.lang.Exception
import java.net.URL
import java.util.*


class CacheChooserController @Inject constructor(
    private val cacheLibraryProvider: CacheLibraryProvider,
    private val xteaManagerProvider: XteaManagerProvider,
    private val configuration: Configuration
) {

    @FXML
    private lateinit var listCaches: ListView<String>
    private var entries: ObservableList<String> = FXCollections.observableArrayList()

    @FXML
    private lateinit var btnChooseDirectory: Button

    @FXML
    private lateinit var txtCacheLocation: TextField

    @FXML
    private lateinit var txtFilter: TextField

    @FXML
    private lateinit var btnDownload: Button

    @FXML
    private lateinit var lblStatusText: Label
    @FXML
    private lateinit var lblErrorText: Label

    private var selectedCache: String? = null

    @FXML
    private lateinit var btnLaunch: Button

    @FXML
    private fun initialize() {
        val doc = Jsoup.connect(RUNESTATS_URL).get()
        entries.addAll(doc.select("a")
            .map { col -> col.attr("href") }
            .filter { it.length > 10 } // get rid of ../ and ./types
            .reversed()
        )
        val filterableEntries = FilteredList(entries)
        listCaches.items = filterableEntries

        listCaches.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            if (newValue != null) {
                selectedCache = newValue
                btnDownload.isDisable = false
            }
        }

        txtFilter.textProperty()
            .addListener { _: ObservableValue<out String>?, _: String?, newVal: String ->
                filterableEntries.setPredicate { obj ->
                    newVal.isEmpty() || obj.toString().toLowerCase().contains(newVal.toLowerCase())
                }
            }

        btnDownload.setOnAction {
            btnDownload.isDisable = true
            downloadCache(selectedCache!!)
        }

        txtCacheLocation.textProperty().addListener { _, _, newVal ->
            if (newVal != "") {
                btnLaunch.isDisable = false
                lblErrorText.isVisible = false
                try {
                    cacheLibraryProvider.setLibraryLocation("${txtCacheLocation.text}/cache")
                    xteaManagerProvider.setXteaLocation(txtCacheLocation.text)
                } catch (e: Exception) {
                    lblErrorText.text = e.message
                    lblErrorText.isVisible = true
                    btnLaunch.isDisable = true
                }
            } else {
                btnLaunch.isDisable = true
            }
        }

        btnChooseDirectory.setOnAction {
            val directoryChooser = DirectoryChooser()
            directoryChooser.initialDirectory = File("./caches")
            val f = directoryChooser.showDialog(null) ?: return@setOnAction
            txtCacheLocation.text = f.absolutePath
        }

        btnLaunch.setOnAction {
            lblStatusText.isVisible = true
            lblStatusText.text = "Launching map editor please wait.."

            GlobalScope.launch {
                configuration.saveProp("last-cache-dir", txtCacheLocation.text)
                btnLaunch.isDisable = true
                // load and open main scene
                val fxmlLoader = FXMLLoader()
                fxmlLoader.controllerFactory = Callback { type: Class<*>? ->
                    injector.getInstance(type)
                }
                fxmlLoader.location = javaClass.getResource("/views/main.fxml")
                val root = fxmlLoader.load<Parent>()
                val controller = fxmlLoader.getController<MainController>()
                val jfxScene = Scene(root)
                Platform.runLater {
                    val stage = Stage()
                    stage.title = "Taylor's Map Editor"
                    stage.scene = jfxScene
                    stage.sizeToScene()
                    stage.x = -10.0
                    stage.y = 0.0
                    stage.setOnShown { controller.forceRefresh() }
                    stage.show()

                    (btnLaunch.scene.window as Stage).close()
                }
            }
        }

        txtCacheLocation.text = configuration.getProp("last-cache-dir")

        if (configuration.getProp("debug") == "true") {
            btnLaunch.fire()
        }
    }

    private fun downloadCache(cacheName: String) {
        lblStatusText.isVisible = true
        lblStatusText.text = "Downloading cache $cacheName please wait.."
        txtCacheLocation.text = ""
        val destFolder = File("caches/${cacheName.removeSuffix(".tar.gz")}")

        GlobalScope.launch {
            try {
                val conn = URL("$RUNESTATS_URL/$cacheName").openConnection()
                conn.addRequestProperty("User-Agent", "taylors-map-editor")
                BufferedInputStream(conn.getInputStream()).use { inputStream ->
                    val tarIn = TarArchiveInputStream(GzipCompressorInputStream(inputStream))
                    var tarEntry: TarArchiveEntry? = tarIn.nextTarEntry
                    while (tarEntry != null) {
                        val dest = File(destFolder, tarEntry.name)
                        if (tarEntry.isDirectory) {
                            dest.mkdirs()
                        } else {
                            dest.createNewFile()
                            val btoRead = ByteArray(1024)
                            val bout = BufferedOutputStream(FileOutputStream(dest))
                            var len: Int

                            while (tarIn.read(btoRead).also { len = it } != -1) {
                                bout.write(btoRead, 0, len)
                            }

                            bout.close()
                        }
                        tarEntry = tarIn.nextTarEntry
                    }
                    tarIn.close()
                    lblStatusText.isVisible = false
                    txtCacheLocation.text = destFolder.absolutePath
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val RUNESTATS_URL = "https://archive.runestats.com/osrs"
    }
}