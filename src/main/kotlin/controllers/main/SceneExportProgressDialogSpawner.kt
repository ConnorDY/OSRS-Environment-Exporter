package controllers.main

import controllers.worldRenderer.SceneExporter
import models.scene.SceneLoadProgressListener
import ui.CancelledException
import ui.ProgressDialog
import utils.NullProgressContainer
import utils.ProgressContainer
import java.awt.Frame
import javax.swing.SwingUtilities

class SceneExportProgressDialogSpawner(private val parent: Frame) {
    private val sceneExporterListener = object : SceneLoadProgressListener {
        private var progressContainer: ProgressContainer = NullProgressContainer()
        private var numRegions = 0
        private var currentRegion = 0

        override fun onBeginLoadingRegions(count: Int) {
            // if regions finish before AWT thread kicks in, we don't want to reuse a stale progress dialog
            progressContainer = NullProgressContainer()

            numRegions = count
            currentRegion = 0

            SwingUtilities.invokeLater {
                if (count > 10) {
                    // Show a dialog if we have a lot to export
                    progressContainer =
                        ProgressDialog(parent, "Exporting...", "Exporting $count regions...").also {
                            it.setLocationRelativeTo(parent)
                            it.isVisible = true
                        }
                }
                progressContainer.progressMax = count
                advanceProgress()
            }
        }

        override fun onRegionLoaded() {
            checkCancelled()

            SwingUtilities.invokeLater {
                advanceProgress()
            }
        }

        override fun onError() {
            SwingUtilities.invokeLater {
                progressContainer.status = "An error occurred"
                progressContainer.complete()
            }
        }

        private fun advanceProgress() {
            // Now done the region we just processed
            progressContainer.progress = currentRegion

            // Increment and tell the user we're working on the next one
            currentRegion++
            if (currentRegion > numRegions) {
                progressContainer.status = "Done!"
                progressContainer.complete()
            } else {
                progressContainer.status = "Exporting region $currentRegion of $numRegions"
            }
        }

        private fun checkCancelled() {
            if (progressContainer.isCancelled) {
                progressContainer.status = "Cancelled"
                progressContainer.complete()
                throw CancelledException()
            }
        }
    }

    fun attach(sceneExporter: SceneExporter) {
        sceneExporter.sceneLoadProgressListeners.add(sceneExporterListener)
    }
}
