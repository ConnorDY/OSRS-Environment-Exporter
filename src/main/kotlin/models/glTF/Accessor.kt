package models.glTF

class Accessor (val bufferView: Int, val count: Int, val min: FloatArray, val max: FloatArray) {
    val byteOffset = 0
    val componentType = ComponentType.FLOAT.value
    val type = AccessorType.VEC3.value
}
