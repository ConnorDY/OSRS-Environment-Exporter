package cache.loaders

import cache.definitions.RegionDefinition

fun RegionLoader.getTileHeight(z: Int, x: Int, y: Int): Int {
    val r: RegionDefinition = findRegionForWorldCoordinates(x, y) ?: return 0
    return r.tiles[z][x % 64][y % 64]?.height ?: return 0
}

fun RegionLoader.getTileSettings(z: Int, x: Int, y: Int): Int {
    val r: RegionDefinition = findRegionForWorldCoordinates(x, y) ?: return 0
    return r.tileSettings[z][x % 64][y % 64]
}
