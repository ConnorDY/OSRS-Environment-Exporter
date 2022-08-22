package utils

class NullProgressContainer : ProgressContainer {
    override var progress: Int
        get() = 0
        set(value) {}
    override var progressMax: Int
        get() = 0
        set(value) {}
    override var status: String
        get() = "No information available"
        set(value) {}
    override var isCancelled: Boolean
        get() = false
        set(value) {
            throw UnsupportedOperationException("Cancellation is not supported")
        }

    override fun complete() {}
}
