package models.formats.glTF

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY

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
}
