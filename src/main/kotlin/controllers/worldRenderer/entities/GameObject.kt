package controllers.worldRenderer.entities

class GameObject(
    val entity: Entity,
    val xWidth: Int,
    val yLength: Int,
) {
    override fun toString(): String {
        return "entity: $entity"
    }
}
