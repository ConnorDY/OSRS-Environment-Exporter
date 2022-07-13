import cache.CacheModule
import cache.loaders.LoaderModule
import com.google.inject.Guice
import com.sun.javafx.css.StyleManager
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage
import javafx.util.Callback
import models.ModelsModule
import controllers.worldRenderer.WorldRendererModule
import kotlin.system.exitProcess

class JfxApplication : Application() {
    companion object {
        val injector = Guice.createInjector(LoaderModule(), CacheModule(), ModelsModule(), WorldRendererModule())
    }

    override fun start(stage: Stage) {
        // set global theme
        setUserAgentStylesheet(STYLESHEET_MODENA)

        StyleManager.getInstance().addUserAgentStylesheet(javaClass.getResource("theme.css").toExternalForm())

        val loadCacheLoader = FXMLLoader()
        loadCacheLoader.controllerFactory = Callback { type: Class<*>? ->
            injector.getInstance(type)
        }
        loadCacheLoader.location = javaClass.getResource("/views/cache-chooser.fxml")
        val cacheChooserRoot = loadCacheLoader.load<Parent>()
        stage.title = "Choose game cache version"
        stage.scene = Scene(cacheChooserRoot)
        stage.show()
    }

    override fun stop() {
        super.stop()
        exitProcess(0)
    }
}
