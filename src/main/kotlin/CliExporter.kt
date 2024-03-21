import cache.ParamsManager
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
import controllers.main.CountingSceneLoadProgressListener
import controllers.worldRenderer.SceneExporter
import controllers.worldRenderer.TextureManager
import models.DebugOptionsModel
import models.StartupOptions
import models.scene.Scene
import models.scene.SceneRegionBuilder
import utils.ChunkWriteListener
import utils.ProgressContainer

class CliExporter(startupOptions: StartupOptions) {
    val scene: Scene
    val exporter: SceneExporter

    init {
        val cacheLibrary = CacheLibrary("${startupOptions.cacheDir}/cache")
        val xteaManager = XteaManager(startupOptions.cacheDir)
        val paramsManager = ParamsManager()
        paramsManager.loadFromPath(startupOptions.cacheDir)

        val spriteLoader = SpriteLoader(cacheLibrary)
        val textureLoader = TextureLoader(cacheLibrary)
        val regionLoader = RegionLoader(cacheLibrary, paramsManager)
        val locationsLoader = LocationsLoader(cacheLibrary, xteaManager)
        val objectLoader = ObjectLoader(cacheLibrary)
        val underlayLoader = UnderlayLoader(cacheLibrary)
        val overlayLoader = OverlayLoader(cacheLibrary)
        val modelLoader = ModelLoader(cacheLibrary)

        val debugOptionsModel = DebugOptionsModel()
        debugOptionsModel.setZLevelsFromList(startupOptions.enabledZLayers)

        val textureManager = TextureManager(spriteLoader, textureLoader)
        val objectToModelConverter = ObjectToModelConverter(modelLoader, debugOptionsModel)
        val sceneRegionBuilder = SceneRegionBuilder(regionLoader, locationsLoader, objectLoader, underlayLoader, overlayLoader, objectToModelConverter)

        scene = Scene(sceneRegionBuilder, debugOptionsModel)
        exporter = SceneExporter(textureManager, debugOptionsModel)

        scene.sceneChangeListeners.add {
            // Export the scene once it has been loaded.
            exporter.exportSceneToFile(
                scene,
                startupOptions.exportDir,
                startupOptions.exportFlat,
                startupOptions.scaleFactor
            )
        }

        // Listen for progress updates
        scene.sceneLoadProgressListeners.add(object : CountingSceneLoadProgressListener() {
            override val progressContainer: ProgressContainer get() = CliProgressContainer
            override val statusDoing: String get() = "Loading region $currentRegion of $numRegions"
            override val statusDone: String get() = "Loaded regions"
        })
        exporter.sceneLoadProgressListeners.add(object : CountingSceneLoadProgressListener() {
            override val progressContainer: ProgressContainer get() = CliProgressContainer
            override val statusDoing: String get() = "Exporting region $currentRegion of $numRegions"
            override val statusDone: String get() = "Exported regions"
        })
        exporter.chunkWriteListeners.add(object : ChunkWriteListener {
            override fun onStartRegion(regionNum: Int) {
                if (regionNum == -1)
                    println("Writing JSON")
                else
                    println("Writing region $regionNum")
            }

            override fun onEndRegion() {}

            override fun onFinishWriting() {
                println("Done!")
            }
        })
    }

    fun exportRadius(regionId: Int, radius: Int) {
        scene.loadRadius(regionId, radius)
    }

    object CliProgressContainer : ProgressContainer {
        override var progress: Int = 0
        override var progressMax: Int = 100
        override var status: String
            get() = ""
            set(value) {
                println(value)
            }
        override var isCancelled: Boolean
            get() = false
            set(value) {
                throw UnsupportedOperationException("Cannot cancel from CLI")
            }

        override fun complete() {}
    }
}
