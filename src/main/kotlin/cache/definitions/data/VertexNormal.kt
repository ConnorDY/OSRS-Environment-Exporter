package cache.definitions.data

class VertexNormal {
    var x = 0
    var y = 0
    var z = 0
    var magnitude = 0
    fun normalize(): Vector3f {
        val v = Vector3f()
        var length = Math.sqrt((x * x + y * y + z * z).toDouble()).toInt()
        if (length == 0) {
            length = 1
        }
        v.x = x.toFloat() / length
        v.y = y.toFloat() / length
        v.z = z.toFloat() / length
        assert(v.x >= -1f && v.x <= 1f)
        assert(v.y >= -1f && v.y <= 1f)
        assert(v.z >= -1f && v.z <= 1f)
        return v
    }
}