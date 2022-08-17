package controllers.worldRenderer.entities

class GameObject(
    val entity: Entity,
    val x: Int,
    val y: Int,
    val xWidth: Int,
    val yLength: Int,
) {
    override fun toString(): String {
        return "entity: $entity"
    }
}
