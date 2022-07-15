package models.glTF

import java.nio.ByteBuffer
import java.nio.ByteOrder

class glTF(private val directory: String) {
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

    private val bufferNames = ArrayList<String>()

    fun addMesh(vertices: ArrayList<FloatArray>, bufferName: String, material: Int?) {
        // convert to byte array
        val byteBuffer = ByteBuffer.allocate(
            vertices.size * 3 * 4 // 3 coordinates per vertex * 4 bytes (in a float)
        ).order(ByteOrder.LITTLE_ENDIAN)
        val floatBuffer = byteBuffer.asFloatBuffer()

        for (vertex in vertices) {
            for (value in vertex) {
                floatBuffer.put(value)
            }
        }

        val byteArray = byteBuffer.array()

        // buffer && buffer view
        val existingBufferIndex = bufferNames.indexOf(bufferName)
        val usingExistingBuffer = existingBufferIndex >= 0
        val bufferIndex = if (usingExistingBuffer) existingBufferIndex else buffers.size

        val buffer =
            if (usingExistingBuffer) buffers[existingBufferIndex] else Buffer(directory, bufferName)

        val byteOffset = buffer.byteLength
        buffer.addBytes(byteArray)

        if (!usingExistingBuffer) {
            bufferNames.add(bufferName)
            buffers.add(buffer)
        }

        val bufferView = BufferView(bufferIndex, byteOffset, byteArray.size)
        bufferViews.add(bufferView)

        // accessor
        val dims = vertices[0].size
        val min = (0 until dims).map { n -> vertices.map { it[n] }.min()!! }.toFloatArray()
        val max = (0 until dims).map { n -> vertices.map { it[n] }.max()!! }.toFloatArray()

        val accessor = Accessor(bufferViews.size - 1, vertices.size, min, max)
        accessors.add(accessor)

        // primitive
        val primitives = ArrayList<Primitive>()
        primitives.add(Primitive(bufferViews.size - 1, material))

        // mesh
        val mesh = Mesh(primitives)
        meshes.add(mesh)

        // node
        val node = Node(meshes.size - 1)
        nodes.add(node)
    }

    fun addTextureMaterial(imagePath: String) {
        val material = Material(materials.size)
        materials.add(material)

        val texture = Texture(materials.size - 1)
        textures.add(texture)

        val image = Image(imagePath)
        images.add(image)
    }

    fun writeBuffersToFiles() {
        for (buffer in buffers) {
            buffer.writeToFile()
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
