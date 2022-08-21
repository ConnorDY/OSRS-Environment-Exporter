package models.glTF

class BufferView(val buffer: Int, val byteOffset: Long, val byteLength: Long) {
    val target = BufferTarget.ARRAY_BUFFER.value
}
