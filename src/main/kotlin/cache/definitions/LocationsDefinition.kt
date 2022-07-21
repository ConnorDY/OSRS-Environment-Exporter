package cache.definitions

data class LocationsDefinition(
    val regionId: Int = 0,
    val locations: ArrayList<Location> = ArrayList()
)

data class Location(
    var objId: Int = -1,
    var type: Int = 0,
    var orientation: Int = 0,
    var x: Int = 0,
    var y: Int = 0,
    var z: Int = 0
)
