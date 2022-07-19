package models.locations

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class Locations @JsonCreator constructor(
    @JsonProperty("locations") val locations: Array<Location>
)
