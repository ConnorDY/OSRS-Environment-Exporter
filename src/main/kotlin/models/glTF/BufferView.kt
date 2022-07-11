package models.glTF

class BufferView (val buffer: Int, val byteLength: Int) {
    val byteOffset = 0
    val target = BufferTarget.ARRAY_BUFFER.value
}
