package controllers.worldRenderer

import cache.definitions.RegionDefinition.Companion.X
import cache.definitions.RegionDefinition.Companion.Y
import cache.definitions.RegionDefinition.Companion.Z
import controllers.worldRenderer.Constants.LOCAL_TILE_SIZE
import controllers.worldRenderer.entities.Entity
import models.math.Ray
import models.scene.REGION_SIZE
import models.scene.Scene
import models.scene.SceneRegion
import org.joml.Intersectionf
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f

class ClickHandler(
    private val scene: Scene,
    private val inputHandler: InputHandler,
) {
    fun handle(viewProjectionMatrix: Matrix4f, width: Int, height: Int) {
        if (!inputHandler.mouseClicked) return
        inputHandler.mouseClicked = false

        val x = inputHandler.mouseX
        val y = inputHandler.mouseY

        val invViewProjectionMatrix = Matrix4f(viewProjectionMatrix).invert()
        val ray = Ray.fromScreenCoordinates(x, y, width, height, invViewProjectionMatrix)

        ray.getAllEntityHits().minByOrNull { it.first }?.let { (dist, entity) ->
            val name = entity.objectDefinition.name
            val id = entity.objectDefinition.id
            println("Clicked $name ($id) at distance $dist")
        }
    }

    private fun Ray.getAllEntityHits(): List<Pair<Float, Entity>> {
        val hits = mutableListOf<Pair<Float, Entity>>()
        withIntersectingRegions { region, regionPos ->
            // Now actually check if the ray hit any entity
            withPotentialEntityHits(region, regionPos, this) { entity, entityPos ->
                // Check if it hit any of the entity's geometry
                val hit = intersectsEntity(entity, entityPos)
                if (hit >= 0f)
                    hits.add(hit to entity)
            }
        }
        return hits
    }

    private inline fun Ray.withIntersectingRegions(
        process: (SceneRegion, Vector3f) -> Unit
    ) {
        val regionSize = (REGION_SIZE * LOCAL_TILE_SIZE).toFloat()
        val aabbMin = Vector3f()
        val aabbMax = Vector3f()
        val intersection = Vector2f()
        for (regionY in 0 until scene.rows) {
            for (regionX in 0 until scene.cols) {
                val region = scene.getRegion(regionX, regionY) ?: continue

                // Check if the ray hit the region
                aabbMin.set(regionX * regionSize, -regionSize, regionY * regionSize)
                aabbMin.add(regionSize, regionSize, regionSize, aabbMax)
                if (!Intersectionf.intersectRayAab(origin, direction, aabbMin, aabbMax, intersection))
                    continue

                // Reuse this vector for region coordinate offset
                // Y offset is taken from bbox max because Y is inverted
                aabbMin.y = aabbMax.y
                process(region, aabbMin)
            }
        }
    }

    private fun Ray.intersectsEntity(
        entity: Entity,
        entityPos: Vector3f
    ): Float {
        val trianglePoint1 = Vector3f()
        val trianglePoint2 = Vector3f()
        val trianglePoint3 = Vector3f()

        val model = entity.model
        val vertexX = model.vertexPositionsX
        val vertexY = model.vertexPositionsY
        val vertexZ = model.vertexPositionsZ
        for (face in 0 until model.faceCount) {
            val ind1 = model.modelDefinition.faceVertexIndices1[face]
            trianglePoint1.set(
                vertexX[ind1].toFloat(),
                vertexY[ind1].toFloat(),
                vertexZ[ind1].toFloat(),
            )
            val ind2 = model.modelDefinition.faceVertexIndices2[face]
            trianglePoint2.set(
                vertexX[ind2].toFloat(),
                vertexY[ind2].toFloat(),
                vertexZ[ind2].toFloat(),
            )
            val ind3 = model.modelDefinition.faceVertexIndices3[face]
            trianglePoint3.set(
                vertexX[ind3].toFloat(),
                vertexY[ind3].toFloat(),
                vertexZ[ind3].toFloat(),
            )
            trianglePoint1.add(entityPos)
            trianglePoint2.add(entityPos)
            trianglePoint3.add(entityPos)
            val dist = Intersectionf.intersectRayTriangle(
                origin,
                direction,
                trianglePoint1,
                trianglePoint2,
                trianglePoint3,
                0.001f,
            )
            if (dist >= 0f) {
                // Note: this might not be the closest face on this model,
                // but it's generally not an issue
                return dist
            }
        }
        return -1.0f
    }

    private inline fun withPotentialEntityHits(
        region: SceneRegion,
        regionPos: Vector3f,
        ray: Ray,
        process: (Entity, Vector3f) -> Unit
    ) {
        val tilePos = Vector3f()
        val entityPos = Vector3f()
        val intersection = Vector2f()
        for (tz in 0 until Z) {
            for (tx in 0 until X) {
                for (ty in 0 until Y) {
                    val tile = region.tiles[tz][tx][ty] ?: continue
                    tilePos.set(
                        (tx * LOCAL_TILE_SIZE).toFloat(),
                        0f,
                        (ty * LOCAL_TILE_SIZE).toFloat(),
                    )
                    for (entity in tile.allEntities) {
                        entityPos.set(
                            entity.model.renderOffsetX.toFloat(),
                            (entity.height + entity.model.renderOffsetY).toFloat(),
                            entity.model.renderOffsetZ.toFloat()
                        )
                        entityPos.add(tilePos).add(regionPos)
                        if (Intersectionf.intersectRaySphere(
                                ray.origin,
                                ray.direction,
                                entityPos,
                                entity.model.boundingSphereRadiusSq.toFloat(),
                                intersection
                            )
                        )
                            process(entity, entityPos)
                    }
                }
            }
        }
    }
}
