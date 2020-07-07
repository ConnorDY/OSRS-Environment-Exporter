package controllers.worldRenderer.entities

class StaticObject(private val model: Model, height: Int, type: Int, orientation: Int) : Entity(height, type, orientation) {
    override fun getModel(): Model {
        return model
    }
}