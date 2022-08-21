package controllers.main

import ui.ProgressDialog
import utils.NullProgressContainer
import utils.ProgressContainer
import java.awt.Frame

abstract class DialogSpawningSceneLoadProgressListener(private val parent: Frame) : CountingSceneLoadProgressListener() {
    abstract override var progressContainer: ProgressContainer
    protected open val dialogTitle get() = "Loading..."
    protected open val dialogSummary get() = "Loading $numRegions regions..."

    override fun prepareProgressContainer() {
        if (numRegions > 10) {
            // Show a dialog if we have a lot to load
            progressContainer =
                ProgressDialog(parent, dialogTitle, dialogSummary).also {
                    it.setLocationRelativeTo(parent)
                    it.isVisible = true
                }
        }
        super.prepareProgressContainer()
    }

    override fun onBeginLoadingRegions(count: Int) {
        // if regions load before AWT thread kicks in, we don't want to reuse a stale progress dialog
        progressContainer = NullProgressContainer()
        super.onBeginLoadingRegions(count)
    }
}
