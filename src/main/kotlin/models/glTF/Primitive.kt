package models.glTF

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include

@JsonInclude(JsonInclude.Include.NON_NULL)
class Primitive(position: Int, val material: Int?) {
    val mode = 4
    val attributes = Attributes(position)
}
