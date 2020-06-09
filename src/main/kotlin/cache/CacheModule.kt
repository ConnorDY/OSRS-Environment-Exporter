package cache

import com.displee.cache.CacheLibrary
import com.google.inject.AbstractModule
import com.google.inject.Provider
import com.google.inject.Singleton
import java.io.IOException

class CacheModule : AbstractModule() {
    override fun configure() {
        bind(CacheLibrary::class.java).toProvider(CacheLibraryProvider::class.java)
        bind(XteaManager::class.java).toProvider(XteaManagerProvider::class.java)
    }
}

@Singleton
class CacheLibraryProvider : Provider<CacheLibrary?> {
    private var library: CacheLibrary? = null

    @Throws(IOException::class)
    fun setLibraryLocation(f: String) {
        library = CacheLibrary(f)
    }

    override fun get(): CacheLibrary? {
        if (library == null) {
            library = CacheLibrary("cache-out")
        }
        return library
    }
}

@Singleton
class XteaManagerProvider : Provider<XteaManager> {
    private var xteaManager: XteaManager? = null

    @Throws(IOException::class)
    fun setXteaLocation(path: String) {
        xteaManager = XteaManager(path)
    }

    override fun get(): XteaManager? {
        if (xteaManager == null) {
            xteaManager = XteaManager("cache-out")
        }
        return xteaManager
    }
}
