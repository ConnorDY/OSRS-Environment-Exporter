package models.config

import controllers.worldRenderer.helpers.AlphaMode
import controllers.worldRenderer.helpers.AntiAliasingMode
import utils.Utils.isMacOS
import controllers.worldRenderer.Renderer.PreferredPriorityRenderer as PriorityRenderers

class ConfigOptions(private val configuration: Configuration) {
    val lastCacheDir = ConfigOption("last-cache-dir", ConfigOptionType.string, "")
    val initialRegionId = ConfigOption("initial-region-id", ConfigOptionType.int, 15256)
    val initialRadius = ConfigOption("initial-radius", ConfigOptionType.int, 1)
    val fpsCap = ConfigOption("fps-cap", ConfigOptionType.intToggle, 60, "Limit FPS", 'F')
    val powerSavingMode = ConfigOption("power-saving-mode", ConfigOptionType.boolean, false, "Power saving mode", 'P')
    val checkForUpdates = ConfigOption("check-for-updates", ConfigOptionType.boolean, true, "Check for updates", 'U')
    val lastCheckedForUpdates = ConfigOption("last-checked-for-updates", ConfigOptionType.long, 0L)
    val debug = ConfigOption("debug", ConfigOptionType.boolean, false, "Debug mode", 'D')
    val mouseWarping = ConfigOption("mouse-warping", ConfigOptionType.boolean, !isMacOS(), "Enable mouse warping", 'W')
    val antiAliasing = ConfigOption("anti-aliasing", ConfigOptionType.Enumerated(AntiAliasingMode::valueOf, AntiAliasingMode.values(), AntiAliasingMode::humanReadableName), AntiAliasingMode.MSAA_16, "Anti-aliasing", 'A')
    val priorityRenderer = ConfigOption("priority-renderer", ConfigOptionType.Enumerated(PriorityRenderers::valueOf, PriorityRenderers.values(), PriorityRenderers::humanReadableName), if (isMacOS()) PriorityRenderers.CPU_NAIVE else PriorityRenderers.GLSL, "Sorting renderer", 'R')
    val alphaMode = ConfigOption("alpha-mode", ConfigOptionType.Enumerated(AlphaMode::valueOf, AlphaMode.values(), AlphaMode::humanReadableName), AlphaMode.ORDERED_DITHER, "Alpha mode", 'L')
    val sampleShading = ConfigOption("sample-shading", ConfigOptionType.boolean, false, "Sub-sample shading (GL4.0; makes hashed alpha look nicer)", 'S')
    val fov = ConfigOption("fov", ConfigOptionType.double, 90.0, "Field of view (degrees)", 'V')
    val skyColor = ConfigOption("sky-color", ConfigOptionType.string, "#90DBE8", "Sky color (hex)", 'C')

    val isMacOS = isMacOS()

    val all = listOf(
        lastCacheDir,
        initialRegionId,
        initialRadius,
        fpsCap,
        powerSavingMode,
        checkForUpdates,
        lastCheckedForUpdates,
        mouseWarping,
        skyColor,
        fov,
        antiAliasing,
        alphaMode,
        sampleShading,
        priorityRenderer,
        debug,
    ).filter { !isMacOS || it.id != "priority-renderer" }

    init {
        all.forEach { load(it) }
    }

    private fun <T> load(it: ConfigOption<T>) {
        it.value.set(configuration.getProp(it))
    }

    private fun <T> setInConfig(it: ConfigOption<T>): Boolean {
        return configuration.setProp(it, it.value.get())
    }

    fun save() {
        val changed = all.count { setInConfig(it) }
        if (changed != 0)
            configuration.save()
    }
}
