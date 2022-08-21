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
    val writeScale = 0.25 // 1:this ratio of region export to file write time

    private val sceneExporterListener = object : DialogSpawningSceneLoadProgressListener(parent) {
        override var progressContainer
            get() = this@SceneExportProgressDialogSpawner.progressContainer
            set(value) {
                this@SceneExportProgressDialogSpawner.progressContainer = value
            }
        override val progressMax get() = (super.progressMax * (1 + writeScale)).toInt() + 1 // regions * (encoding + writing) + json
        override val dialogTitle get() = "Exporting..."
        override val dialogSummary get() = "Exporting $numRegions regions..."
        override val statusDoing get() = "Exporting region $currentRegion of $numRegions"
        override val statusDone get() = "Writing to file"
    }

    private val sceneWriteListener = object : ChunkWriteListener {
        var bytesWritten = 0L
        var bytesTotal = 0L

        private val bytesFrac get() = bytesWritten.toDouble() / bytesTotal

        override fun onStartWriting(totalSize: Long) {
            checkCanceled()
            bytesTotal = totalSize
            bytesWritten = 0
            updateProgress()
        }

        override fun onChunkWritten(written: Long) {
            checkCanceled()
            bytesWritten += written
            updateProgress()
        }

        override fun onFinishWriting() {
            SwingUtilities.invokeLater {
                progressContainer.progress = progressContainer.progressMax
                progressContainer.status = "Done"
                progressContainer.complete()
            }
        }

        private fun bytesToProgress(bytes: Long): Int {
            // Total progress for writes is between
            // (sceneExporterListener.numRegions, sceneExporterListener.numRegions * 2)
            // so we need to scale the bytes written to the total bytes to get a progress value
            val numRegions = sceneExporterListener.numRegions
            if (bytesTotal == 0L) {
                return numRegions // zero progress
            }
            return (numRegions * (1 + bytesFrac * writeScale)).toInt()
        }

        private fun updateProgress() {
            SwingUtilities.invokeLater {
                progressContainer.progress = bytesToProgress(bytesWritten)
                if (bytesWritten == bytesTotal) {
                    progressContainer.status = "Writing JSON"
                } else {
                    progressContainer.status = String.format("Writing to file (%.0f%% complete)", bytesFrac * 100)
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
