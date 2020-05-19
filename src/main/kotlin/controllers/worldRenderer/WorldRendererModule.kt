package controllers.worldRenderer

import com.google.inject.AbstractModule

class WorldRendererModule : AbstractModule() {
    override fun configure() {
        bind(Camera::class.java).toInstance(Camera())
    }
}