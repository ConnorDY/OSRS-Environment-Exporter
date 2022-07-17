package models.glTF

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
class Primitive(val attributes: Attributes, val material: Int?) {
    val mode = 4
}
