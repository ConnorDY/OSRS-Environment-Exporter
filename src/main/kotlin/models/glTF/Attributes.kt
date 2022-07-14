package models.glTF

import com.fasterxml.jackson.annotation.JsonProperty

class Attributes (position: Int) {
    val position = position
    @JsonProperty("POSITION") get
}
