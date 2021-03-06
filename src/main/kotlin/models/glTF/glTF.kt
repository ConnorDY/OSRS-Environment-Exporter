package models.glTF

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import java.io.File
import kotlin.math.max
import kotlin.math.min

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

    fun addMesh(material: Int, buffer: Buffer) {
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
        buffer: Buffer
    ): Int {
        val floatsByteArray = floatBuffer.getBytes()

        // buffer view
        val bufferView = BufferView(0, buffer.getByteLength(), floatsByteArray.size)
        bufferViews.add(bufferView)

        buffer.addBytes(floatsByteArray)

        // accessor
        val accessorType = when (floatBuffer.dims) {
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

    fun save(directory: String) {
        // create buffer
        val buffer = Buffer("data")
        buffers.add(buffer)

        for (materialId in materialMap.keys) {
            addMesh(materialId, buffer)
        }

        // write buffer to files
        File("$directory/${buffer.uri}").writeBytes(buffer.getBytes())

        // convert to JSON
        val mapper = ObjectMapper()
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
        val json = mapper.writeValueAsString(this)

        // write glTf file
        File("$directory/scene.gltf").printWriter().use {
            it.write(json)
        }
    }

    init {
        // setup single scene
        val _sceenNodes = ArrayList<Int>()
        _sceenNodes.add(0)

        val _scene = Scene(_sceenNodes)
        scenes.add(_scene)
    }
}
