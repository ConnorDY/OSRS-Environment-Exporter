package controllers.main

import controllers.worldRenderer.SceneExporter
import utils.NullProgressContainer
import utils.ProgressContainer
import java.awt.Frame

class SceneExportProgressDialogSpawner(private val parent: Frame) {
    private val sceneExporterListener = object : DialogSpawningSceneLoadProgressListener(parent) {
        override var progressContainer: ProgressContainer = NullProgressContainer()
        override val dialogTitle get() = "Exporting..."
        override val dialogSummary get() = "Exporting $numRegions regions..."
        override val statusDoing get() = "Exporting region $currentRegion of $numRegions"
        override val statusDone get() = "Done!"
    }

    fun attach(sceneExporter: SceneExporter) {
        sceneExporter.sceneLoadProgressListeners.add(sceneExporterListener)
    }
}
