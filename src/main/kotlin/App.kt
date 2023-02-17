import controllers.CacheChooserController
import models.StartupOptions
import models.config.ConfigOptions
import models.config.Configuration
import org.pushingpixels.radiance.theming.api.skin.RadianceGraphiteAquaLookAndFeel
import utils.PackageMetadata
import utils.Utils.isWindows
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities
import javax.swing.ToolTipManager
import javax.swing.UIManager
import javax.swing.UnsupportedLookAndFeelException

private fun setSysProperty(key: String, value: String) {
    if (System.getProperty(key) == null) {
        System.setProperty(key, value)
    }
}

private fun startSwingApplication(configOptions: ConfigOptions, startupOptions: StartupOptions) {
    setThemingOptions()

    SwingUtilities.invokeLater {
        // These may be occluded by the GL canvas
        JPopupMenu.setDefaultLightWeightPopupEnabled(false)
        ToolTipManager.sharedInstance().isLightWeightPopupEnabled = false

        CacheChooserController(
            "Choose game cache version",
            configOptions,
            startupOptions,
        ).isVisible = true
    }
}

private fun printVersion() {
    println("${PackageMetadata.NAME} version ${PackageMetadata.VERSION}")
}
private fun printHelp() {
    printVersion()
    println()

    val programName = if (isWindows()) "run.bat" else "./run"
    println("Usage:")
    println("  $programName [options] [--] [regionID] [radius]")
    println()
    println("Options:")
    println("  --help, -h            Print this help message")
    println("  --version, -v         Print the version number")
    println("  --debug, -d           Enable debug mode for this run")
    println("  --cache-dir <path>    Set the cache directory for this run")
    println("  --export, -e          Don't run the GUI, just export the scene")
    println("  --export-dir <path>   Set the export directory for this run")
    println("  --flat, -f            Do not use timestamped subdirectories for export")
    println("  --format <format>     Set the export format for this run")
    println("  --scale <factor>      Set the export scale factor (e.g. 1:128), overrides config")
    println("  --no-preview          Run the GUI, but don't render the preview")
    println("  --                    End of options")
    println()
    println("Arguments:")
    println("  regionID              The region ID to load")
    println("  radius                The radius of regions to load")
}

fun main(args: Array<String>) {
    val configOptions = ConfigOptions(Configuration())
    val startupOptions = StartupOptions(configOptions)

    // Parse command line arguments
    if (args.isNotEmpty()) {
        var argIndex = 0
        while (argIndex < args.size) {
            val arg = args[argIndex]
            when (arg) {
                "--help", "-h" -> {
                    printHelp()
                    return
                }
                "--version", "-v" -> {
                    printVersion()
                    return
                }
                "--debug", "-d" -> {
                    configOptions.debug.value.set(true)
                }
                "--cache-dir" -> {
                    if (args.size < 2) {
                        println("Error: --cache-dir requires an argument")
                        return
                    }
                    startupOptions.cacheDir = args[++argIndex]
                }
                "--export", "-e" -> {
                    startupOptions.exportOnly = true
                }
                "--export-dir" -> {
                    if (args.size < 2) {
                        println("Error: --export-dir requires an argument")
                        return
                    }
                    startupOptions.exportDir = args[++argIndex]
                }
                "--flat", "-f" -> {
                    startupOptions.exportFlat = true
                }
                "--format" -> {
                    if (args.size < 2) {
                        println("Error: --format requires an argument")
                        return
                    }
                    val formatName = args[++argIndex]
                    // Placeholder for when we actually support multiple export formats
                    if (formatName != "gltf") {
                        println("Error: Unknown export format: $formatName")
                        return
                    }
                }
                "--scale" -> {
                    if (args.size < 2) {
                        println("Error: --scale requires an argument")
                        return
                    }
                    val scale = args[++argIndex]
                    val scaleParts = scale.split(":")
                    if (scaleParts.size != 2) {
                        println("Error: Invalid scale: $scale")
                        return
                    }
                    try {
                        val scaleNumerator = scaleParts[0].trim().toFloat()
                        val scaleDenominator = scaleParts[1].trim().toFloat()
                        if (scaleNumerator == 0f || scaleDenominator == 0f) {
                            println("Error: Invalid scale: $scale")
                            return
                        }
                        startupOptions.scaleFactor = scaleNumerator / scaleDenominator
                    } catch (e: NumberFormatException) {
                        println("Error: Invalid scale: $scale")
                        return
                    }
                }
                "--no-preview" -> {
                    startupOptions.showPreview = false
                }
                "--" -> {
                    argIndex++
                    break
                }

                else -> {
                    if (arg.startsWith("-")) {
                        println("Error: Unknown option '$arg'")
                        return
                    } else {
                        // Positional argument, pass downwards
                        break
                    }
                }
            }
            argIndex++
        }

        // We should have 2 arguments left: region ID and radius. Anything else is an error.
        if (args.size - argIndex > 2) {
            println("Error: Too many arguments")
            return
        }

        if (argIndex < args.size) {
            startupOptions.regionId = args[argIndex++].toInt()
        }
        if (argIndex < args.size) {
            startupOptions.radius = args[argIndex].toInt()
        }
    }

    if (!startupOptions.exportOnly) {
        startSwingApplication(configOptions, startupOptions)
    } else {
        CliExporter(startupOptions).exportRadius(startupOptions.regionId, startupOptions.radius)
    }
}

fun setThemingOptions() {
    setSysProperty("awt.useSystemAAFontSettings", "on")
    setSysProperty("swing.aatext", "true")

    SwingUtilities.invokeLater {
        try {
            UIManager.setLookAndFeel(RadianceGraphiteAquaLookAndFeel())
        } catch (_: UnsupportedLookAndFeelException) {
        } catch (_: ClassNotFoundException) {
        } catch (_: InstantiationException) {
        } catch (_: IllegalAccessException) {
        }
    }
}
