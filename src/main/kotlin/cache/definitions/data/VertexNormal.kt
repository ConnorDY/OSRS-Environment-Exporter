package cache.definitions.data

class VertexNormal(
    var x: Int = 0,
    var y: Int = 0,
    var z: Int = 0,
    var magnitude: Int = 0,
) {
    constructor(other: VertexNormal) : this(other.x, other.y, other.z, other.magnitude)

    operator fun plusAssign(other: VertexNormal) {
        x += other.x
        y += other.y
        z += other.z
        magnitude += other.magnitude
    }
}
