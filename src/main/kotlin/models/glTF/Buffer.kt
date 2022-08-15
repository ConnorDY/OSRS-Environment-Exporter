package models.glTF

class Buffer(filename: String, val byteLength: Int) {
    val uri = "$filename.bin"
}
