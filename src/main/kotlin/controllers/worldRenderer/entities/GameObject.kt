package controllers.worldRenderer.entities

class GameObject(
    val entity: Entity
) {
    override fun toString(): String {
        return "entity: $entity"
    }
}