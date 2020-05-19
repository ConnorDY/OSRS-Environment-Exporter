package cache.definitions

class ObjectDefinition {
    var id = 0
    var retextureToFind: ShortArray? = null
    var decorDisplacement = 16
    var isHollow = false
    var name = "null"
    var modelIds: IntArray? = null
    var modelTypes: IntArray? = null
    var recolorToFind: ShortArray? = null
    var mapAreaId = -1
    var textureToReplace: ShortArray? = null
    var sizeX = 1
    var sizeY = 1
    var anInt2083 = 0
    var anIntArray2084: IntArray? = null
    var offsetX = 0
    var mergeNormals = false
    var wallOrDoor = -1
    var animationID = -1
    var transformVarbit = -1
    var ambient = 0
    var contrast = 0
    var actions = arrayOfNulls<String>(5)
    var interactType = 2
    var mapSceneID = -1
    var blockingMask = 0
    lateinit var recolorToReplace: ShortArray
    var shadow = true
    var modelSizeX = 128
    var modelSizeHeight = 128
    var modelSizeY = 128
    var objectID = 0
    var offsetHeight = 0
    var offsetY = 0
    var obstructsGround = false
    var contouredGround = -1
    var supportsItems = -1
    lateinit var transforms: IntArray
    var isRotated = false
    var transformVarp = -1
    var ambientSoundId = -1
    var aBool2111 = false
    var anInt2112 = 0
    var anInt2113 = 0
    var blocksProjectile = true
    var params: Map<Int, Any>? = null
}