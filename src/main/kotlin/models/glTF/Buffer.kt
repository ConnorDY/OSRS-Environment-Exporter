package models.glTF

import java.io.File

class Buffer (bytes: ByteArray, filename: String) {
    val byteLength = bytes.size
    val uri = "${filename}.bin"

    init {
        File("./output/${uri}").writeBytes(bytes)
    }
}
