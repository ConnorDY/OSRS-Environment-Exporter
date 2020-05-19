package cache.definitions.data

object CircularAngle {
    private const val UNIT = Math.PI / 1024.0 // How much of the circle each unit of SINE/COSINE is
    val SINE = IntArray(2048) // sine angles for each of the 2048 units, * 65536 and stored as an int
    val COSINE = IntArray(2048) // cosine

    init {
        for (i in 0..2047) {
            SINE[i] = (65536.0 * Math.sin(i.toDouble() * UNIT)).toInt()
            COSINE[i] = (65536.0 * Math.cos(i.toDouble() * UNIT)).toInt()
        }
    }
}