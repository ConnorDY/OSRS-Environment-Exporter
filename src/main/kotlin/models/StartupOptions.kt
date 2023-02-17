package models

import AppConstants.OUTPUT_DIRECTORY
import models.config.ConfigOptions

class StartupOptions(configOptions: ConfigOptions) {
    var cacheDir = configOptions.lastCacheDir.value.get()
    var regionId = configOptions.initialRegionId.value.get()
    var radius = configOptions.initialRadius.value.get()
    var exportOnly = false
    var exportDir = OUTPUT_DIRECTORY
    var exportFlat = false
    var showPreview = true
    var scaleFactor = 0f
    val hasScaleFactor get() = scaleFactor != 0f
    val defaultScaleFactor = configOptions.scaleMode.value.get().scaleFactor
}
