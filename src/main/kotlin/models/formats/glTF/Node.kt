package models.formats.glTF

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_EMPTY)
class Node(
    val mesh: Int? = null,
    val children: List<Int> = emptyList(),
)
