package models.locations

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class Location @JsonCreator constructor(
    @JsonProperty("name") val name: String,
    @JsonProperty("coords") val coords: Array<Int>,
)
