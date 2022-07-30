package controllers.worldRenderer.entities

import cache.definitions.ModelDefinition
import cache.definitions.data.FaceNormal
import cache.definitions.data.VertexNormal
import cache.loaders.RegionLoader
import cache.loaders.getTileHeight
import controllers.worldRenderer.Constants
import controllers.worldRenderer.helpers.GpuIntBuffer
import controllers.worldRenderer.helpers.ModelBuffers
import controllers.worldRenderer.helpers.ModelBuffers.Companion.MAX_TRIANGLE
import kotlin.math.min
import kotlin.math.sqrt

class Model(
    val modelDefinition: ModelDefinition,

    var orientation: Int = 0,
    var orientationType: OrientationType = OrientationType.STRAIGHT,
    var x: Int = 0, // 3d world space position
    var y: Int = 0,
    var yOff: Int = 0,
    var xOff: Int = 0,
    var height: Int = 0,
    val computeObj: ComputeObj = ComputeObj(),
    val faceColors1: IntArray = IntArray(modelDefinition.faceCount),
    val faceColors2: IntArray = IntArray(modelDefinition.faceCount),
    val faceColors3: IntArray = IntArray(modelDefinition.faceCount)
) : Renderable {
    val vertexPositionsX: IntArray = modelDefinition.vertexPositionsX.clone()
    val vertexPositionsY: IntArray = modelDefinition.vertexPositionsY.clone()
    val vertexPositionsZ: IntArray = modelDefinition.vertexPositionsZ.clone()

    var field1840: ByteArray? = null
    var field1852 = 0
    var field1844: IntArray? = null
    var field1865: IntArray? = null
    var field1846: IntArray? = null

    private var boundsType = 0
    private var bottomY = 0
    var xYZMag = 0
    private var radius = 0
    private var diameter = 0

    override fun draw(modelBuffers: ModelBuffers, sceneX: Int, sceneY: Int, height: Int, objType: Int) {
        val x: Int = sceneX * Constants.LOCAL_TILE_SIZE + xOff
        val z: Int = sceneY * Constants.LOCAL_TILE_SIZE + yOff

        val b: GpuIntBuffer = modelBuffers.bufferForTriangles(min(MAX_TRIANGLE, modelDefinition.faceCount))
        b.ensureCapacity(13)

        computeObj.idx = modelBuffers.targetBufferOffset
        computeObj.flags = ModelBuffers.FLAG_SCENE_BUFFER or (radius shl 12) or orientationType.id
        computeObj.x = x
        computeObj.y = height
        computeObj.z = z
        computeObj.pickerId = modelBuffers.calcPickerId(sceneX, sceneY, objType)
        b.buffer.put(computeObj.toArray())

        modelBuffers.addTargetBufferOffset(computeObj.size * 3)
    }

    private fun calculateBoundsCylinder() {
        if (boundsType != 1) {
            boundsType = 1
            bottomY = 0
            xYZMag = 0
            height = 0
            for (var1 in 0 until modelDefinition.vertexCount) {
                val var2: Int = modelDefinition.vertexPositionsX[var1]
                val var3: Int = modelDefinition.vertexPositionsY[var1]
                val var4: Int = modelDefinition.vertexPositionsZ[var1]
                if (-var3 > height) {
                    height = -var3
                }
                if (var3 > bottomY) {
                    bottomY = var3
                }
                val var5 = var2 * var2 + var4 * var4
                if (var5 > xYZMag) {
                    xYZMag = var5
                }
            }
            xYZMag = (sqrt(xYZMag.toDouble()) + 0.99).toInt()
            radius =
                (sqrt((xYZMag * xYZMag + height * height).toDouble()) + 0.99).toInt()
            diameter =
                radius + (sqrt((xYZMag * xYZMag + bottomY * bottomY).toDouble()) + 0.99).toInt()
        }
    }

    fun contourGround(
        regionLoader: RegionLoader,
        xOff: Int,
        height: Int,
        yOff: Int,
        z: Int,
        baseX: Int,
        baseY: Int,
        deepCopy: Boolean,
        clipType: Int
    ): Model {
        calculateBoundsCylinder()
        var left = xOff - xYZMag
        var right = xOff + xYZMag
        var top = yOff - xYZMag
        var bottom = yOff + xYZMag
        left = left shr 7
        right = right + 127 shr 7
        top = top shr 7
        bottom = bottom + 127 shr 7

        // refactored to find heights from cache
        // it would not contour tiles near edge of regions
        val topLeft = regionLoader.getTileHeight(z, baseX + left, baseY + top)
        val topRight = regionLoader.getTileHeight(z, baseX + right, baseY + top)
        val bottomLeft = regionLoader.getTileHeight(z, baseX + left, baseY + bottom)
        val bottomRight = regionLoader.getTileHeight(z, baseX + right, baseY + bottom)
        return if (height == topLeft && height == topRight && height == bottomLeft && height == bottomRight) {
            this
        } else {
            val model: Model = if (deepCopy) Model(
                ModelDefinition(modelDefinition),
                faceColors1 = faceColors1,
                faceColors2 = faceColors2,
                faceColors3 = faceColors3
            ) else this
            var var12: Int
            var var13: Int
            var var14: Int
            var var15: Int
            var var16: Int
            var var17: Int
            var var18: Int
            var var19: Int
            var var20: Int
            var var21: Int
            if (clipType == 0) {
                var12 = 0
                while (var12 < modelDefinition.vertexCount) {
                    var13 = xOff + modelDefinition.vertexPositionsX[var12]
                    var14 = yOff + modelDefinition.vertexPositionsZ[var12]
                    var15 = var13 and 127
                    var16 = var14 and 127
                    var17 = var13 shr 7
                    var18 = var14 shr 7
                    val first = regionLoader.getTileHeight(z, baseX + var17, baseY + var18)
                    val second = regionLoader.getTileHeight(z, baseX + var17 + 1, baseY + var18)
                    val third = regionLoader.getTileHeight(z, baseX + var17, baseY + var18 + 1)
                    val fourth = regionLoader.getTileHeight(z, baseX + var17 + 1, baseY + var18 + 1)
                    var19 = first * (128 - var15) + second * var15 shr 7
                    var20 = third * (128 - var15) + var15 * fourth shr 7
                    var21 = var19 * (128 - var16) + var20 * var16 shr 7
                    model.vertexPositionsY[var12] =
                        var21 + model.modelDefinition.vertexPositionsY[var12] - height
                    ++var12
                }
            } else {
                var12 = 0
                while (var12 < modelDefinition.vertexCount) {
                    var13 = (-modelDefinition.vertexPositionsY[var12] shl 16) / height
                    if (var13 < clipType) {
                        var14 = xOff + modelDefinition.vertexPositionsX[var12]
                        var15 = yOff + modelDefinition.vertexPositionsZ[var12]
                        var16 = var14 and 127
                        var17 = var15 and 127
                        var18 = var14 shr 7
                        var19 = var15 shr 7
                        val first = regionLoader.getTileHeight(z, baseX + var18, baseY + var19)
                        val second = regionLoader.getTileHeight(z, baseX + var18 + 1, baseY + var19)
                        val third = regionLoader.getTileHeight(z, baseX + var18, baseY + var19 + 1)
                        val fourth = regionLoader.getTileHeight(z, baseX + var18 + 1, baseY + var19 + 1)
                        var20 = first * (128 - var15) + second * var15 shr 7
                        var21 = third * (128 - var15) + var15 * fourth shr 7
                        val var22 = var20 * (128 - var17) + var21 * var17 shr 7
                        model.vertexPositionsY[var12] =
                            (clipType - var13) * (var22 - height) / clipType + model.modelDefinition.vertexPositionsY[var12]
                    }
                    ++var12
                }
            }
            model.resetBounds()
            model
        }
    }

    fun scaleBy(
        x: Int,
        y: Int,
        z: Int
    ): Model =
        if (x == 128 && y == 128 && z == 128) {
            this
        } else {
            val newDef = ModelDefinition(modelDefinition)
            for (n in 0 until modelDefinition.vertexCount) {
                newDef.vertexPositionsX[n] = newDef.vertexPositionsX[n] * x / 128
                newDef.vertexPositionsY[n] = newDef.vertexPositionsY[n] * y / 128
                newDef.vertexPositionsZ[n] = newDef.vertexPositionsZ[n] * z / 128
            }
            Model(
                newDef,
                faceColors1 = faceColors1,
                faceColors2 = faceColors2,
                faceColors3 = faceColors3
            )
        }

    private fun resetBounds() {
        boundsType = 0
    }

    constructor(def: ModelDefinition, ambient: Int, contrast: Int) : this(def) {
        val x = -50
        val y = -10
        val z = -50
        val ambient = ambient + 64
        val contrast = contrast + 768
        def.computeNormals()
        def.computeTextureUVCoordinates()
        val somethingMagnitude = sqrt(z * z + x * x + (y * y).toDouble()).toInt()
        val var7 = somethingMagnitude * contrast shr 8
        val origTextureCoordinates = def.textureCoordinates
        if (def.textureTriangleCount > 0 && origTextureCoordinates != null) {
            val var9 = IntArray(def.textureTriangleCount)
            var var10 = 0
            while (var10 < def.faceCount) {
                if (origTextureCoordinates[var10].toInt() != -1) {
                    ++var9[origTextureCoordinates[var10].toInt() and 255]
                }
                ++var10
            }
            field1852 = 0
            var10 = 0
            while (var10 < def.textureTriangleCount) {
                if (var9[var10] > 0 && def.textureRenderTypes[var10].toInt() == 0) {
                    ++field1852
                }
                ++var10
            }
            field1844 = IntArray(field1852)
            field1865 = IntArray(field1852)
            field1846 = IntArray(field1852)
            var10 = 0
            for (i in 0 until def.textureTriangleCount) {
                if (var9[i] > 0 && def.textureRenderTypes[i].toInt() == 0) {
                    field1844!![var10] = def.textureTriangleVertexIndices1[i].toInt() and 0xffff
                    field1865!![var10] = def.textureTriangleVertexIndices2[i].toInt() and 0xffff
                    field1846!![var10] = def.textureTriangleVertexIndices3[i].toInt() and 0xffff
                    var9[i] = var10++
                } else {
                    var9[i] = -1
                }
            }
            field1840 = ByteArray(def.faceCount) { i ->
                val coord = origTextureCoordinates[i].toInt()
                if (coord == -1) -1
                else var9[coord and 255].toByte()
            }
        }
        val origFaceAlphas = def.faceAlphas
        for (faceIdx in 0 until def.faceCount) {
            val faceRenderTypes = def.faceRenderTypes
            var faceType =
                if (faceRenderTypes == null) 0
                else faceRenderTypes[faceIdx]
            val faceTextures = def.faceTextures
            val faceTexture: Short =
                if (faceTextures == null) -1
                else faceTextures[faceIdx]
            if (origFaceAlphas != null) {
                when (origFaceAlphas[faceIdx].toInt()) {
                    -1 -> faceType = 2
                    -2 -> faceType = 3
                }
            }
            var vertexNormal: VertexNormal
            var tmp: Int
            var faceNormal: FaceNormal
            if (faceTexture.toInt() == -1) {
                when (faceType.toInt()) {
                    0 -> {
                        val var15: Int = def.faceColors[faceIdx].toInt() and 0xffff
                        vertexNormal = def.vertexNormals!![def.faceVertexIndices1[faceIdx]]!!
                        tmp =
                            (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient
                        faceColors1[faceIdx] = method2608(var15, tmp)
                        vertexNormal = def.vertexNormals!![def.faceVertexIndices2[faceIdx]]!!
                        tmp =
                            (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient
                        faceColors2[faceIdx] = method2608(var15, tmp)
                        vertexNormal = def.vertexNormals!![def.faceVertexIndices3[faceIdx]]!!
                        tmp =
                            (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient
                        faceColors3[faceIdx] = method2608(var15, tmp)
                    }
                    1 -> {
                        faceNormal = def.faceNormals!![faceIdx]!!
                        tmp =
                            (y * faceNormal.y + z * faceNormal.z + x * faceNormal.x) / (var7 / 2 + var7) + ambient
                        faceColors1[faceIdx] =
                            method2608(
                                def.faceColors[faceIdx].toInt() and 0xffff,
                                tmp
                            )
                        faceColors3[faceIdx] = -1
                    }
                    3 -> {
                        faceColors1[faceIdx] = 128
                        faceColors3[faceIdx] = -1
                    }
                    else -> {
                        faceColors3[faceIdx] = -2
                    }
                }
            } else {
                when (faceType.toInt()) {
                    0 -> {
                        vertexNormal =
                            def.vertexNormals!![def.faceVertexIndices1[faceIdx]]!!
                        tmp =
                            (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient
                        faceColors1[faceIdx] = bound2to126(tmp)
                        vertexNormal =
                            def.vertexNormals!![def.faceVertexIndices2[faceIdx]]!!
                        tmp =
                            (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient
                        faceColors2[faceIdx] = bound2to126(tmp)
                        vertexNormal =
                            def.vertexNormals!![def.faceVertexIndices3[faceIdx]]!!
                        tmp =
                            (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient
                        faceColors3[faceIdx] = bound2to126(tmp)
                    }
                    1 -> {
                        faceNormal = def.faceNormals!![faceIdx]!!
                        tmp =
                            (y * faceNormal.y + z * faceNormal.z + x * faceNormal.x) / (var7 / 2 + var7) + ambient
                        faceColors1[faceIdx] = bound2to126(tmp)
                        faceColors3[faceIdx] = -1
                    }
                    else -> {
                        faceColors3[faceIdx] = -2
                    }
                }
            }
        }
    }

    companion object {
        fun method2608(var0: Int, var1: Int): Int {
            var var1 = var1
            var1 = (var0 and 0x007f) * var1 shr 7
            var1 = bound2to126(var1)
            return (var0 and 0xff80) + var1
        }

        fun bound2to126(var0: Int): Int {
            var var0 = var0
            if (var0 < 2) {
                var0 = 2
            } else if (var0 > 126) {
                var0 = 126
            }
            return var0
        }
    }

    override fun toString(): String {
        return "Model(${super.toString()}, x: $x y: $y, orientation: $orientation, definition: $modelDefinition)"
    }
}
