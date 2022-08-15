package controllers

import models.DebugOptionsModel
import java.awt.Frame

class DebugOptionsController(owner: Frame, model: DebugOptionsModel) : AbstractSettingsDialog(owner, "Debug Options", model.all) {
    init {
        pack()
    }
}
