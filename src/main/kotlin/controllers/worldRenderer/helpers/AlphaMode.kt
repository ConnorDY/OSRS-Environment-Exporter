package controllers.worldRenderer.helpers

enum class AlphaMode(val internalId: Int, val humanReadableName: String) {
    BLEND(0, "Blend"),
    CLIP(1, "Clip at 0.1"),
    HASH(2, "Hash"),
    ORDERED_DITHER(4, "Ordered Dither"),
    IGNORE(3, "Opaque")
}
