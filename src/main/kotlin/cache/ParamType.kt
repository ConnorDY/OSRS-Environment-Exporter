package cache

enum class ParamType(val id: Int) {
    /**
     * Contains the Runescape url
     */
    WEB_URL(2),
    /**
     * The game revision number of the cache starting in rev209
     */
    REVISION(25)
}