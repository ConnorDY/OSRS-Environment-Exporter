package controllers.worldRenderer.entities

abstract class Entity(val height: Int = 0, val type: Int, val orientation: Int) {
    abstract fun getModel(): Model

    override fun toString(): String {
        return "$javaClass, Height: $height, type $type, orientation $orientation"
    }
}