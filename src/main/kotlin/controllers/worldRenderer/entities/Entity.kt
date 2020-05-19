package controllers.worldRenderer.entities

abstract class Entity(val height: Int = 0) {
    abstract fun getModel(): Model
}