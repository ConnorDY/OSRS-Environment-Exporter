package controllers.worldRenderer.entities

data class ComputeObj(
    // offset into buffer
    var offset: Int = -1,

    // offset into uv buffer
    var uvOffset: Int = -1,

    // length in faces
    var size: Int = -1,
    // write idx in target buffer
    var idx: Int = -1,
    // radius, orientation
    var flags: Int = -1,

    // scene position x
    var x: Int = 0,
    // scene position y
    var y: Int = 0,
    // scene position z
    var z: Int = 0,

    // unique id for object picking
    var pickerId: Int = -1,

    // anim vars
    var frame: Int = -1,
    var frameDuration: Int = -1,
    var frameOffset: Int = -1,
    var totalFrames: Int = -1
) {
    fun toArray(): IntArray {
        return intArrayOf(offset, uvOffset, size, idx, flags, x, y, z, pickerId, frame, frameDuration, frameOffset, totalFrames)
    }
}
