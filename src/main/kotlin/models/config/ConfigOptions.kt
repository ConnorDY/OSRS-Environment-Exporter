package models.config

import controllers.worldRenderer.helpers.AlphaMode
import controllers.worldRenderer.helpers.AntiAliasingMode
import utils.Utils.isMacos
import controllers.worldRenderer.Renderer.PreferredPriorityRenderer as PriorityRenderers

class ConfigOptions(private val configuration: Configuration) {
    val lastCacheDir = ConfigOption("last-cache-dir", ConfigOptionType.string, "")
    val initialRegionId = ConfigOption("initial-region-id", ConfigOptionType.int, 15256)
    val initialRadius = ConfigOption("initial-radius", ConfigOptionType.int, 1)
    val fpsCap = ConfigOption("fps-cap", ConfigOptionType.intToggle, 60, "Limit FPS", 'F')
    val checkForUpdates = ConfigOption("check-for-updates", ConfigOptionType.boolean, true, "Check for updates", 'U')
    val lastCheckedForUpdates = ConfigOption("last-checked-for-updates", ConfigOptionType.long, 0L)
    val debug = ConfigOption("debug", ConfigOptionType.boolean, false, "Debug mode", 'D')
    val mouseWarping = ConfigOption("mouse-warping", ConfigOptionType.boolean, !isMacos(), "Enable mouse warping", 'W')
    val antiAliasing = ConfigOption("anti-aliasing", ConfigOptionType.Enumerated(AntiAliasingMode::valueOf, AntiAliasingMode.values(), AntiAliasingMode::humanReadableName), AntiAliasingMode.MSAA_16, "Anti-aliasing", 'A')
    val priorityRenderer = ConfigOption("priority-renderer", ConfigOptionType.Enumerated(PriorityRenderers::valueOf, PriorityRenderers.values(), PriorityRenderers::humanReadableName), if (isMacos()) PriorityRenderers.CPU_NAIVE else PriorityRenderers.GLSL, "Sorting renderer", 'R')
    val alphaMode = ConfigOption("alpha-mode", ConfigOptionType.Enumerated(AlphaMode::valueOf, AlphaMode.values(), AlphaMode::humanReadableName), AlphaMode.ORDERED_DITHER, "Alpha mode", 'L')
    val sampleShading = ConfigOption("sample-shading", ConfigOptionType.boolean, false, "Sub-sample shading (GL4.0; makes hashed alpha look nicer)", 'S')

    val all = listOf(
        lastCacheDir,
        initialRegionId,
        initialRadius,
        fpsCap,
        checkForUpdates,
        lastCheckedForUpdates,
        mouseWarping,
        antiAliasing,
        alphaMode,
        sampleShading,
        priorityRenderer,
        debug,
    )

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
