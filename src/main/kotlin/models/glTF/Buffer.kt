package models.glTF

class Buffer(filename: String, val byteLength: Long) {
    val uri = "$filename.bin"
}
