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
    var scaleFactor = configOptions.scaleMode.value.get().scaleFactor
    var hasScaleFactor = false
    var enabledZLayers = (0..3).toList()
}
