package models

import cache.definitions.RegionDefinition.Companion.Z
import models.config.ConfigOption
import models.config.ConfigOptionType
import utils.ObservableValue

class DebugOptionsModel {
    val removeProperlyTypedModels = ConfigOption("", ConfigOptionType.boolean, false, "Remove properly-typed models", 'E')
    val badModelIndexOverride = ConfigOption("", ConfigOptionType.int, -1, "Bad model index override", 'B')
    val modelSubIndex = ConfigOption("", ConfigOptionType.int, -1, "Model sub-index", 'S')
    val resetCameraOnSceneChange = ConfigOption("", ConfigOptionType.boolean, true, "Reset camera on scene change", 'R')
    val showOnlyModelType = ConfigOption("", ConfigOptionType.intToggle, null, "Show only model type", 'T')
    val showTilePaint = ConfigOption("", ConfigOptionType.boolean, true, "Show tile paint", 'P')
    val showTileModels = ConfigOption("", ConfigOptionType.boolean, true, "Show tile models", 'M')

    val all = listOf(showOnlyModelType, showTilePaint, showTileModels, resetCameraOnSceneChange, removeProperlyTypedModels, modelSubIndex, badModelIndexOverride)

    val zLevelsSelected = Array(Z) { true }.map(::ObservableValue)

    fun setZLevelsFromList(enabledZLayers: List<Int>) {
        zLevelsSelected.forEachIndexed { index, value ->
            value.set(index in enabledZLayers)
        }
    }
}
