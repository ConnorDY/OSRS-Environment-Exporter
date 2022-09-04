package controllers.main

import models.scene.SceneLoadProgressListener
import ui.CancelledException
import utils.ProgressContainer
import javax.swing.SwingUtilities

abstract class CountingSceneLoadProgressListener : SceneLoadProgressListener {
    protected abstract val progressContainer: ProgressContainer
    var currentRegion = 0
        private set
    var numRegions = 0
        private set
    protected open val progress get() = currentRegion
    protected open val progressMax get() = numRegions
    protected abstract val statusDoing: String
    protected abstract val statusDone: String

    override fun onBeginLoadingRegions(count: Int) {
        checkCancelled()

        currentRegion = 0
        numRegions = count
        SwingUtilities.invokeLater {
            prepareProgressContainer()
            progressContainer.progressMax = progressMax
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

    open fun prepareProgressContainer() {}

    private fun advanceProgress() {
        val progressBeforeInc = progress
        progressContainer.progress = progressBeforeInc

        currentRegion++
        if (currentRegion > numRegions) {
            progressContainer.status = statusDone
            if (progressBeforeInc == progressMax) {
                progressContainer.complete()
            }
        } else {
            progressContainer.status = statusDoing
        }
    }

    private fun checkCancelled() {
        if (progressContainer.isCancelled) {
            SwingUtilities.invokeLater {
                progressContainer.status = "Cancelled"
                progressContainer.complete()
            }
            throw CancelledException()
        }
    }
}
