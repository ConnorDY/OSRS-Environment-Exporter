package models

import utils.ObservableValue

class DebugOptionsModel {
    val removeProperlyTypedModels = ObservableValue(false)
    val badModelIndexOverride = ObservableValue(-1)
    val modelSubIndex = ObservableValue(-1)
    val resetCameraOnSceneChange = ObservableValue(true)
    val onlyType10Models = ObservableValue(false)

    val all = listOf(removeProperlyTypedModels, badModelIndexOverride, modelSubIndex, resetCameraOnSceneChange, onlyType10Models)
}
