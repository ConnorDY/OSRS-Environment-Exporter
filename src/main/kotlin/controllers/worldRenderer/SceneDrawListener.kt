package controllers.worldRenderer

interface SceneDrawListener {
    fun onStartDraw()
    fun onEndDraw()
    fun onError()
}
