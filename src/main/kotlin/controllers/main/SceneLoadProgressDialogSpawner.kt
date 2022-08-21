package controllers.main

import controllers.worldRenderer.Renderer
import controllers.worldRenderer.SceneDrawListener
import controllers.worldRenderer.SceneUploader
import models.scene.Scene
import models.scene.SceneLoadProgressListener
import ui.CancelledException
import ui.ProgressDialog
import utils.NullProgressContainer
import utils.ProgressContainer
import java.awt.Frame
import javax.swing.SwingUtilities

class SceneLoadProgressDialogSpawner(val parent: Frame) {
    private var progressContainer: ProgressContainer = NullProgressContainer()
    private var initialNumRegions = 0
    private var numDrawableRegions = 0

    private fun checkCancelled() {
        if (progressContainer.isCancelled) {
            progressContainer.status = "Cancelled"
            progressContainer.complete()
            throw CancelledException()
        }
    }

    private val sceneLoaderListener = object : SceneLoadProgressListener {
        var currentRegion = 0

        override fun onBeginLoadingRegions(count: Int) {
            // if regions load before AWT thread kicks in, we don't want to reuse a stale progress dialog
            progressContainer = NullProgressContainer()

            initialNumRegions = count
            currentRegion = 0

            SwingUtilities.invokeLater {
                if (count > 10) {
                    // Show a dialog if we have a lot to load
                    progressContainer =
                        ProgressDialog(parent, "Loading...", "Loading $count regions...").also {
                            it.setLocationRelativeTo(parent)
                            it.isVisible = true
                        }
                }
                progressContainer.progressMax = count * 2 + 1 // regions * (load + upload) + draw
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
            if (currentRegion > initialNumRegions) {
                progressContainer.status = "Finished decoding regions; passing to uploader"
            } else {
                progressContainer.status = "Decoding region $currentRegion of $initialNumRegions"
            }
        }
    }

    private val sceneUploadProgressListener = object : SceneLoadProgressListener {
        var currentRegion = 0

        override fun onBeginLoadingRegions(count: Int) {
            checkCancelled()

            numDrawableRegions = count
            SwingUtilities.invokeLater {
                progressContainer.progressMax = initialNumRegions + count + 1
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
            progressContainer.progress = currentRegion + initialNumRegions

            currentRegion++
            if (currentRegion >= numDrawableRegions) {
                progressContainer.status = "Sending to GPU"
            } else {
                progressContainer.status = "Drawing region $currentRegion of $numDrawableRegions"
            }
        }
    }

    private val sceneDrawListener = object : SceneDrawListener {
        override fun onStartDraw() {
            checkCancelled()

            // Can't really do anything here because the calculation is done with the AWT lock held
            // So this has been rolled into the previous class's advanceProgress() method
        }

        override fun onEndDraw() {
            checkCancelled()

            SwingUtilities.invokeLater {
                progressContainer.progress++
                progressContainer.status = "Finished drawing"
                progressContainer.complete()
            }
        }
    }

    fun attach(scene: Scene, sceneUploader: SceneUploader, renderer: Renderer) {
        scene.sceneLoadProgressListeners.add(sceneLoaderListener)
        sceneUploader.sceneLoadProgressListeners.add(sceneUploadProgressListener)
        renderer.sceneDrawListeners.add(sceneDrawListener)
    }
}
