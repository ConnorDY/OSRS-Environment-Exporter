package utils

interface ProgressContainer {
    var progress: Int
    var progressMax: Int
    var status: String
    var isCancelled: Boolean
    fun complete()
}
