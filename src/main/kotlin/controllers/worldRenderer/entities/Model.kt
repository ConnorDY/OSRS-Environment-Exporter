package controllers.worldRenderer.entities

import cache.definitions.ModelDefinition
import cache.definitions.RegionDefinition
import cache.definitions.data.FaceNormal
import cache.definitions.data.VertexNormal
import cache.loaders.RegionLoader
import controllers.worldRenderer.Constants
import controllers.worldRenderer.SceneUploader
import controllers.worldRenderer.helpers.GpuIntBuffer
import controllers.worldRenderer.helpers.ModelBuffers
import controllers.worldRenderer.helpers.ModelBuffers.Companion.MAX_TRIANGLE
import java.nio.IntBuffer
import kotlin.math.min
import kotlin.math.sqrt

class Model(
    var modelDefinition: ModelDefinition,
    val computeObj: ComputeObj = ComputeObj()
) : Renderable() {

    var faceColors1: IntArray? = null
    var faceColors2: IntArray? = null
    var faceColors3: IntArray? = null
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

    fun drawPersistent(modelBuffers: ModelBuffers, sceneX: Int, sceneY: Int, height: Int, objType: Int) {
        val x: Int = sceneX * Constants.LOCAL_TILE_SIZE + xOff
        val z: Int = sceneY * Constants.LOCAL_TILE_SIZE + yOff

        val b: GpuIntBuffer = modelBuffers.bufferForTriangles(min(MAX_TRIANGLE, modelDefinition.faceCount))
        b.ensureCapacity(13)

        computeObj.idx = modelBuffers.targetBufferOffset
        computeObj.flags = (radius shl 12) or orientationType.id
        computeObj.x = x
        computeObj.y = height
        computeObj.z = z
        computeObj.pickerId = modelBuffers.calcPickerId(sceneX, sceneY, objType)
        b.buffer.put(computeObj.toArray())

        modelBuffers.addTargetBufferOffset(computeObj.size * 3)
    }

    override fun drawDynamic(modelBuffers: ModelBuffers, sceneX: Int, sceneY: Int, height: Int) {
        val x: Int = sceneX * Constants.LOCAL_TILE_SIZE + xOff
        val z: Int = sceneY * Constants.LOCAL_TILE_SIZE + yOff

        val b: GpuIntBuffer = modelBuffers.bufferForTriangles(min(MAX_TRIANGLE, modelDefinition.faceCount))
        b.ensureCapacity(13)

        computeObj.idx = modelBuffers.targetBufferOffset + modelBuffers.tempOffset
        computeObj.flags = (radius shl 12) or orientationType.id
        computeObj.x = x
        computeObj.y = height
        computeObj.z = z
        computeObj.pickerId = -2
        b.buffer.put(computeObj.toArray())

        modelBuffers.addTempOffset(computeObj.size * 3)
        modelBuffers.addTempUvOffset(computeObj.size * 3)
    }

    override fun clearDraw(modelBuffers: ModelBuffers) {
        val b: GpuIntBuffer = modelBuffers.bufferForTriangles(min(MAX_TRIANGLE, modelDefinition.faceCount))
        b.ensureCapacity(13)

        // FIXME: hack to make it look like the object has been removed..
        computeObj.x = Int.MAX_VALUE
        computeObj.y = Int.MAX_VALUE
        computeObj.z = Int.MAX_VALUE
        b.buffer.put(computeObj.toArray())
    }

    private fun getTileHeight(regionLoader: RegionLoader, x: Int, y: Int): Int {
        val r: RegionDefinition = regionLoader.findRegionForWorldCoordinates(x, y) ?: return 0
        return r.tileHeights[0][x % 64][y % 64]
    }

    private fun calculateBoundsCylinder() {
        if (boundsType != 1) {
            boundsType = 1
            bottomY = 0
            xYZMag = 0
            super.height = 0
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
        val topLeft = getTileHeight(regionLoader, baseX + left, baseY + top)
        val topRight = getTileHeight(regionLoader, baseX + right, baseY + top)
        val bottomLeft = getTileHeight(regionLoader, baseX + left, baseY + bottom)
        val bottomRight = getTileHeight(regionLoader, baseX + right, baseY + bottom)
        return if (height == topLeft && height == topRight && height == bottomLeft && height == bottomRight) {
            this
        } else {
            val model: Model
            if (deepCopy) {
                modelDefinition = ModelDefinition(
                    modelDefinition,
                    shallowCopyVerts = true,
                    shallowCopyFaceColors = true,
                    shallowCopyFaceTextures = true
                )
                modelDefinition.vertexPositionsY = IntArray(modelDefinition.vertexCount)
                model = Model(modelDefinition)
                model.faceColors1 = faceColors1
                model.faceColors2 = faceColors2
                model.faceColors3 = faceColors3
            } else {
                model = this
            }
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
                while (var12 < model.modelDefinition.vertexCount) {
                    var13 = xOff + modelDefinition.vertexPositionsX[var12]
                    var14 = yOff + modelDefinition.vertexPositionsZ[var12]
                    var15 = var13 and 127
                    var16 = var14 and 127
                    var17 = var13 shr 7
                    var18 = var14 shr 7
                    val first = getTileHeight(regionLoader, baseX + var17, baseY + var18)
                    val second = getTileHeight(regionLoader, baseX + var17 + 1, baseY + var18)
                    val third = getTileHeight(regionLoader, baseX + var17, baseY + var18 + 1)
                    val fourth = getTileHeight(regionLoader, baseX + var17 + 1, baseY + var18 + 1)
                    var19 = first * (128 - var15) + second * var15 shr 7
                    var20 = third * (128 - var15) + var15 * fourth shr 7
                    var21 = var19 * (128 - var16) + var20 * var16 shr 7
                    model.modelDefinition.vertexPositionsY[var12] =
                        var21 + this.modelDefinition.vertexPositionsY[var12] - height
                    ++var12
                }
            } else {
                var12 = 0
                while (var12 < model.modelDefinition.vertexCount) {
                    var13 = (-modelDefinition.vertexPositionsY[var12] shl 16) / super.height
                    if (var13 < clipType) {
                        var14 = xOff + modelDefinition.vertexPositionsX[var12]
                        var15 = yOff + modelDefinition.vertexPositionsZ[var12]
                        var16 = var14 and 127
                        var17 = var15 and 127
                        var18 = var14 shr 7
                        var19 = var15 shr 7
                        val first = getTileHeight(regionLoader, baseX + var18, baseY + var19)
                        val second = getTileHeight(regionLoader, baseX + var18 + 1, baseY + var19)
                        val third = getTileHeight(regionLoader, baseX + var18, baseY + var19 + 1)
                        val fourth = getTileHeight(regionLoader, baseX + var18 + 1, baseY + var19 + 1)
                        var20 = first * (128 - var15) + second * var15 shr 7
                        var21 = third * (128 - var15) + var15 * fourth shr 7
                        val var22 = var20 * (128 - var17) + var21 * var17 shr 7
                        model.modelDefinition.vertexPositionsY[var12] =
                            (clipType - var13) * (var22 - height) / clipType + modelDefinition.vertexPositionsY[var12]
                    }
                    ++var12
                }
            }
            model.resetBounds()
            model
        }
    }

    private fun resetBounds() {
        boundsType = 0
    }

    constructor(def: ModelDefinition, ambient: Int, contrast: Int) : this(def) {
        def.computeNormals()
        def.computeTextureUVCoordinates()
        val x = -50
        val y = -10
        val z = -50
        val ambient = ambient + 64
        val contrast = contrast + 768
        def.computeNormals()
        def.computeTextureUVCoordinates()
        val somethingMagnitude = sqrt(z * z + x * x + (y * y).toDouble()).toInt()
        val var7 = somethingMagnitude * contrast shr 8
        faceColors1 = IntArray(def.faceCount)
        faceColors2 = IntArray(def.faceCount)
        faceColors3 = IntArray(def.faceCount)
        if (def.textureTriangleCount > 0 && def.textureCoordinates != null) {
            val var9 = IntArray(def.textureTriangleCount)
            var var10 = 0
            while (var10 < def.faceCount) {
                if (def.textureCoordinates!![var10].toInt() != -1) {
                    ++var9[def.textureCoordinates!![var10].toInt() and 255]
                }
                ++var10
            }
            field1852 = 0
            var10 = 0
            val textureRenderTypes = def.textureRenderTypes!!
            while (var10 < def.textureTriangleCount) {
                if (var9[var10] > 0 && textureRenderTypes[var10].toInt() == 0) {
                    ++field1852
                }
                ++var10
            }
            field1844 = IntArray(field1852)
            field1865 = IntArray(field1852)
            field1846 = IntArray(field1852)
            var10 = 0
            for (i in 0 until def.textureTriangleCount) {
                if (var9[i] > 0 && textureRenderTypes[i].toInt() == 0) {
                    field1844!![var10] = def.textureTriangleVertexIndices1!![i].toInt() and '\uffff'.toInt()
                    field1865!![var10] = def.textureTriangleVertexIndices2!![i].toInt() and '\uffff'.toInt()
                    field1846!![var10] = def.textureTriangleVertexIndices3!![i].toInt() and '\uffff'.toInt()
                    var9[i] = var10++
                } else {
                    var9[i] = -1
                }
            }
            field1840 = ByteArray(def.faceCount)
            for (i in 0 until def.faceCount) {
                if (def.textureCoordinates!![i].toInt() != -1) {
                    field1840!![i] = var9[def.textureCoordinates!![i].toInt() and 255].toByte()
                } else {
                    field1840!![i] = -1
                }
            }
        }
        for (faceIdx in 0 until def.faceCount) {
            var faceType: Byte
            faceType = if (def.faceRenderTypes == null) {
                0
            } else {
                def.faceRenderTypes!![faceIdx]
            }
            val faceAlpha: Byte = if (def.faceAlphas == null) {
                0
            } else {
                def.faceAlphas!![faceIdx]
            }
            val faceTexture: Short = if (def.faceTextures == null) {
                -1
            } else {
                def.faceTextures!![faceIdx]
            }
            if (faceAlpha.toInt() == -2) {
                faceType = 3
            }
            if (faceAlpha.toInt() == -1) {
                faceType = 2
            }
            var vertexNormal: VertexNormal
            var tmp: Int
            var faceNormal: FaceNormal
            if (faceTexture.toInt() == -1) {
                if (faceType.toInt() != 0) {
                    when {
                        faceType.toInt() == 1 -> {
                            faceNormal = def.faceNormals!![faceIdx]!!
                            tmp =
                                (y * faceNormal.y + z * faceNormal.z + x * faceNormal.x) / (var7 / 2 + var7) + ambient
                            faceColors1!![faceIdx] =
                                method2608(def.faceColors!![faceIdx].toInt() and '\uffff'.toInt(), tmp)
                            faceColors3!![faceIdx] = -1
                        }
                        faceType.toInt() == 3 -> {
                            faceColors1!![faceIdx] = 128
                            faceColors3!![faceIdx] = -1
                        }
                        else -> {
                            faceColors3!![faceIdx] = -2
                        }
                    }
                } else {
                    val var15: Int = def.faceColors!![faceIdx].toInt() and '\uffff'.toInt()
                    vertexNormal = def.vertexNormals!![def.faceVertexIndices1!![faceIdx]]!!
                    tmp =
                        (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient
                    faceColors1!![faceIdx] = method2608(var15, tmp)
                    vertexNormal = def.vertexNormals!![def.faceVertexIndices2!![faceIdx]]!!
                    tmp =
                        (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient
                    faceColors2!![faceIdx] = method2608(var15, tmp)
                    vertexNormal = def.vertexNormals!![def.faceVertexIndices3!![faceIdx]]!!
                    tmp =
                        (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient
                    faceColors3!![faceIdx] = method2608(var15, tmp)
                }
            } else if (faceType.toInt() != 0) {
                if (faceType.toInt() == 1) {
                    faceNormal = def.faceNormals!![faceIdx]!!
                    tmp = (y * faceNormal.y + z * faceNormal.z + x * faceNormal.x) / (var7 / 2 + var7) + ambient
                    faceColors1!![faceIdx] = bound2to126(tmp)
                    faceColors3!![faceIdx] = -1
                } else {
                    faceColors3!![faceIdx] = -2
                }
            } else {
                vertexNormal = def.vertexNormals!![def.faceVertexIndices1!![faceIdx]]!!
                tmp =
                    (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient
                faceColors1!![faceIdx] = bound2to126(tmp)
                vertexNormal = def.vertexNormals!![def.faceVertexIndices2!![faceIdx]]!!
                tmp =
                    (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient
                faceColors2!![faceIdx] = bound2to126(tmp)
                vertexNormal = def.vertexNormals!![def.faceVertexIndices3!![faceIdx]]!!
                tmp =
                    (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient
                faceColors3!![faceIdx] = bound2to126(tmp)
            }
        }
        modelDefinition.id = def.id
        modelDefinition.vertexCount = def.vertexCount
        modelDefinition.vertexPositionsX = def.vertexPositionsX
        modelDefinition.vertexPositionsY = def.vertexPositionsY
        modelDefinition.vertexPositionsZ = def.vertexPositionsZ
        modelDefinition.faceCount = def.faceCount
        modelDefinition.faceVertexIndices1 = def.faceVertexIndices1
        modelDefinition.faceVertexIndices2 = def.faceVertexIndices2
        modelDefinition.faceVertexIndices3 = def.faceVertexIndices3
        modelDefinition.faceRenderPriorities = def.faceRenderPriorities
        modelDefinition.faceTextureUCoordinates = def.faceTextureUCoordinates
        modelDefinition.faceTextureVCoordinates = def.faceTextureVCoordinates
        modelDefinition.faceAlphas = def.faceAlphas
        modelDefinition.priority = def.priority
        modelDefinition.faceTextures = def.faceTextures
    }

    companion object {
        fun method2608(var0: Int, var1: Int): Int {
            var var1 = var1
            var1 = (var0 and 127) * var1 shr 7
            var1 = bound2to126(var1)
            return (var0 and 65408) + var1
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
}