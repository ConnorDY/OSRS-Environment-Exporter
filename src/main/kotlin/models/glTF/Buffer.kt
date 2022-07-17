package models.glTF

import com.fasterxml.jackson.annotation.JsonIgnore

class Buffer(filename: String) {
    val uri = "$filename.bin"

    fun getByteLength() = bytes.size

    // TODO: replace this with something more efficient
    var bytes = ByteArray(0)
        @JsonIgnore get

    fun addBytes(bytesToAdd: ByteArray) {
        bytes += bytesToAdd
    }
}
