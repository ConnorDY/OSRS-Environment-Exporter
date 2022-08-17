package models.scene

import cache.LocationType
import cache.definitions.Location
import cache.definitions.LocationsDefinition
import cache.definitions.OverlayDefinition
import cache.definitions.RegionDefinition
import cache.definitions.UnderlayDefinition
import controllers.worldRenderer.Constants
import controllers.worldRenderer.entities.Entity
import controllers.worldRenderer.entities.FloorDecoration
import controllers.worldRenderer.entities.GameObject
import controllers.worldRenderer.entities.TileModel
import controllers.worldRenderer.entities.TilePaint
import controllers.worldRenderer.entities.WallDecoration
import controllers.worldRenderer.entities.WallObject

class SceneRegion(val locationsDefinition: LocationsDefinition) {
    val tiles = Array(RegionDefinition.Z) { Array(RegionDefinition.X) { arrayOfNulls<SceneTile>(RegionDefinition.Y) } }
    val tileBrightness = Array(RegionDefinition.X + 1) { IntArray(RegionDefinition.Y + 1) }

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
        tile.gameObjects.add(GameObject(entity))
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

    companion object {
        val displacementX = intArrayOf(1, 0, -1, 0) // field456
        val displacementY = intArrayOf(0, -1, 0, 1) // field457
        val diagonalDisplacementX = intArrayOf(1, -1, -1, 1) // field458
        val diagonalDisplacementY = intArrayOf(-1, -1, 1, 1) // field459
    }
}
