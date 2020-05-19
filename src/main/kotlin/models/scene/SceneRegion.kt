package models.scene

import cache.definitions.LocationsDefinition
import cache.definitions.RegionDefinition
import cache.definitions.UnderlayDefinition
import controllers.worldRenderer.entities.*


class SceneRegion(val regionDefinition: RegionDefinition, val locationsDefinition: LocationsDefinition) {
    val tiles = Array(RegionDefinition.Z) { Array(RegionDefinition.X) { arrayOfNulls<SceneTile>(RegionDefinition.Y) } }
    val tileColors = Array(RegionDefinition.X + 1) { IntArray(RegionDefinition.Y + 1) }
//    private val tileHeights: Array<Array<IntArray>>
//    private val tileSettings: Array<Array<ByteArray>>
//    private val overlayIds: Array<Array<ByteArray>>
//    private val overlayPaths: Array<Array<ByteArray>>
//    private val overlayRotations: Array<Array<ByteArray>>
//    private val underlayIds: Array<Array<ByteArray>>

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
        var15: Int,
        var16: Int,
        var17: Int,
        var18: Int,
        rgb: Int,
        overlayRgb: Int,
        underlayDefinition: UnderlayDefinition?
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
                    rgb,
                    underlayDefinition
                )
            }
            overlayPath != 1 -> {
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
                        var15,
                        var16,
                        var17,
                        var18,
                        rgb,
                        overlayRgb
                    )
            }
            else -> {
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
                    var15,
                    var16,
                    var17,
                    var18,
                    overlayTexture,
                    overlayRgb,
                    underlayDefinition
                )
            }
        }
    }

    fun newFloorDecoration(z: Int, x: Int, y: Int, entity: Entity?) {
        for (iz in z downTo 0) {
            if (tiles[iz][x][y] == null) {
                tiles[iz][x][y] = SceneTile(iz, x, y)
            }
        }
        val floorDecoration = FloorDecoration(entity)
        tiles[z][x][y]!!.floorDecoration = floorDecoration
    }

    fun newWallDecoration(
        z: Int,
        x: Int,
        y: Int,
        height: Int,
        entityA: Entity?,
        entityB: Entity?,
        orientationA: Int,
        orientationB: Int,
        xOff: Int,
        yOff: Int,
        tag: Long
    ) {
        for (iz in z downTo 0) {
            if (tiles[iz][x][y] == null) {
                tiles[iz][x][y] = SceneTile(iz, x, y)
            }
        }
        val wallDecoration = WallDecoration(entityA)
        tiles[z][x][y]!!.wallDecoration = wallDecoration
    }
//
//    fun newGameObject(
//        l: Location?,
//        z: Int,
//        x: Int,
//        y: Int,
//        height: Int,
//        width: Int,
//        length: Int,
//        entity: Entity?,
//        orientation: Int,
//        tag: Long
//    ) {
//        for (iz in z downTo 0) {
//            if (tiles[iz][x][y] == null) {
//                tiles[iz][x][y] = SceneTile(iz, x, y)
//            }
//        }
//        val gameObject = GameObject(
//            l,
//            tag,
//            width,
//            length,
//            height,
//            entity,
//            orientation
//        )
//        tiles[z][x][y].getGameObjects().add(gameObject)
//    } //    public void newBoundaryObject(int z, int x, int y, int height, Entity entityA, Entity entityB, int orientationA, int orientationB, long tag) {
//
//    //        for (int iz = z; iz >= 0; --iz) {
//    //            if (this.tiles[iz][x][y] == null) {
//    //                this.tiles[iz][x][y] = new SceneTile(iz, x, y);
//    //            }
//    //        }
//    //
//    //        WallDecoration wallDecoration = new WallDecoration(tag,
//    //                x * Perspective.LOCAL_TILE_SIZE + Constants.REGION_SIZE,
//    //                y * Perspective.LOCAL_TILE_SIZE + Constants.REGION_SIZE,
//    //                height,
//    //                entityA,
//    //                entityB,
//    //                orientationA,
//    //                orientationB,
//    //                0,
//    //                0);
//    //
//    //        this.tiles[z][x][y].getBoundaryObjects().add(wallDecoration);
//    //    }
//    init {
//        regionId = region.getRegionID()
//        baseX = region.getBaseX()
//        baseY = region.getBaseY()
//        tileHeights = region.getTileHeights()
//        tileSettings = region.getTileSettings()
//        overlayIds = region.getOverlayIds()
//        overlayPaths = region.getOverlayPaths()
//        overlayRotations = region.getOverlayRotations()
//        underlayIds = region.getUnderlayIds()
//        locations = region.getLocations()
//    }
}