package models.formats.glTF

class Accessor(val bufferView: Int, _type: AccessorType, val count: Int, val min: FloatArray, val max: FloatArray) {
    val byteOffset = 0
    val type = _type.value
    val componentType = ComponentType.FLOAT.value
}
