package models.config.types

enum class ScaleMode(val humanReadableName: String, val scaleFactor: Float) {
    SCALE_1TO1("1:1 (Internal cache size, giant!)", 1f),
    SCALE_1TO100("1:100 (Previous default)", 1f / 100f),
    SCALE_1TO128("1:128 (1 tile per unit)", 1f / 128f),
}
