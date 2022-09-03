package models.formats.glTF

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import models.formats.glTF.extensions.Extensions
import models.formats.glTF.extensions.KHRMaterialsSpecular

class Material(texture: Int?) {
    @JsonInclude(Include.NON_NULL)
    class PbrMetallicRoughness(texture: Int?) {
        class BaseColorTexture(val index: Int)

        val baseColorTexture = if (texture == null) null else BaseColorTexture(texture)

        val metallicFactor = 0f
    }

    val pbrMetallicRoughness = PbrMetallicRoughness(texture)

    val doubleSided = true
    val alphaMode = "MASK"
    val alphaCutoff = 0.2f

    val extensions = Extensions()

    init {
        extensions.khrMaterialsSpecular = KHRMaterialsSpecular(0f, arrayOf(0f, 0f, 0f))
    }
}
