package controllers.worldRenderer

import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.Provider
import com.google.inject.Singleton
import models.DebugModel
import models.scene.Scene

class WorldRendererModule : AbstractModule() {
    override fun configure() {
        bind(Camera::class.java).toInstance(Camera())
        bind(Renderer::class.java).toProvider(RendererProvider::class.java)
    }
}

@Singleton
class RendererProvider @Inject constructor(
    camera: Camera,
    scene: Scene,
    sceneUploader: SceneUploader,
    inputHandler: InputHandler,
    textureManager: TextureManager,
    debugModel: DebugModel
) : Provider<Renderer> {
    private val renderer = Renderer(camera, scene, sceneUploader, inputHandler, textureManager, debugModel)
    override fun get() = renderer
}
