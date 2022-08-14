package models.config

import utils.Utils.isMacos

data class ConfigOption<T>(
    val id: String,
    val type: ConfigOptionType<T>,
    val default: T,
    val humanReadableName: String = "",
    val mnemonic: Char = 0.toChar(),
) {
    val hidden get() = humanReadableName.isEmpty()

    companion object {
        val lastCacheDir = ConfigOption("last-cache-dir", ConfigOptionType.string, "")
        val initialRegionId = ConfigOption("initial-region-id", ConfigOptionType.int, 15256)
        val initialRadius = ConfigOption("initial-radius", ConfigOptionType.int, 1)
        val fpsCap = ConfigOption("fps-cap", ConfigOptionType.intToggle, 60, "Limit FPS", 'F')
        val checkForUpdates = ConfigOption("check-for-updates", ConfigOptionType.boolean, true, "Check for updates", 'U')
        val lastCheckedForUpdates = ConfigOption("last-checked-for-updates", ConfigOptionType.long, 0L)
        val debug = ConfigOption("debug", ConfigOptionType.boolean, false, "Debug mode (requires restart)", 'D')
        val mouseWarping = ConfigOption("mouse-warping", ConfigOptionType.boolean, !isMacos(), "Enable mouse warping", 'W')

        val all = listOf(
            lastCacheDir,
            initialRegionId,
            initialRadius,
            fpsCap,
            checkForUpdates,
            lastCheckedForUpdates,
            mouseWarping,
            debug,
        )
    }
}
