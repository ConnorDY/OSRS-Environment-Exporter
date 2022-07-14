package models.glTF

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class glTF {
  val asset = Asset(
    "2.0",
    "OSRS-Environment-Exporter"
  )

  val scene = 0

  val scenes = ArrayList<Scene>()
  val nodes = ArrayList<Node>()
  val meshes = ArrayList<Mesh>()
  val accessors = ArrayList<Accessor>()
  val bufferViews = ArrayList<BufferView>()
  val buffers = ArrayList<Buffer>()

  fun addMesh(vertices: ArrayList<FloatArray>, filename: String) {
    val bytes = ByteBuffer.allocate(vertices.size * 3 * 4).order(ByteOrder.LITTLE_ENDIAN)
    val floatBuffer = bytes.asFloatBuffer()

    for (vertex in vertices) {
      for (value in vertex) {
        floatBuffer.put(value)
      }
    }

    val buffer = Buffer(bytes.array(), filename)
    buffers.add(buffer)

    val bufferView = BufferView(buffers.size - 1, buffer.byteLength)
    bufferViews.add(bufferView)

    val dims = vertices[0].size
    val min = (0 until dims).map { n -> vertices.map { it[n] }.min()!! }.toFloatArray()
    val max = (0 until dims).map { n -> vertices.map { it[n] }.max()!! }.toFloatArray()

    val accessor = Accessor(bufferViews.size - 1, vertices.size, min, max)
    accessors.add(accessor)

    val primitives = ArrayList<Primitive>()
    primitives.add(Primitive(buffers.size - 1))

    val mesh = Mesh(primitives)
    meshes.add(mesh)

    val node = Node(meshes.size - 1)
    nodes.add(node)
  }

  init {
    // setup single scene
    val _sceenNodes = ArrayList<Int>()
    _sceenNodes.add(0)

    val _scene = Scene(_sceenNodes)
    scenes.add(_scene)
  }
}
