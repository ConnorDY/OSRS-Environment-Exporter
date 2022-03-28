package models.scene

import cache.LocationType
import cache.definitions.*
import controllers.worldRenderer.Constants
import controllers.worldRenderer.entities.*

class SceneRegion(val regionDefinition: RegionDefinition, val locationsDefinition: LocationsDefinition) {
    val tiles = Array(RegionDefinition.Z) { Array(RegionDefinition.X) { arrayOfNulls<SceneTile>(RegionDefinition.Y) } }
    val tileColors = Array(RegionDefinition.X + 1) { IntArray(RegionDefinition.Y + 1) }

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
        underlayRgb: Int,
        overlayRgb: Int,
        underlayDefinition: UnderlayDefinition?,
        overlayDefinition: OverlayDefinition?,
        cacheTile: RegionDefinition.Tile?
    ) {
        when {
            overlayPath == 0 -> {
                for (iz in z downTo 0) {
                    if (tiles[iz][x][y] == null) {
                        tiles[iz][x][y] = SceneTile(iz, x, y)
                    }
                }
                tiles[z][x][y]!!.tilePaint = TilePaint(
                    swHeight,
                    seHeight,
                    neHeight,
                    nwHeight,
                    swColor,
                    seColor,
                    neColor,
                    nwColor,
                    -1,
                    underlayRgb
                )
            }
            overlayPath == 1 -> {
                for (iz in z downTo 0) {
                    if (tiles[iz][x][y] == null) {
                        tiles[iz][x][y] = SceneTile(iz, x, y)
                    }
                }
                tiles[z][x][y]!!.tilePaint = TilePaint(
                    swHeight,
                    seHeight,
                    neHeight,
                    nwHeight,
                    swColorB,
                    seColorB,
                    neColorB,
                    nwColorB,
                    overlayTexture,
                    overlayRgb
                )
            }
            else -> {
                for (iz in z downTo 0) {
                    if (tiles[iz][x][y] == null) {
                        tiles[iz][x][y] = SceneTile(iz, x, y)
                    }
                }
                tiles[z][x][y]!!.tileModel =
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
                        nwColorB,
                        underlayRgb,
                        overlayRgb
                    )
            }
        }
        tiles[z][x][y]?.underlayDefinition = underlayDefinition
        tiles[z][x][y]?.overlayDefinition = overlayDefinition
        tiles[z][x][y]?.cacheTile = cacheTile
    }

    fun newFloorDecoration(z: Int, x: Int, y: Int, entity: Entity?) {
        for (iz in z downTo 0) {
            if (tiles[iz][x][y] == null) {
                tiles[iz][x][y] = SceneTile(iz, x, y)
            }
        }
        entity!!.getModel().xOff = Constants.LOCAL_HALF_TILE_SIZE
        entity.getModel().yOff = Constants.LOCAL_HALF_TILE_SIZE
        val floorDecoration = FloorDecoration(entity)
        tiles[z][x][y]!!.floorDecoration = floorDecoration
    }

    fun newWallDecoration(
        z: Int,
        x: Int,
        y: Int,
        entity: Entity?
    ) {
        for (iz in z downTo 0) {
            if (tiles[iz][x][y] == null) {
                tiles[iz][x][y] = SceneTile(iz, x, y)
            }
        }

        entity!!.getModel().xOff = Constants.LOCAL_HALF_TILE_SIZE
        entity.getModel().yOff = Constants.LOCAL_HALF_TILE_SIZE
        val wallDecoration = WallDecoration(entity)
        tiles[z][x][y]!!.wallDecoration = wallDecoration
    }

    fun newWall(
        z: Int,
        x: Int,
        y: Int,
        width: Int,
        length: Int,
        entity: Entity?,
        entity2: Entity?,
        location: Location
    ) {
        for (iz in z downTo 0) {
            if (tiles[iz][x][y] == null) {
                tiles[iz][x][y] = SceneTile(iz, x, y)
            }
        }

        entity!!.getModel().xOff = width * REGION_SIZE
        entity.getModel().yOff = length * REGION_SIZE
        entity2?.getModel()?.xOff = width * REGION_SIZE
        entity2?.getModel()?.yOff = length * REGION_SIZE
        tiles[z][x][y]!!.wall = WallObject(entity, entity2, LocationType.fromId(location.type)!!)
        tiles[z][x][y]!!.locations.add(location)
    }

    fun newGameObject(
        z: Int,
        x: Int,
        y: Int,
        width: Int,
        length: Int,
        entity: Entity?,
        location: Location
    ) {
        for (iz in z downTo 0) {
            if (tiles[iz][x][y] == null) {
                tiles[iz][x][y] = SceneTile(iz, x, y)
            }
        }

        entity!!.getModel().xOff = width * REGION_SIZE
        entity.getModel().yOff = length * REGION_SIZE
        tiles[z][x][y]!!.gameObjects.add(GameObject(entity))
        tiles[z][x][y]!!.locations.add(location)
    }
}