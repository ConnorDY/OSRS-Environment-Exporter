package cache.loaders

import cache.XteaManager
import com.displee.cache.CacheLibrary
import com.google.inject.*
import javax.inject.Singleton

class LoaderModule : AbstractModule() {
    override fun configure() {
        bind(RegionLoader::class.java).toProvider(RegionLoaderProvider::class.java)
        bind(ModelLoader::class.java).toProvider(ModelLoaderProvider::class.java)
        bind(LocationsLoader::class.java).toProvider(LocationsLoaderProvider::class.java)
        bind(ObjectLoader::class.java).toProvider(ObjectLoaderProvider::class.java)
        bind(SpriteLoader::class.java).toProvider(SpriteLoaderProvider::class.java)
        bind(TextureLoader::class.java).toProvider(TextureLoaderProvider::class.java)
        bind(UnderlayLoader::class.java).toProvider(UnderlayLoaderProvider::class.java)
        bind(OverlayLoader::class.java).toProvider(OverlayLoaderProvider::class.java)
    }
}

@Singleton
internal class RegionLoaderProvider @Inject constructor(library: CacheLibrary) :
    Provider<RegionLoader> {
    private val regionLoader: RegionLoader = RegionLoader(library)
    override fun get(): RegionLoader {
        return regionLoader
    }
}

@Singleton
internal class ModelLoaderProvider @Inject constructor(library: CacheLibrary) :
    Provider<ModelLoader> {
    private val modelLoader: ModelLoader = ModelLoader(library)
    override fun get(): ModelLoader {
        return modelLoader
    }
}

@Singleton
internal class LocationsLoaderProvider @Inject constructor(library: CacheLibrary, xteaManager: XteaManager) :
    Provider<LocationsLoader> {
    private val locationsLoader: LocationsLoader = LocationsLoader(library, xteaManager)
    override fun get(): LocationsLoader {
        return locationsLoader
    }
}

@Singleton
internal class ObjectLoaderProvider @Inject constructor(library: CacheLibrary) :
    Provider<ObjectLoader> {
    private val objectLoader: ObjectLoader = ObjectLoader(library)
    override fun get(): ObjectLoader {
        return objectLoader
    }
}

@Singleton
internal class SpriteLoaderProvider @Inject constructor(library: CacheLibrary) :
    Provider<SpriteLoader> {
    private val spriteLoader: SpriteLoader = SpriteLoader(library)
    override fun get(): SpriteLoader {
        return spriteLoader
    }
}

@Singleton
internal class TextureLoaderProvider @Inject constructor(library: CacheLibrary) :
    Provider<TextureLoader> {
    private val textureLoader: TextureLoader = TextureLoader(library)
    override fun get(): TextureLoader {
        return textureLoader
    }
}

@Singleton
internal class UnderlayLoaderProvider @Inject constructor(library: CacheLibrary) :
    Provider<UnderlayLoader> {
    private val underlayLoader: UnderlayLoader = UnderlayLoader(library)
    override fun get(): UnderlayLoader {
        return underlayLoader
    }
}

@Singleton
internal class OverlayLoaderProvider @Inject constructor(library: CacheLibrary) :
    Provider<OverlayLoader> {
    private val overlayLoader: OverlayLoader = OverlayLoader(library)
    override fun get(): OverlayLoader {
        return overlayLoader
    }
}