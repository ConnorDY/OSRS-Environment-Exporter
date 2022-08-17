package models

import utils.ObservableValue

class FrameRateModel(val powerSavingMode: ObservableValue<Boolean>) {
    var frameCount = 0
    val updateNotifier = Object()
    var needAnotherFrame = false

    fun notifyNeedFrames() {
        synchronized(updateNotifier) {
            needAnotherFrame = true
            updateNotifier.notifyAll()
        }
    }
}
