package models.openrs2

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenRs2CacheKeys @JsonCreator constructor(
    @JsonProperty("archive")
    val archive: Int,
    @JsonProperty("group")
    val group: Int,
    @JsonProperty("name_hash")
    val nameHash: Int,
    @JsonProperty("name")
    val name: String,
    @JsonProperty("mapsquare") @JsonAlias("mapSquare")
    val mapSquare: Int,
    @JsonProperty("key")
    val key: Array<Int>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OpenRs2CacheKeys

        if (archive != other.archive) return false
        if (group != other.group) return false
        if (nameHash != other.nameHash) return false
        if (mapSquare != other.mapSquare) return false
        if (name != other.name) return false
        if (!key.contentEquals(other.key)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = archive
        result = 31 * result + group
        result = 31 * result + nameHash
        result = 31 * result + mapSquare
        result = 31 * result + name.hashCode()
        result = 31 * result + key.contentHashCode()
        return result
    }
}
