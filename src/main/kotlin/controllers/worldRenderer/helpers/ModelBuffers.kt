package controllers.worldRenderer.helpers

class ModelBuffers {
    fun clear() {
        modelBuffer.clear()
        modelBufferSmall.clear()
        modelBufferUnordered.clear()
        unorderedModelsCount = 0
        largeModelsCount = 0
        smallModelsCount = 0
    }

    fun clearBufferOffset() {
        // TODO: understand why this isn't always done(?)
        targetBufferOffset = 0
    }

    fun flip() {
        modelBuffer.flip()
        modelBufferSmall.flip()
        modelBufferUnordered.flip()
    }

    fun bufferForTriangles(triangles: Int): GpuIntBuffer {
        return if (triangles <= SMALL_TRIANGLE_COUNT) {
            ++smallModelsCount
            modelBufferSmall
        } else {
            ++largeModelsCount
            modelBuffer
        }
    }

    fun bufferUnordered(): GpuIntBuffer {
        unorderedModelsCount++
        return modelBufferUnordered
    }

    val modelBufferUnordered: GpuIntBuffer = GpuIntBuffer()
    val modelBufferSmall: GpuIntBuffer = GpuIntBuffer()
    val modelBuffer: GpuIntBuffer = GpuIntBuffer()

    var unorderedModelsCount = 0
        private set

    /**
     * number of models in small buffer
     */
    var smallModelsCount = 0
        private set

    /**
     * number of models in large buffer
     */
    var largeModelsCount = 0
        private set

    /**
     * offset in the target buffer for model
     */
    var targetBufferOffset = 0
        private set
    fun addTargetBufferOffset(n: Int) {
        targetBufferOffset += n
    }

    companion object {
        const val FLAG_SCENE_BUFFER = Int.MIN_VALUE
        const val MAX_TRIANGLE = 4096
        const val SMALL_TRIANGLE_COUNT = 512
    }
}
