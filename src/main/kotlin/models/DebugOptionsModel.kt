package models

import utils.ObservableValue

class DebugOptionsModel {
    val modelSubIndex = ObservableValue(-1)
    val resetCameraOnSceneChange = ObservableValue(true)
    val onlyType10Models = ObservableValue(false)
}
