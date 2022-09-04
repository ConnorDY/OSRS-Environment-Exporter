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
}
