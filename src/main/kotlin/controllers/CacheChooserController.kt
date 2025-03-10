package controllers

import AppConstants
import cache.ParamsManager
import cache.XteaManager
import com.displee.cache.CacheLibrary
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import controllers.main.MainController
import models.StartupOptions
import models.config.ConfigOptions
import models.openrs2.OpenRs2Cache
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.jsoup.Jsoup
import ui.FilteredListModel
import ui.listener.DocumentTextListener
import ui.listener.FilterTextListener
import utils.OpenRs2Api
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.io.BufferedInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.net.ssl.SSLHandshakeException
import javax.swing.GroupLayout
import javax.swing.GroupLayout.Alignment
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JScrollPane
import javax.swing.JTextField
import javax.swing.LayoutStyle.ComponentPlacement
import javax.swing.ListSelectionModel
import javax.swing.ScrollPaneConstants
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.text.JTextComponent

class CacheChooserController(
    title: String,
    private val configOptions: ConfigOptions,
    private val startupOptions: StartupOptions,
) : JFrame(title) {
    var cacheLibrary: CacheLibrary? = null
    var xteaManager: XteaManager? = null
    private val paramsManager: ParamsManager = ParamsManager()

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        preferredSize = Dimension(750, 400)

        val groups = GroupLayout(contentPane)
        layout = groups

        val cacheListModel = FilteredListModel<String> { it }
        val txtFilter = JTextField().apply {
            document.addDocumentListener(
                FilterTextListener(
                    this,
                    cacheListModel
                )
            )
            maximumSize = Dimension(maximumSize.width, preferredSize.height)
        }
        val btnDownload = JButton("Download").apply {
            mnemonic = 'O'.code
            isEnabled = false
        }
        val listCaches = JList(cacheListModel).apply {
            isVisible = false
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            addListSelectionListener {
                btnDownload.isEnabled = selectedIndex != -1
            }
        }
        val txtCacheLocation = JTextField().apply {
            isEnabled = false
            maximumSize = Dimension(maximumSize.width, preferredSize.height)
        }
        val lblStatusText = JLabel()
        val lblErrorText = JLabel().apply {
            foreground = Color.RED
        }
        btnDownload.addActionListener {
            btnDownload.isEnabled = false
            listCaches.selectedValue?.let {
                lblStatusText.text = "Downloading cache $it, please wait.."
                txtCacheLocation.text = ""

                downloadCache(it, { path ->
                    lblStatusText.text = ""
                    txtCacheLocation.text = path
                    btnDownload.isEnabled = true
                }, { err ->
                    lblStatusText.text = ""
                    lblErrorText.text = "Failed to download cache: $err"
                    btnDownload.isEnabled = true
                })
            }
        }
        val scrollErrorText = JScrollPane(lblErrorText)

        lblErrorText.text = "Test Error"
        scrollErrorText.maximumSize = Dimension(
            Int.MAX_VALUE,
            lblErrorText.maximumSize.height + scrollErrorText.horizontalScrollBar.maximumSize.height
        )
        lblErrorText.text = ""

        scrollErrorText.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
        val listCachesPlaceholder =
            JLabel("No downloadable caches found.", SwingConstants.CENTER)
        val btnLaunch = JButton("Launch").apply {
            mnemonic = 'L'.code
            addActionListener {
                launch(lblStatusText, txtCacheLocation, this)
            }
        }

        txtCacheLocation.document.addDocumentListener(
            DocumentTextListener {
                onCacheChooserUpdate(
                    txtCacheLocation,
                    btnLaunch,
                    lblErrorText
                )
            }
        )

        val lblRuneStats = JLabel("Caches available from RuneStats")
        val lblFilter = JLabel("Filter:").apply {
            displayedMnemonic = 'F'.code
            labelFor = txtFilter
        }
        val listCachesPane = JScrollPane(listCaches)

        val lblCacheDirectory = JLabel("Cache Directory:")
        val btnBrowse = JButton("Browse").apply {
            mnemonic = 'B'.code
            addActionListener {
                val initDir = File(AppConstants.CACHES_DIRECTORY)
                initDir.mkdirs()
                JFileChooser(initDir).apply {
                    fileSelectionMode =
                        JFileChooser.DIRECTORIES_ONLY
                    showOpenDialog(this@CacheChooserController)
                    selectedFile?.absolutePath?.let {
                        txtCacheLocation.text = it
                    }
                }
            }
        }

        groups.setHorizontalGroup(
            groups.createSequentialGroup()
                .addGroup(
                    groups.createParallelGroup()
                        .addComponent(lblRuneStats)
                        .addGroup(
                            groups.createSequentialGroup()
                                .addComponent(lblFilter)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(txtFilter)
                        )
                        .addComponent(listCachesPane)
                        .addComponent(
                            btnDownload,
                            0,
                            GroupLayout.PREFERRED_SIZE,
                            Int.MAX_VALUE
                        )
                )
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(
                    groups.createParallelGroup()
                        .addComponent(lblCacheDirectory)
                        .addGroup(
                            groups.createSequentialGroup()
                                .addComponent(txtCacheLocation)
                                .addComponent(btnBrowse)
                        )
                        .addComponent(scrollErrorText)
                        .addComponent(btnLaunch, Alignment.CENTER)
                        .addComponent(lblStatusText, Alignment.CENTER)
                )
        )

        groups.setVerticalGroup(
            groups.createParallelGroup()
                .addGroup(
                    groups.createSequentialGroup()
                        .addComponent(lblRuneStats)
                        .addGroup(
                            groups.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblFilter)
                                .addComponent(txtFilter)
                        )
                        .addComponent(listCachesPane)
                        .addComponent(btnDownload)
                )
                .addGroup(
                    groups.createSequentialGroup()
                        .addGap(0, 0, Int.MAX_VALUE)
                        .addComponent(lblCacheDirectory)
                        .addGroup(
                            groups.createParallelGroup(Alignment.BASELINE)
                                .addComponent(txtCacheLocation)
                                .addComponent(btnBrowse)
                        )
                        .addComponent(scrollErrorText)
                        .addComponent(btnLaunch)
                        .addComponent(lblStatusText)
                        .addGap(0, 0, Int.MAX_VALUE)
                )
        )

        populateCachesList(cacheListModel, listCaches, listCachesPlaceholder)
        txtCacheLocation.text = startupOptions.cacheDir
        if (txtCacheLocation.text.isEmpty()) {
            btnLaunch.isEnabled = false
        }

        if (configOptions.debug.value.get()) {
            launch(lblStatusText, txtCacheLocation, btnLaunch)
        }

        pack()

        rootPane.defaultButton = btnLaunch
        btnLaunch.requestFocus()
    }

    private fun launch(
        lblStatusText: JLabel,
        txtCacheLocation: JTextField,
        btnLaunch: JButton
    ) {
        val cacheLibrary = cacheLibrary ?: return
        val xteaManager = xteaManager ?: return
        val paramsManager = paramsManager

        btnLaunch.isEnabled = false
        lblStatusText.text =
            "Launching map editor... Please wait... (this may take a while)"

        Thread {
            configOptions.lastCacheDir.value.set(txtCacheLocation.text)
            configOptions.save()
            // load and open main scene
            SwingUtilities.invokeLater {
                MainController(
                    "OSRS Environment Exporter",
                    configOptions,
                    startupOptions,
                    xteaManager,
                    cacheLibrary,
                    paramsManager
                ).isVisible = true
                dispose()
            }
        }.start()
    }

    private fun populateCachesList(
        cacheListModel: FilteredListModel<String>,
        cacheList: Component,
        listCachesPlaceholder: JLabel
    ) {
        try {
            val doc = Jsoup.connect(RUNESTATS_URL).get()
            cacheListModel.backingList = doc.select("a")
                .map { col -> col.attr("href") }
                .filter { it.length > 10 } // get rid of ../ and ./types
                .reversed()
            cacheList.isVisible = true
            listCachesPlaceholder.isVisible = false
        } catch (e: Exception) {
            e.printStackTrace()
            listCachesPlaceholder.text += "\n\n${e.message}"
            if (e is SSLHandshakeException) {
                listCachesPlaceholder.text += "\n\nSSLHandshakeException is a known bug with certain Java versions, try updating."
            }
        }
    }

    private fun downloadCache(
        cacheName: String,
        onComplete: (String) -> Unit,
        onFailure: (IOException) -> Unit,
    ) {
        val destFolder = File("${AppConstants.CACHES_DIRECTORY}/${cacheName.removeSuffix(".tar.gz")}")

        Thread {
            try {
                val conn = URL("$RUNESTATS_URL/$cacheName").openConnection()
                conn.addRequestProperty("User-Agent", "osrs-environment-exporter")
                BufferedInputStream(conn.getInputStream()).use { inputStream ->
                    val tarIn = TarArchiveInputStream(
                        GzipCompressorInputStream(inputStream)
                    )
                    var tarEntry: TarArchiveEntry? = tarIn.nextTarEntry
                    while (tarEntry != null) {
                        val dest = File(destFolder, tarEntry.name)
                        if (tarEntry.isDirectory) {
                            dest.mkdirs()
                        } else {
                            Files.copy(tarIn, dest.toPath())
                        }
                        tarEntry = tarIn.nextTarEntry
                    }
                    tarIn.close()

                    SwingUtilities.invokeLater {
                        onComplete(destFolder.absolutePath)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                SwingUtilities.invokeLater {
                    onFailure(e)
                }
            }
        }.start()
    }

    private fun onCacheChooserUpdate(
        txtCacheLocation: JTextComponent,
        btnLaunch: Component,
        lblErrorText: JLabel
    ) {
        if (txtCacheLocation.text.isEmpty()) {
            btnLaunch.isEnabled = false
            return
        }

        btnLaunch.isEnabled = true
        lblErrorText.text = ""
        cacheLibrary = try {
            CacheLibrary("${txtCacheLocation.text}/cache")
        } catch (e: Exception) {
            lblErrorText.text = defaultErrorText(e)
            btnLaunch.isEnabled = false
            return
        }

        xteaManager = try {
            XteaManager(txtCacheLocation.text)
        } catch (e: Exception) {
            if(e is JsonMappingException || e is JsonProcessingException) {
                lblErrorText.text = "Bad cache: Could not decode xteas file: ${e.message}"
                return
            }

            if (e is FileNotFoundException) {
                val msg = e.message ?: "Unknown file"
                if (msg.contains("xteas.json")) {
                    println("cache decryption keys not found as part of installed cache. Searching archive.openrs2.org")
                    val success = tryLocateCacheKeys(txtCacheLocation.text)
                    if(!success) {
                        defaultErrorText(e)
                    }
                    return
                }
            }

            defaultErrorText(e)
            btnLaunch.isEnabled = false
            return
        }

        try {
            paramsManager.loadFromPath(txtCacheLocation.text)
        } catch (e: Exception) {
            lblErrorText.text = defaultErrorText(e)
            btnLaunch.isEnabled = false
            return
        }
    }

     /**
      * Fetches the list of all available caches from OpenRS2 archive and filters for a cache which matches the
      *  date on the user's selected cache.
      *
      * @return A list of OpenRs2Cache objects containing information about available caches
      * @throws IOException If a network error occurs
      * @throws HttpException If the server returns a non-2xx status code
      */
    private fun tryLocateCacheKeys(cacheLocation: String): Boolean {
        val date = parseDateFromCachePath(cacheLocation) ?: return false

        println("attempting to locate cache decryption keys for cache: $cacheLocation")

        val openRsApi = OpenRs2Api()
        val caches = openRsApi.getCaches()

        for (cache in caches) {
            val instant = cache.timestamp?.let { Instant.parse(it) }
            val localDate = instant?.atZone(ZoneId.of("UTC"))?.toLocalDate()
            localDate?.format(DateTimeFormatter.ISO_LOCAL_DATE)

            if(localDate == date) {
                println("Found openrs2 cache matching date: $date with id: ${cache.id}, fetching keys...")
                val keys = openRsApi.getCacheKeysById(cache.id.toString())

                val directory = Paths.get(cacheLocation)
                Files.createDirectories(directory)

                val filePath = directory.resolve("xteas.json")
                Files.newBufferedWriter(
                    filePath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
                ).use { writer ->
                    ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(writer, keys)
                }
                return true
            }
        }

        return false
    }

    /**
     * Parses a date from a cache directory path in the format "{path}/2025-03-05-rev229".
     * Extracts and parses the date portion (2025-03-05).
     *
     * @param cachePath The path string containing the date to parse
     * @return The LocalDate object represented by the date in the path, or null if parsing fails
     */
    private fun parseDateFromCachePath(cachePath: String): LocalDate? {
        try {
            val fileName = Paths.get(cachePath).fileName.toString()

            val datePattern = """(\d{4}-\d{2}-\d{2})-rev\d+""".toRegex()
            val matchResult = datePattern.find(fileName)

            val dateStr = matchResult?.groupValues?.get(1) ?: return null
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            println("failed to parse date from cache: ${e.message}")
            return null
        }
    }

    private fun defaultErrorText(e: Exception) = when (e) {
        is FileNotFoundException -> "Bad cache: Missing required file: ${e.message}"
        else -> {
            e.printStackTrace()
            e.message
        }
    }

    companion object {
        private const val RUNESTATS_URL = "https://archive.runestats.com/osrs"
        private const val OPENRS2_URL = "https://archive.openrs2.org"
    }
}
