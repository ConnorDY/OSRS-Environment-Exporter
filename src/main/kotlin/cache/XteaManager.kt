package cache

import com.google.gson.Gson
import java.io.File

class XteaManager(path: String) {
    private val xteaKeys: MutableMap<Int, IntArray> = HashMap()

    init {
        val xteaFile = File(path, "xteas.json")
        val contents = xteaFile.bufferedReader().use { it.readText() }
        val keys = Gson().fromJson(contents, Array<XteaKey>::class.java)
        keys.forEach {
            if (it.mapsquare > 0) {
                xteaKeys[it.mapsquare] = it.key
            } else {
                xteaKeys[it.region] = it.keys
            }
        }
    }

    fun getKeys(region: Int): IntArray? {
        return xteaKeys[region]
    }

    data class XteaKey(
        val region: Int,
        val keys: IntArray,
        val mapsquare: Int,
        val key: IntArray
    )
}
