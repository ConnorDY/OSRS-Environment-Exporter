package controllers.worldRenderer.entities

class TilePaint(
    val swHeight: Int,
    val seHeight: Int,
    val neHeight: Int,
    val nwHeight: Int,
    val swColor: Int,
    val seColor: Int,
    val neColor: Int,
    val nwColor: Int,
    val texture: Int,
) : Renderable {
    override val computeObj = ComputeObj()
    override val renderUnordered get() = true
    override val faceCount get() = 2
}
