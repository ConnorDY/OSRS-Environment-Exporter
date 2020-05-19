package controllers.worldRenderer.entities

class StaticObject(private val model: Model, height: Int) : Entity(height) {
    override fun getModel(): Model {
        return model
    }
}