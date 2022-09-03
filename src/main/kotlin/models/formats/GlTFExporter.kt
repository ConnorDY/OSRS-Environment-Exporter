package models.formats

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import models.formats.glTF.Accessor
import models.formats.glTF.AccessorType
import models.formats.glTF.Attributes
import models.formats.glTF.Buffer
import models.formats.glTF.BufferView
import models.formats.glTF.Image
import models.formats.glTF.Material
import models.formats.glTF.Mesh
import models.formats.glTF.Node
import models.formats.glTF.Primitive
import models.formats.glTF.Scene
import models.formats.glTF.Texture
import models.formats.glTF.glTF
import utils.ByteChunkBuffer
import utils.ChunkWriteListener
import java.io.File

class GlTFExporter(private val directory: String, private val chunkWriteListeners: List<ChunkWriteListener>) : MeshFormatExporter {
    private val materialMap = HashMap<Int, MaterialBuffers>()
    private val rsIndexToMaterialIndex = HashMap<Int, Int>()
    private val sceneNodes = ArrayList<Int>()
    private val chunkBuffer = ByteChunkBuffer()
    /** How many bytes the data file contains already, offsetting memory buffers by that many bytes. */
    private var bufferFileOffset = 0L

    private val dataFile = File("$directory/$dataFilename").outputStream().channel
    private var flushingRegionNum = 0
    private val gltfModel = glTF()

    private val nullMaterial = createNullTextureMaterial(gltfModel)

    private fun addMesh(material: Int, buffer: ByteChunkBuffer): Node {
        val materialBuffer = materialMap[material]!!

        val positionsAccessor = addAccessorForFloats(materialBuffer.positions, buffer)
        var texCoordsAccessor: Int? = null
        var colorsAccessor: Int? = null

        if (material != -1) {
            texCoordsAccessor = addAccessorForFloats(materialBuffer.texcoords!!, buffer)
        } else {
            colorsAccessor = addAccessorForFloats(materialBuffer.colors!!, buffer)
        }

        // primitive attributes
        val attributes = Attributes(positionsAccessor, texCoordsAccessor, colorsAccessor)

        // primitive
        val primitives = ArrayList<Primitive>()
        primitives.add(Primitive(attributes, rsIndexToMaterialIndex[material] ?: nullMaterial))

        // mesh
        val mesh = Mesh(primitives)
        gltfModel.meshes.add(mesh)

        // node
        return Node(mesh = gltfModel.meshes.size - 1)
    }

    private fun addAccessorForFloats(
        floatBuffer: FloatVectorBuffer,
        buffer: ByteChunkBuffer
    ): Int {
        val floatsByteChunkBuffer = floatBuffer.getByteChunks()

        // buffer view
        val bufferView = BufferView(0, bufferFileOffset + buffer.byteLength, floatsByteChunkBuffer.byteLength)
        gltfModel.bufferViews.add(bufferView)

        buffer.addBytes(floatsByteChunkBuffer)

        // accessor
        val accessorType = when (floatBuffer.dims) {
            4 -> AccessorType.VEC4
            3 -> AccessorType.VEC3
            2 -> AccessorType.VEC2
            else -> throw UnsupportedOperationException()
        }

        val accessor = Accessor(
            gltfModel.bufferViews.size - 1,
            accessorType,
            floatBuffer.size,
            floatBuffer.min,
            floatBuffer.max
        )
        gltfModel.accessors.add(accessor)

        return gltfModel.accessors.size - 1
    }

    override fun getOrCreateBuffersForMaterial(materialId: Int) = materialMap.getOrPut(materialId) {
        MaterialBuffers(materialId >= 0)
    }

    override fun addTextureMaterial(rsIndex: Int, imagePath: String) {
        if (rsIndexToMaterialIndex.containsKey(rsIndex)) return
        rsIndexToMaterialIndex[rsIndex] = gltfModel.materials.size

        val material = Material(gltfModel.textures.size, name = "rs_texture_$rsIndex")
        gltfModel.materials.add(material)

        val texture = Texture(gltfModel.images.size)
        gltfModel.textures.add(texture)

        val image = Image(imagePath)
        gltfModel.images.add(image)
    }

    private fun createNullTextureMaterial(gltfModel: glTF): Int {
        val material = Material(null, name = "rs_untextured")
        gltfModel.materials.add(material)
        return gltfModel.materials.size - 1
    }

    override fun flush(name: String) {
        if (materialMap.isNotEmpty()) {
            val unflushedNodes = ArrayList<Node>()
            for (materialId in materialMap.keys) {
                unflushedNodes.add(addMesh(materialId, chunkBuffer))
            }

            // Now that the meshes have been added, clear old buffers out
            materialMap.clear()

            // Flush unflushed nodes; bundle into one parent node
            val sceneNode = Node(
                name = name,
                children = unflushedNodes.indices.map { it + gltfModel.nodes.size }
            )
            gltfModel.nodes.addAll(unflushedNodes)
            unflushedNodes.clear()

            // Add parent node into scene
            sceneNodes.add(gltfModel.nodes.size)
            gltfModel.nodes.add(sceneNode)

            // Flush buffers to data file
            chunkWriteListeners.forEach { it.onStartRegion(flushingRegionNum) }
            chunkBuffer.getBuffers().forEach { buf ->
                val dataLength = buf.remaining().toLong()
                dataFile.write(buf)
                bufferFileOffset += dataLength
            }
            chunkBuffer.clear()
            chunkWriteListeners.forEach { it.onEndRegion() }
        }

        flushingRegionNum++
    }

    override fun finish() {
        flush("")
        dataFile.close()

        // setup single scene
        gltfModel.scenes.add(Scene(sceneNodes))

        // create buffer
        val buffer = Buffer(dataFilename, byteLength = bufferFileOffset)
        gltfModel.buffers.add(buffer)

        // indicate start of gltf file
        chunkWriteListeners.forEach { it.onStartRegion(-1) }

        // convert to JSON
        val mapper = ObjectMapper()
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
        val json = mapper.writeValueAsString(gltfModel)

        // write glTf file
        File("$directory/scene.gltf").printWriter().use {
            it.write(json)
        }
        chunkWriteListeners.forEach { it.onFinishWriting() }
    }

    companion object {
        private const val dataFilename = "data.bin"
    }
}
