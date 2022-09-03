package controllers.main

import controllers.worldRenderer.SceneExporter
import ui.CancelledException
import utils.ChunkWriteListener
import utils.NullProgressContainer
import utils.ProgressContainer
import java.awt.Frame
import javax.swing.SwingUtilities

class SceneExportProgressDialogSpawner(private val parent: Frame) {
    var progressContainer: ProgressContainer = NullProgressContainer()

    private val sceneExporterListener = object : DialogSpawningSceneLoadProgressListener(parent) {
        override var progressContainer
            get() = this@SceneExportProgressDialogSpawner.progressContainer
            set(value) {
                this@SceneExportProgressDialogSpawner.progressContainer = value
            }
        override val progress get() = super.progress * 2 // interleaved export and write
        override val progressMax get() = super.progressMax * 2 + 1 // regions * (encoding + writing) + json
        override val dialogTitle get() = "Exporting..."
        override val dialogSummary get() = "Exporting $numRegions regions..."
        public override val statusDoing get() = "Exporting region $currentRegion of $numRegions"
        override val statusDone get() = "Writing to file"
    }

    private val sceneWriteListener = object : ChunkWriteListener {
        var currentRegion = 0

        override fun onStartRegion(regionNum: Int) {
            checkCanceled()
            currentRegion = regionNum
            updateProgress(regionNum == -1)
        }

        override fun onEndRegion() {
            checkCanceled()
            SwingUtilities.invokeLater {
                progressContainer.progress++
                if (currentRegion != sceneExporterListener.numRegions) {
                    // Interleaving with other listener's action again
                    progressContainer.status = sceneExporterListener.statusDoing
                }
            }
        }

        override fun onFinishWriting() {
            SwingUtilities.invokeLater {
                progressContainer.progress = progressContainer.progressMax
                progressContainer.status = "Done"
                progressContainer.complete()
            }
        }

        private fun updateProgress(writingMetadata: Boolean) {
            SwingUtilities.invokeLater {
                if (writingMetadata) {
                    progressContainer.status = "Writing JSON"
                } else {
                    progressContainer.status = "Writing region ${currentRegion + 1} of ${sceneExporterListener.numRegions}"
                }
            }
        }

        private fun checkCanceled() {
            if (progressContainer.isCancelled) {
                SwingUtilities.invokeLater {
                    progressContainer.status = "Cancelled"
                    progressContainer.complete()
                }
                throw CancelledException()
            }
        }
    }

    fun attach(sceneExporter: SceneExporter) {
        sceneExporter.sceneLoadProgressListeners.add(sceneExporterListener)
        sceneExporter.chunkWriteListeners.add(sceneWriteListener)
    }
}
