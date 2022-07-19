package cache

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

class XteaManager(path: String) {
    private val xteaKeys: Map<Int, IntArray>

    init {
        val xteaFile = File(path, "xteas.json")
        val contents = xteaFile.bufferedReader().use { it.readText() }
        xteaKeys =
            ObjectMapper().readValue(contents, Array<XteaKey>::class.java)
                .associate {
                    it.mapsquare to it.key
                }
    }

    fun getKeys(region: Int): IntArray? {
        return xteaKeys[region]
    }

    data class XteaKey @JsonCreator constructor(
        @JsonProperty("mapsquare") @JsonAlias("region") val mapsquare: Int,
        @JsonProperty("key") @JsonAlias("keys") val key: IntArray
    )
}
