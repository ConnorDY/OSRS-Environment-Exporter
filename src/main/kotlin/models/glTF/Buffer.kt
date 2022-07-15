package models.glTF

import java.io.File

class Buffer (bytes: ByteArray, directory: String, filename: String) {
    val byteLength = bytes.size
    val uri = "${filename}.bin"

    init {
        File("${directory}/${uri}").writeBytes(bytes)
    }
}
