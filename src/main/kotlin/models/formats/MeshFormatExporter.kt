package models.formats

import models.glTF.MaterialBuffers
import utils.ChunkWriteListener

interface MeshFormatExporter {
    fun getOrCreateBuffersForMaterial(materialId: Int): MaterialBuffers
    fun addTextureMaterial(rsIndex: Int, imagePath: String)
    fun save(directory: String, chunkWriteListeners: List<ChunkWriteListener>)
}
