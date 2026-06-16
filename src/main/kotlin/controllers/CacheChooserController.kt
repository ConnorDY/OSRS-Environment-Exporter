package controllers

import AppConstants
import cache.ParamType
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
import models.openrs2.dateString
import models.openrs2.timestampDateTime
import models.openrs2.versionString
import org.slf4j.LoggerFactory
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
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
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
    private val logger = LoggerFactory.getLogger(CacheChooserController::class.java)

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        preferredSize = Dimension(750, 400)

        val groups = GroupLayout(contentPane)
        layout = groups

        val cacheListModel = FilteredListModel<OpenRs2Cache> { cache ->
            formatCacheDisplayString(cache)
        }
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
            cellRenderer = object : javax.swing.DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>?,
                    value: Any?,
                    index: Int,
                    isSelected: Boolean,
                    cellHasFocus: Boolean
                ): java.awt.Component {
                    val result = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                    if (value != null && value is OpenRs2Cache) {
                        (result as JLabel).text = formatCacheDisplayString(value)
                    }
                    return result
                }
            }
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
            verticalAlignment = SwingConstants.TOP
            isOpaque = true
        }
        btnDownload.addActionListener {
            btnDownload.isEnabled = false
            listCaches.selectedValue?.let { cache ->
                lblStatusText.text = "Downloading cache ${cache.dateString ?: "unknown"}, please wait.."
                txtCacheLocation.text = ""

                downloadCache(cache, { path ->
                    lblStatusText.text = ""
                    txtCacheLocation.text = path
                    btnDownload.isEnabled = true
                }, { err ->
                    lblStatusText.text = ""
                    setErrorText(lblErrorText, "Failed to download cache: $err")
                    btnDownload.isEnabled = true
                })
            }
        }
        val scrollErrorText = JScrollPane(lblErrorText).apply {
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
            preferredSize = Dimension(400, 80)
            minimumSize = Dimension(300, 50)
            maximumSize = Dimension(Int.MAX_VALUE, 120)
        }
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

        val lblRuneStats = JLabel("Caches available from OpenRS2 Archive (Old School RuneScape)")
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
        cacheListModel: FilteredListModel<OpenRs2Cache>,
        cacheList: Component,
        listCachesPlaceholder: JLabel
    ) {
        Thread {
            try {
                val openRs2Api = OpenRs2Api()

                // Filter by oldschool game and sort by timestamp (newest first)
                val filteredCaches = openRs2Api.fetchCaches()
                    .filter { it.game == "oldschool" }
                    .sortedByDescending { cache ->
                        cache.timestamp?.let {
                            try {
                                it.let(Instant::parse)
                            } catch (_: DateTimeException) {
                                null
                            }
                        }
                    }

                SwingUtilities.invokeLater {
                    if (filteredCaches.isEmpty()) {
                        listCachesPlaceholder.text = "No Old School RuneScape caches found."
                        listCachesPlaceholder.isVisible = true
                        cacheList.isVisible = false
                    } else {
                        cacheListModel.backingList = filteredCaches
                        cacheList.isVisible = true
                        listCachesPlaceholder.isVisible = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                SwingUtilities.invokeLater {
                    listCachesPlaceholder.text = "Error loading caches: ${e.message}"
                    if (e is SSLHandshakeException) {
                        listCachesPlaceholder.text += "\n\nSSLHandshakeException is a known bug with certain Java versions, try updating."
                    }
                    listCachesPlaceholder.isVisible = true
                    cacheList.isVisible = false
                }
            }
        }.start()
    }

    private fun downloadCache(
        cache: OpenRs2Cache,
        onComplete: (String) -> Unit,
        onFailure: (IOException) -> Unit,
    ) {
        // Generate a folder name based on date and build
        val dateStr = cache.dateString ?: "unknown"
        val buildStr = "rev${cache.builds.firstOrNull()?.versionString ?: "unknown"}"

        val destFolderName = "$dateStr-$buildStr"
        val destFolder = File(AppConstants.CACHES_DIRECTORY, destFolderName)

        Thread {
            try {
                val openRs2Api = OpenRs2Api()

                // Download and extract cache from OpenRS2
                BufferedInputStream(openRs2Api.fetchCacheZipById(cache.scope, cache.id.toString())).use { inputStream ->

                    ZipInputStream(inputStream).use { zipIn ->
                        // Delete existing cache folder if it exists to avoid conflicts
                        val cacheDir = File(destFolder, "cache")
                        cacheDir.deleteRecursively()

                        var zipEntry: ZipEntry? = zipIn.nextEntry
                        while (zipEntry != null) {
                            // OpenRS2 disk.zip format: cache/<filename>.dat2 or cache/<filename>.idx
                            // Extract directly to destFolder/cache/<filename>
                            if (!zipEntry.isDirectory) {
                                val entryName = zipEntry.name
                                // Remove the leading "cache/" prefix from the path if present
                                val relativePath = entryName.removePrefix("cache/")
                                val dest = File(destFolder, "cache/$relativePath")
                                dest.parentFile?.mkdirs()
                                // Use REPLACE_EXISTING to overwrite files if they exist
                                Files.copy(zipIn, dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
                            }
                            zipIn.closeEntry()
                            zipEntry = zipIn.nextEntry
                        }
                    }
                }

                // Also download the keys if available
                try {
                    val keys = openRs2Api.fetchCacheKeysById(cache.scope, cache.id.toString())
                    if (keys.isNotEmpty()) {
                        val filePath = destFolder.toPath().resolve("xteas.json")
                        Files.createDirectories(filePath.parent)
                        Files.newBufferedWriter(
                            filePath,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING
                        ).use { writer ->
                            ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(writer, keys)
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Warning: Could not download cache keys", e)
                    // Continue anyway, keys are optional
                }

                // Generate a basic params.txt file with the revision number from build
                try {
                    val revisionNumber = cache.builds.firstOrNull()?.major

                    if (revisionNumber != null) {
                        val paramsFile = File(destFolder, "params.txt")
                        Files.createDirectories(destFolder.toPath())
                        Files.newBufferedWriter(
                            paramsFile.toPath(),
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING
                        ).use { writer ->
                            writer.write("Created at ${LocalDateTime.now()}\n")
                            writer.write("Synthetic params.txt, not derived from launcher\n")
                            writer.write("param=${ParamType.REVISION.id}=$revisionNumber\n")
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Warning: Could not create params.txt", e)
                    // Continue anyway, params.txt is optional
                }

                SwingUtilities.invokeLater {
                    onComplete(destFolder.absolutePath)
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
            setErrorText(lblErrorText, defaultErrorText(e))
            btnLaunch.isEnabled = false
            return
        }

        xteaManager = try {
            XteaManager(txtCacheLocation.text)
        } catch (e: Exception) {
            if (e is JsonMappingException || e is JsonProcessingException) {
                setErrorText(lblErrorText, "Bad cache: Could not decode xteas file: ${e.message ?: "Unknown error"}")
                return
            }

            if (e is FileNotFoundException) {
                if (e.message?.contains("xteas.json") ?: false) {
                    logger.warn("cache decryption keys not found as part of installed cache. Searching archive.openrs2.org")
                    val success = tryLocateCacheKeys(txtCacheLocation.text)
                    if (!success) {
                        setErrorText(lblErrorText, defaultErrorText(e))
                    }
                    return
                }
            }

            setErrorText(lblErrorText, defaultErrorText(e))
            btnLaunch.isEnabled = false
            return
        }

        try {
            paramsManager.loadFromPath(txtCacheLocation.text)
        } catch (e: Exception) {
            setErrorText(lblErrorText, defaultErrorText(e))
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

        logger.debug("attempting to locate cache decryption keys for cache: {}", cacheLocation)

        val openRsApi = OpenRs2Api()
        val caches = openRsApi.fetchCaches().filter { it.game == "oldschool" }

        for (cache in caches) {
            val localDate = cache.timestampDateTime?.toLocalDate()

            if (localDate == date) {
                logger.debug("Found openrs2 cache matching date: {} with id: {}, fetching keys...", date, cache.id)
                val keys = openRsApi.fetchCacheKeysById(cache.scope, cache.id.toString())

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
            logger.error("failed to parse date from cache", e)
            return null
        }
    }

    private fun formatCacheDisplayString(cache: OpenRs2Cache): String {
        val dateStr = cache.dateString ?: "Unknown date"
        val buildStr = cache.builds.firstOrNull()?.versionString ?: "?"
        val envStr = cache.environment.replaceFirstChar { if (it.isLowerCase()) it.uppercaseChar() else it }

        return "$dateStr - Build $buildStr ($envStr)"
    }

    private fun setErrorText(lblErrorText: JLabel, message: String) {
        val escapedMessage = message
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
        // Use a larger width and allow word wrapping
        lblErrorText.text = "<html><body style='width: 400px; padding: 4px; word-wrap: break-word;'>$escapedMessage</body></html>"
    }

    private fun defaultErrorText(e: Exception) = when (e) {
        is FileNotFoundException -> "Bad cache: Missing required file: ${e.message}"
        else -> {
            e.printStackTrace()
            e.message ?: "Unknown error occurred"
        }
    }
}
