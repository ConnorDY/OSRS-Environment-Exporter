package models.scene

interface SceneLoadProgressListener {
    fun onBeginLoadingRegions(count: Int)
    fun onRegionLoaded()
    fun onError()
}
