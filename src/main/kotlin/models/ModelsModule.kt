package models

import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.Provider
import models.scene.Scene
import models.scene.SceneRegionBuilder
import javax.inject.Singleton

class ModelsModule : AbstractModule() {
    override fun configure() {
        bind(DebugModel::class.java).toInstance(DebugModel())
        bind(ObjectsModel::class.java).toInstance(ObjectsModel())
        bind(Scene::class.java).toProvider(SceneProvider::class.java)
    }
}

@Singleton
internal class SceneProvider @Inject constructor(sceneRegionBuilder: SceneRegionBuilder) :
    Provider<Scene> {
    private val scene: Scene = Scene(sceneRegionBuilder)
    override fun get(): Scene {
        return scene
    }
}
