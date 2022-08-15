package models

import models.config.ConfigOption
import models.config.ConfigOptionType

class DebugOptionsModel {
    val removeProperlyTypedModels = ConfigOption("", ConfigOptionType.boolean, false, "Remove properly-typed models", 'E')
    val badModelIndexOverride = ConfigOption("", ConfigOptionType.int, -1, "Bad model index override", 'B')
    val modelSubIndex = ConfigOption("", ConfigOptionType.int, -1, "Model sub-index", 'S')
    val resetCameraOnSceneChange = ConfigOption("", ConfigOptionType.boolean, true, "Reset camera on scene change", 'R')
    val onlyType10Models = ConfigOption("", ConfigOptionType.boolean, false, "Only type-10 models", '1')

    val all = listOf(onlyType10Models, resetCameraOnSceneChange, removeProperlyTypedModels, modelSubIndex, badModelIndexOverride)
}
