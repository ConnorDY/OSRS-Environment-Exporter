package controllers.worldRenderer.helpers

class MeshBuffers {
    val vertexBuffer = GpuIntBuffer()
    val uvBuffer = GpuFloatBuffer()

    fun clear() {
        vertexBuffer.clear()
        uvBuffer.clear()
    }

    fun flip() {
        vertexBuffer.flip()
        uvBuffer.flip()
    }
}
