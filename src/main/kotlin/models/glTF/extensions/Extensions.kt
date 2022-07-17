package models.glTF.extensions

import com.fasterxml.jackson.annotation.JsonProperty

class Extensions {
    var khrMaterialsSpecular: KHRMaterialsSpecular? = null
        @JsonProperty("KHR_materials_specular") get
}
