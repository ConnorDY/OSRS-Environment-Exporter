package models.scene

import cache.LocationType
import cache.definitions.Location
import cache.definitions.LocationsDefinition
import cache.definitions.OverlayDefinition
import cache.definitions.RegionDefinition
import cache.definitions.RegionDefinition.Companion.X
import cache.definitions.RegionDefinition.Companion.Y
import cache.definitions.RegionDefinition.Companion.Z
import cache.definitions.UnderlayDefinition
import cache.loaders.RegionLoader
import cache.loaders.getTileHeight
import controllers.worldRenderer.Constants
import controllers.worldRenderer.entities.Entity
import controllers.worldRenderer.entities.FloorDecoration
import controllers.worldRenderer.entities.GameObject
import controllers.worldRenderer.entities.Model
import controllers.worldRenderer.entities.TileModel
import controllers.worldRenderer.entities.TilePaint
import controllers.worldRenderer.entities.WallDecoration
import controllers.worldRenderer.entities.WallObject

class SceneRegion(val locationsDefinition: LocationsDefinition) {
    val tiles = Array(Z) { Array(X) { arrayOfNulls<SceneTile>(Y) } }
    val tileBrightness = Array(X + 1) { IntArray(Y + 1) }

    fun addTile(
        z: Int,
        x: Int,
        y: Int,
        overlayPath: Int,
        overlayRotation: Int,
        overlayTexture: Int,
        swHeight: Int,
        seHeight: Int,
        neHeight: Int,
        nwHeight: Int,
        swColor: Int,
        seColor: Int,
        neColor: Int,
        nwColor: Int,
        swColorB: Int,
        seColorB: Int,
        neColorB: Int,
        nwColorB: Int,
        underlayDefinition: UnderlayDefinition?,
        overlayDefinition: OverlayDefinition?,
        cacheTile: RegionDefinition.Tile
    ) {
        when (overlayPath) {
            0 -> {
                ensureTile(z, x, y).tilePaint = TilePaint(
                    swHeight,
                    seHeight,
                    neHeight,
                    nwHeight,
                    swColor,
                    seColor,
                    neColor,
                    nwColor,
                    -1
                )
            }
            1 -> {
                ensureTile(z, x, y).tilePaint = TilePaint(
                    swHeight,
                    seHeight,
                    neHeight,
                    nwHeight,
                    swColorB,
                    seColorB,
                    neColorB,
                    nwColorB,
                    overlayTexture
                )
            }
            else -> {
                ensureTile(z, x, y).tileModel =
                    TileModel(
                        overlayPath,
                        overlayRotation,
                        overlayTexture,
                        x,
                        y,
                        swHeight,
                        seHeight,
                        neHeight,
                        nwHeight,
                        swColor,
                        seColor,
                        neColor,
                        nwColor,
                        swColorB,
                        seColorB,
                        neColorB,
                        nwColorB
                    )
            }
        }
        tiles[z][x][y]?.underlayDefinition = underlayDefinition
        tiles[z][x][y]?.overlayDefinition = overlayDefinition
        tiles[z][x][y]?.cacheTile = cacheTile
    }

    fun newFloorDecoration(z: Int, x: Int, y: Int, entity: Entity) {
        val tile = ensureTile(z, x, y)
        entity.model.offsetX = Constants.LOCAL_HALF_TILE_SIZE
        entity.model.offsetY = Constants.LOCAL_HALF_TILE_SIZE
        tile.floorDecoration = FloorDecoration(entity)
    }

    fun newWallDecoration(
        z: Int,
        x: Int,
        y: Int,
        entity: Entity,
        entity2: Entity? = null,
        displacementX: Int = 0,
        displacementY: Int = 0,
    ) {
        val tile = ensureTile(z, x, y)

        entity.setDecoDisplacements(displacementX, displacementY)
        if (entity2 != null) {
            val model = entity2.model
            model.offsetX = Constants.LOCAL_HALF_TILE_SIZE
            model.offsetY = Constants.LOCAL_HALF_TILE_SIZE
            // don't set displacementX/Y on entity2
        }

        tile.wallDecoration = WallDecoration(entity, entity2)
    }

    private fun Entity.setDecoDisplacements(
        displacementX: Int,
        displacementY: Int
    ) {
        val model = model
        model.offsetX = Constants.LOCAL_HALF_TILE_SIZE
        model.offsetY = Constants.LOCAL_HALF_TILE_SIZE
        model.wallAttachedOffsetX = displacementX
        model.wallAttachedOffsetY = displacementY
    }

    fun getWallDisplacement(
        loc: Location,
        z: Int,
        x: Int,
        y: Int
    ): Pair<Int, Int> {
        val displacement = ensureTile(z, x, y).wallDisplacement
        val displacementX = displacementX[loc.orientation] * displacement
        val displacementY = displacementY[loc.orientation] * displacement
        return Pair(displacementX, displacementY)
    }

    fun getWallDiagonalDisplacement(
        loc: Location,
        z: Int,
        x: Int,
        y: Int
    ): Pair<Int, Int> {
        val displacement = ensureTile(z, x, y).wallDisplacement
        val displacementX = diagonalDisplacementX[loc.orientation] * displacement
        val displacementY = diagonalDisplacementY[loc.orientation] * displacement
        return Pair(displacementX, displacementY)
    }

    fun newWall(
        z: Int,
        x: Int,
        y: Int,
        width: Int,
        length: Int,
        entity: Entity,
        entity2: Entity?,
        location: Location
    ) {
        val tile = ensureTile(z, x, y)

        entity.model.offsetX = width * REGION_SIZE
        entity.model.offsetY = length * REGION_SIZE
        entity2?.model?.offsetX = width * REGION_SIZE
        entity2?.model?.offsetY = length * REGION_SIZE
        tile.wall = WallObject(entity, entity2, LocationType.fromId(location.type)!!)
        tile.locations.add(location)

        rescaleWall(tile, entity)
    }

    fun newPseudoWall(
        z: Int,
        x: Int,
        y: Int,
        width: Int,
        length: Int,
        entity: Entity,
        location: Location
    ) {
        newGameObject(z, x, y, width, length, entity, location)
        rescaleWall(tiles[z][x][y]!!, entity)
    }

    private fun rescaleWall(
        tile: SceneTile,
        entity: Entity
    ) {
        val wallDecoration = tile.wallDecoration
        val wallDisplacement = entity.objectDefinition.decorDisplacement
        tile.wallDisplacement = wallDisplacement
        if (wallDecoration != null) {
            val model = wallDecoration.entity.model
            model.wallAttachedOffsetX = wallDisplacement * model.wallAttachedOffsetX / 16
            model.wallAttachedOffsetY = wallDisplacement * model.wallAttachedOffsetY / 16
        }
    }

    fun newGameObject(
        z: Int,
        x: Int,
        y: Int,
        width: Int,
        length: Int,
        entity: Entity,
        location: Location
    ) {
        val tile = ensureTile(z, x, y)

        entity.model.offsetX = width * REGION_SIZE
        entity.model.offsetY = length * REGION_SIZE
        tile.gameObjects.add(GameObject(entity, x, y, width, length))
        tile.locations.add(location)
    }

    private fun ensureTile(z: Int, x: Int, y: Int): SceneTile {
        for (iz in z downTo 0) {
            if (tiles[iz][x][y] == null) {
                tiles[iz][x][y] = SceneTile(iz, x, y)
            }
        }
        return tiles[z][x][y]!!
    }

    fun applyLighting(regionLoader: RegionLoader) {
        for (z in 0 until Z) {
            for (x in 0 until X) {
                for (y in 0 until Y) {
                    val tile = tiles[z][x][y]
                    if (tile != null) {
                        val wall = tile.wall
                        if (wall != null && !wall.entity.model.isLit) {
                            val model1 = wall.entity.model
                            model1.mergeLargeObjectNormals(regionLoader, z, x, y, 1, 1)

                            val model2 = wall.entity2?.model
                            if (model2 != null && !model2.isLit) {
                                model2.mergeLargeObjectNormals(regionLoader, z, x, y, 1, 1)
                                model1.mergeNormals(model2, 0, 0, 0, false)
                                model2.light()
                            }
                            model1.light()
                        }

                        for (gameObject in tile.gameObjects) {
                            val model = gameObject.entity.model
                            if (!model.isLit) {
                                model.mergeLargeObjectNormals(regionLoader, z, x, y, gameObject.xWidth, gameObject.yLength)
                                model.light()
                            }
                        }

                        val floorDecoration = tile.floorDecoration
                        if (floorDecoration != null && !floorDecoration.entity.model.isLit) {
                            val model = floorDecoration.entity.model
                            model.mergeFloorNormalsNorthEast(z, x, y)
                            model.light()
                        }
                    }
                }
            }
        }
    }

    private fun Model.mergeFloorNormalsNorthEast(z: Int, x: Int, y: Int) {
        // TODO these bounds were all off by one in the original code :S
        if (x < X - 1) {
            val model2 = tiles[z][x + 1][y]?.floorDecoration?.entity?.model
            if (model2 != null && !model2.isLit) {
                mergeNormals(model2, 128, 0, 0, true)
            }
        }
        if (y < Y - 1) {
            val model2 = tiles[z][x][y + 1]?.floorDecoration?.entity?.model
            if (model2 != null && !model2.isLit) {
                mergeNormals(model2, 0, 0, 128, true)
            }
        }
        if (x < X - 1 && y < Y - 1) {
            val model2 = tiles[z][x + 1][y + 1]?.floorDecoration?.entity?.model
            if (model2 != null && !model2.isLit) {
                mergeNormals(model2, 128, 0, 128, true)
            }
        }
        if (x < X - 1 && y > 0) {
            val model2 = tiles[z][x + 1][y - 1]?.floorDecoration?.entity?.model
            if (model2 != null && !model2.isLit) {
                mergeNormals(model2, 128, 0, -128, true)
            }
        }
    }

    private fun Model.mergeLargeObjectNormals(
        regionLoader: RegionLoader,
        z: Int,
        x: Int,
        y: Int,
        width: Int,
        length: Int
    ) {
        var hideOccludedFaces = true
        var xMin = x
        val xMax = x + width
        val yMin = y - 1
        val yMax = y + length
        val regionTileX = locationsDefinition.regionId.shr(8).and(0xFF).shl(6)
        val regionTileY = locationsDefinition.regionId.and(0xFF).shl(6)
        val surroundingHeight = (
            regionLoader.getTileHeight(z, regionTileX + x + 1, regionTileY + y) +
                regionLoader.getTileHeight(z, regionTileX + x + 1, regionTileY + y + 1) +
                regionLoader.getTileHeight(z, regionTileX + x, regionTileY + y + 1) +
                regionLoader.getTileHeight(z, regionTileX + x, regionTileY + y)
            ) / 4
        for (zi in z..z + 1) {
            if (zi == Z) continue
            for (xi in xMin..xMax) {
                if (xi !in 0 until X) continue
                for (yi in yMin..yMax) {
                    if (yi !in 0 until Y) continue
                    if (hideOccludedFaces && xi < xMax && yi < yMax && (yi >= y || x == xi)) continue
                    val tile = tiles[zi][xi][yi] ?: continue
                    val thisSurroundingHeight = (
                        regionLoader.getTileHeight(zi, regionTileX + xi + 1, regionTileY + yi) +
                            regionLoader.getTileHeight(zi, regionTileX + xi + 1, regionTileY + yi + 1) +
                            regionLoader.getTileHeight(zi, regionTileX + xi, regionTileY + yi + 1) +
                            regionLoader.getTileHeight(zi, regionTileX + xi, regionTileY + yi)
                        ) / 4
                    val heightDiff = thisSurroundingHeight - surroundingHeight
                    val wall = tile.wall
                    if (wall != null) {
                        val model1 = wall.entity.model
                        if (!model1.isLit) {
                            mergeNormals(
                                model1,
                                (1 - width) * 64 + (xi - x) * 128,
                                heightDiff,
                                (yi - y) * 128 + (1 - length) * 64,
                                hideOccludedFaces
                            )
                        }
                        val model2 = wall.entity2?.model
                        if (model2 != null && !model2.isLit) {
                            mergeNormals(
                                model2,
                                (1 - width) * 64 + (xi - x) * 128,
                                heightDiff,
                                (yi - y) * 128 + (1 - length) * 64,
                                hideOccludedFaces
                            )
                        }
                    }

                    for (gameObject in tile.gameObjects) {
                        val model = gameObject.entity.model
                        if (!model.isLit) {
                            val distanceOtherX = gameObject.x - x
                            val distanceOtherY = gameObject.y - y
                            val widthDiff = gameObject.xWidth - width
                            val lengthDiff = gameObject.yLength - length
                            mergeNormals(
                                model,
                                distanceOtherX * 128 + widthDiff * 64,
                                heightDiff,
                                distanceOtherY * 128 + lengthDiff * 64,
                                hideOccludedFaces
                            )
                        }
                    }
                }
            }
            --xMin
            hideOccludedFaces = false
        }
    }

    companion object {
        val displacementX = intArrayOf(1, 0, -1, 0)
        val displacementY = intArrayOf(0, -1, 0, 1)
        val diagonalDisplacementX = intArrayOf(1, -1, -1, 1)
        val diagonalDisplacementY = intArrayOf(-1, -1, 1, 1)
    }
}
