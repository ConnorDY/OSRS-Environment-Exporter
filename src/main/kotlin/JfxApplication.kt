import cache.CacheModule
import cache.loaders.LoaderModule
import com.google.inject.Guice
import com.sun.javafx.css.StyleManager
import controllers.MainController
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage
import javafx.util.Callback
import models.ModelsModule
import controllers.worldRenderer.WorldRendererModule


class JfxApplication : Application() {
    companion object {
        val injector = Guice.createInjector(LoaderModule(), CacheModule(), ModelsModule(), WorldRendererModule())
    }

    override fun start(primaryStage: Stage) {
        primaryStage.title = "FOSS Map Editor"

        val fxmlLoader = FXMLLoader()
        fxmlLoader.controllerFactory = Callback { type: Class<*>? ->
            injector.getInstance(type)
        }

        // set global theme
        setUserAgentStylesheet(STYLESHEET_MODENA)

        StyleManager.getInstance().addUserAgentStylesheet(javaClass.getResource("theme.css").toExternalForm())

        // load and open main scene
        fxmlLoader.location = javaClass.getResource("/views/main.fxml")
        val root = fxmlLoader.load<Parent>()
        val controller = fxmlLoader.getController<MainController>()
        val jfxScene = Scene(root)
        primaryStage.scene = jfxScene
        primaryStage.sizeToScene()
        primaryStage.x = -10.0
        primaryStage.y = 0.0
        primaryStage.setOnShown { controller.forceRefresh() }
        primaryStage.show()
    }
}