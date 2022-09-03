package models.formats.glTF

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
class Attributes(position: Int, texcoord: Int?, color: Int?) {
    val position = position
        @JsonProperty("POSITION") get

    val texcoord_0 = texcoord
        @JsonProperty("TEXCOORD_0") get

    val color_0 = color
        @JsonProperty("COLOR_0") get
}
