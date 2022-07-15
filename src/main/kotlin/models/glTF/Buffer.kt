package models.glTF

import java.io.File

class Buffer (private val directory: String, filename: String) {
    var byteLength = 0
    val uri = "${filename}.bin"

    private var bytes = ByteArray(0)

    fun addBytes(bytesToAdd: ByteArray) {
        bytes += bytesToAdd
        byteLength += bytesToAdd.size
    }

    fun writeToFile() {
        File("${directory}/${uri}").writeBytes(bytes)
    }
}
