package environmentexporter

import cache.XteaManager
import cache.definitions.converters.ObjectToModelConverter
import cache.loaders.LocationsLoader
import cache.loaders.ModelLoader
import cache.loaders.ObjectLoader
import cache.loaders.OverlayLoader
import cache.loaders.RegionLoader
import cache.loaders.SpriteLoader
import cache.loaders.TextureLoader
import cache.loaders.UnderlayLoader
import com.displee.cache.CacheLibrary
import controllers.worldRenderer.SceneExporter
import controllers.worldRenderer.TextureManager
import models.DebugOptionsModel
import models.scene.Scene
import models.scene.SceneRegionBuilder

class CliExporter(startupOptions: StartupOptions) {
    val scene: Scene
    val exporter: SceneExporter

    init {
        val cacheLibrary = CacheLibrary("${startupOptions.cacheDir}/cache")
        val xteaManager = XteaManager(startupOptions.cacheDir)

        val spriteLoader = SpriteLoader(cacheLibrary)
        val textureLoader = TextureLoader(cacheLibrary)
        val regionLoader = RegionLoader(cacheLibrary)
        val locationsLoader = LocationsLoader(cacheLibrary, xteaManager)
        val objectLoader = ObjectLoader(cacheLibrary)
        val underlayLoader = UnderlayLoader(cacheLibrary)
        val overlayLoader = OverlayLoader(cacheLibrary)
        val modelLoader = ModelLoader(cacheLibrary)

        val debugOptionsModel = DebugOptionsModel()

        val textureManager = TextureManager(spriteLoader, textureLoader)
        val objectToModelConverter = ObjectToModelConverter(modelLoader, debugOptionsModel)
        val sceneRegionBuilder = SceneRegionBuilder(regionLoader, locationsLoader, objectLoader, underlayLoader, overlayLoader, objectToModelConverter)

        scene = Scene(sceneRegionBuilder, debugOptionsModel)
        exporter = SceneExporter(textureManager, debugOptionsModel)

        scene.sceneChangeListeners.add {
            // Export the scene once it has been loaded.
            exporter.exportSceneToFile(scene)
        }
        // TODO: progress indicator hooks
        // TODO: directory option
        // TODO: flat export option
    }

    fun exportRadius(regionId: Int, radius: Int) {
        scene.loadRadius(regionId, radius)
    }
}
