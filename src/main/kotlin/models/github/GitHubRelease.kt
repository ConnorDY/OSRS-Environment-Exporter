package models.github

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubRelease @JsonCreator constructor(
    @JsonProperty("html_url") val htmlURL: String,
    @JsonProperty("tag_name") val tagName: String
)
