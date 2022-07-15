package models.glTF

class BufferView (val buffer: Int, val byteOffset: Int, val byteLength: Int) {
    val target = BufferTarget.ARRAY_BUFFER.value
}
