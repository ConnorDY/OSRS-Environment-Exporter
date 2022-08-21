package models.glTF

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import utils.ByteChunkBuffer
import utils.ChunkWriteListener
import java.io.File

@JsonInclude(NON_EMPTY)
class glTF {
    val asset = Asset(
        "2.0",
        "OSRS-Environment-Exporter"
    )

    val scene = 0

    val scenes = ArrayList<Scene>()
    val nodes = ArrayList<Node>()
    val meshes = ArrayList<Mesh>()
    val materials = ArrayList<Material>()
    val textures = ArrayList<Texture>()
    val images = ArrayList<Image>()
    val accessors = ArrayList<Accessor>()
    val bufferViews = ArrayList<BufferView>()
    val buffers = ArrayList<Buffer>()

    val extensionsUsed = arrayOf("KHR_materials_specular")

    private val materialMap = HashMap<Int, MaterialBuffers>()
    private val rsIndexToMaterialIndex = HashMap<Int, Int>()

    fun addMesh(material: Int, buffer: ByteChunkBuffer) {
        val materialBuffer = materialMap.get(material)!!

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
        primitives.add(Primitive(attributes, rsIndexToMaterialIndex[material]))

        // mesh
        val mesh = Mesh(primitives)
        meshes.add(mesh)

        // node
        val node = Node(meshes.size - 1)
        nodes.add(node)
    }

    private fun addAccessorForFloats(
        floatBuffer: FloatVectorBuffer,
        buffer: ByteChunkBuffer
    ): Int {
        val floatsByteChunkBuffer = floatBuffer.getByteChunks()

        // buffer view
        val bufferView = BufferView(0, buffer.byteLength, floatsByteChunkBuffer.byteLength)
        bufferViews.add(bufferView)

        buffer.addBytes(floatsByteChunkBuffer)

        // accessor
        val accessorType = when (floatBuffer.dims) {
            4 -> AccessorType.VEC4
            3 -> AccessorType.VEC3
            2 -> AccessorType.VEC2
            else -> throw UnsupportedOperationException()
        }

        val accessor = Accessor(
            bufferViews.size - 1,
            accessorType,
            floatBuffer.size,
            floatBuffer.min,
            floatBuffer.max
        )
        accessors.add(accessor)

        return accessors.size - 1
    }

    fun getOrCreateBuffersForMaterial(materialId: Int) = materialMap.getOrPut(materialId) {
        MaterialBuffers(materialId >= 0)
    }

    fun addTextureMaterial(rsIndex: Int, imagePath: String) {
        if (rsIndexToMaterialIndex.containsKey(rsIndex)) return
        rsIndexToMaterialIndex[rsIndex] = materials.size

        val material = Material(materials.size)
        materials.add(material)

        val texture = Texture(materials.size - 1)
        textures.add(texture)

        val image = Image(imagePath)
        images.add(image)
    }

    fun save(directory: String, chunkWriteListeners: List<ChunkWriteListener>) {
        val chunkBuffer = ByteChunkBuffer()

        for (materialId in materialMap.keys) {
            addMesh(materialId, chunkBuffer)
        }

        // create buffer
        val buffer = Buffer("data", chunkBuffer.byteLength)
        buffers.add(buffer)

        // write buffer to files
        chunkWriteListeners.forEach { it.onStartWriting(buffer.byteLength) }
        File("$directory/${buffer.uri}").outputStream().channel.use { file ->
            chunkBuffer.getBuffers().forEach { buf ->
                val dataLength = buf.remaining().toLong()
                file.write(buf)
                chunkWriteListeners.forEach { it.onChunkWritten(dataLength) }
            }
        }

        // convert to JSON
        val mapper = ObjectMapper()
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
        val json = mapper.writeValueAsString(this)

        // write glTf file
        File("$directory/scene.gltf").printWriter().use {
            it.write(json)
        }
        chunkWriteListeners.forEach { it.onFinishWriting() }
    }

    init {
        // setup single scene
        val _sceenNodes = ArrayList<Int>()
        _sceenNodes.add(0)

        val _scene = Scene(_sceenNodes)
        scenes.add(_scene)
    }
}
