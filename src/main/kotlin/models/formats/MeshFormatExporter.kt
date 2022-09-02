package models.formats

/** A buffered 3D mesh file format writer. */
interface MeshFormatExporter {
    /** (Create and) retrieve the buffers for the given material ID.
     *
     *  @param materialId The RuneScape material ID, or a negative value for flat colours.
     *  @return The buffers for the given material ID.
     */
    fun getOrCreateBuffersForMaterial(materialId: Int): MaterialBuffers

    /** Assigns a texture to the given RuneScape material ID.
     *  Subsequent calls with the same material ID will be ignored.
     *
     *  @param rsIndex The RuneScape material ID.
     *  @param imagePath The path to the texture image file.
     */
    fun addTextureMaterial(rsIndex: Int, imagePath: String)

    /** Flush buffers to file.
     *  May be ignored depending on implementation if partial writes are not supported.
     *  The material buffers in memory may be cleared after calling this method.
     */
    fun flush()

    /** Finish saving buffers to file. This will write any unwritten metadata etc. */
    fun finish()
}
