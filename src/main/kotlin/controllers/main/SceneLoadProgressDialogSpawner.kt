package controllers.main

import controllers.worldRenderer.Renderer
import controllers.worldRenderer.SceneDrawListener
import controllers.worldRenderer.SceneUploader
import models.scene.Scene
import ui.CancelledException
import utils.NullProgressContainer
import utils.ProgressContainer
import java.awt.Frame
import javax.swing.SwingUtilities

class SceneLoadProgressDialogSpawner(private val parent: Frame) {
    private var progressContainer: ProgressContainer = NullProgressContainer()
    private var hasPreview = false

    private fun checkCancelled() {
        if (progressContainer.isCancelled) {
            progressContainer.status = "Cancelled"
            progressContainer.complete()
            throw CancelledException()
        }
    }

    private val sceneLoaderListener = object : DialogSpawningSceneLoadProgressListener(parent) {
        override var progressContainer
            get() = this@SceneLoadProgressDialogSpawner.progressContainer
            set(value) {
                this@SceneLoadProgressDialogSpawner.progressContainer = value
            }
        override val progressMax get() =
            if (hasPreview) numRegions * 2 + 1 // regions * (load + upload) + draw
            else numRegions
        override val statusDoing get() = "Decoding region $currentRegion of $numRegions"
        override val statusDone get() =
            if (hasPreview) "Finished decoding regions; passing to uploader"
            else "Done"
    }

    private val sceneUploadProgressListener = object : CountingSceneLoadProgressListener() {
        override val progressContainer get() = this@SceneLoadProgressDialogSpawner.progressContainer
        override val progress get() = super.progress + sceneLoaderListener.numRegions
        override val progressMax get() = numRegions + sceneLoaderListener.numRegions + 1
        override val statusDoing get() = "Drawing region $currentRegion of $numRegions"
        override val statusDone get() = "Sending to GPU"
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

        override fun onError(t: Throwable) {
            SwingUtilities.invokeLater {
                progressContainer.status = "An error occurred"
                progressContainer.complete()
            }
        }
    }

    fun attach(scene: Scene, sceneUploader: SceneUploader, renderer: Renderer) {
        attach(scene)
        sceneUploader.sceneLoadProgressListeners.add(sceneUploadProgressListener)
        renderer.sceneDrawListeners.add(sceneDrawListener)
        hasPreview = true
    }

    fun attach(scene: Scene) {
        scene.sceneLoadProgressListeners.add(sceneLoaderListener)
    }
}
