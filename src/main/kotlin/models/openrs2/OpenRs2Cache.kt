package models.openrs2;

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenRs2Cache @JsonCreator constructor(
    @JsonProperty("id")
    val id: Int,
    @JsonProperty("scope")
    val scope: String,
    @JsonProperty("game")
    val game: String,
    @JsonProperty("environment")
    val environment: String,
    @JsonProperty("language")
    val language: String,
    @JsonProperty("builds")
    val builds: List<Build>,
    @JsonProperty("timestamp")
    val timestamp: String? = null,
    @JsonProperty("sources")
    val sources: List<String>,
    @JsonProperty("valid_indexes")
    val validIndexes: Int? = null,
    @JsonProperty("indexes") val indexes: Int? = null,
    @JsonProperty("valid_groups")
    val validGroups: Int? = null,
    @JsonProperty("groups")
    val groups: Int? = null,
    @JsonProperty("valid_keys")
    val validKeys: Int? = null,
    @JsonProperty("keys")
    val keys: Int? = null,
    @JsonProperty("size")
    val size: Long? = null,
    @JsonProperty("blocks")
    val blocks: Int? = null,
    @JsonProperty("disk_store_valid")
    val diskStoreValid: Boolean? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Build @JsonCreator constructor(
    @JsonProperty("major")
    val major: Int,
    @JsonProperty("minor")
    val minor: Int? = null
)
