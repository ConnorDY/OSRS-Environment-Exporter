package models.glTF

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include

class Material {
    @JsonInclude(Include.NON_NULL)
    class PbrMetallicRoughness() {
        class BaseColorTexture (val index: Int)

        var metallicFactor: Float? = null
        var roughnessFactor: Float? = null

        var baseColorFactor: Array<Float>? = null
        var baseColorTexture: BaseColorTexture? = null

        constructor(color: Array<Float>): this() {
            this.baseColorFactor = color
            this.metallicFactor = 0.0f
            this.roughnessFactor = 1.0f
        }

        constructor(texture: Int): this() {
            this.baseColorTexture = BaseColorTexture(texture)
        }
    }

    var pbrMetallicRoughness: PbrMetallicRoughness

    constructor(color: Array<Float>) {
       this.pbrMetallicRoughness = PbrMetallicRoughness(color)
    }

    constructor(texture: Int) {
        this.pbrMetallicRoughness = PbrMetallicRoughness(texture)
    }
}
