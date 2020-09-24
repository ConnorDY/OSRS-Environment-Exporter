package models

import cache.definitions.converters.ObjectToModelConverter
import cache.loaders.OverlayLoader
import cache.loaders.RegionLoader
import cache.loaders.TextureLoader
import cache.loaders.UnderlayLoader
import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.Provider
import models.scene.Scene
import models.scene.SceneRegionBuilder
import javax.inject.Singleton

class ModelsModule : AbstractModule() {
    override fun configure() {
        bind(Configuration::class.java).toInstance(Configuration())
        bind(DebugModel::class.java).toInstance(DebugModel())
        bind(ObjectsModel::class.java).toInstance(ObjectsModel())
        bind(HoverModel::class.java).toInstance(HoverModel())
        bind(Scene::class.java).toProvider(SceneProvider::class.java)
    }
}

@Singleton
internal class SceneProvider @Inject constructor(
    sceneRegionBuilder: SceneRegionBuilder,
    underlayLoader: UnderlayLoader,
    regionLoader: RegionLoader,
    objectToModelConverter: ObjectToModelConverter,
    overlayLoader: OverlayLoader,
    textureLoader: TextureLoader
) :
    Provider<Scene> {
    private val scene: Scene = Scene(sceneRegionBuilder, underlayLoader, regionLoader, objectToModelConverter, overlayLoader, textureLoader)
    override fun get(): Scene {
        return scene
    }
}
