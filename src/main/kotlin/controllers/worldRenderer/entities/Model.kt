package controllers.worldRenderer.entities

import cache.definitions.ModelDefinition
import cache.definitions.data.FaceNormal
import cache.definitions.data.VertexNormal
import cache.loaders.RegionLoader
import cache.loaders.getTileHeight
import controllers.worldRenderer.Constants
import models.scene.SceneRegionBuilder.Companion.multiplyHslBrightness
import utils.clamp
import java.lang.ref.SoftReference
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class Model private constructor(
    val modelDefinition: ModelDefinition,
    val ambient: Int,
    val contrast: Int,
    val faceColors1: IntArray = IntArray(modelDefinition.faceCount),
    val faceColors2: IntArray = IntArray(modelDefinition.faceCount),
    val faceColors3: IntArray = IntArray(modelDefinition.faceCount)
) : Renderable {
    /** The object type corresponding to this model. For debug purposes. */
    var debugType: Int = -1
    override val computeObj = ComputeObj()
    override val renderFlags get() = super.renderFlags or (xyzMag shl 12)
    override val renderUnordered get() = false
    override val faceCount get() = modelDefinition.faceCount
    override val renderOffsetX get() = offsetX + wallAttachedOffsetX
    override var renderOffsetY = 0
    override val renderOffsetZ get() = offsetY + wallAttachedOffsetY

    var offsetX = 0
    var offsetY = 0
    var wallAttachedOffsetX = 0
    var wallAttachedOffsetY = 0

    var vertexPositionsX: IntArray = modelDefinition.vertexPositionsX
        private set
    var vertexPositionsY: IntArray = modelDefinition.vertexPositionsY
        private set
    var vertexPositionsZ: IntArray = modelDefinition.vertexPositionsZ
        private set

    var field1840: ByteArray? = null
    var field1852 = 0
    var field1844: IntArray? = null
    var field1865: IntArray? = null
    var field1846: IntArray? = null

    private var vertexNormals: Array<VertexNormal?>? = null
    private var faceRenderTypes: ByteArray? = null
    var isLit = false
        private set

    private var minXCoord = Int.MAX_VALUE
    private var maxXCoord = Int.MIN_VALUE
    private var minZCoord = Int.MAX_VALUE
    private var maxZCoord = Int.MIN_VALUE

    private var bottomY = Int.MIN_VALUE
    private var xzMag = 0
    private var xyzMag = 0
    private var diagonalMag = 0
    var boundingSphereRadiusSq = -1
        get() {
            if (field < 0) {
                computeBounds()
            }
            return field
        }
        private set

    var sceneId = -1 // scene ID in which this model was rendered

    private fun computeBounds(recalculate: Boolean = false) {
        // Check if we have already calculated this
        if (maxXCoord >= minXCoord && !recalculate) return

        var height = Int.MIN_VALUE
        bottomY = Int.MIN_VALUE
        minXCoord = Int.MAX_VALUE
        maxXCoord = Int.MIN_VALUE
        maxZCoord = Int.MIN_VALUE
        minZCoord = Int.MAX_VALUE
        var realMagSq = Int.MIN_VALUE
        for (i in 0 until modelDefinition.vertexCount) {
            val x = modelDefinition.vertexPositionsX[i]
            val y = modelDefinition.vertexPositionsY[i]
            val z = modelDefinition.vertexPositionsZ[i]
            minXCoord = min(minXCoord, x)
            maxXCoord = max(maxXCoord, x)
            height = max(height, -y)
            bottomY = max(bottomY, y)
            minZCoord = min(minZCoord, z)
            maxZCoord = max(maxZCoord, z)
            val mag = x * x + z * z
            xzMag = max(xzMag, mag)
            realMagSq = max(realMagSq, mag + y * y)
        }
        xzMag = (sqrt(xzMag.toDouble()) + 0.99).toInt()
        xyzMag = (sqrt((xzMag * xzMag + height * height).toDouble()) + 0.99).toInt()
        diagonalMag = xyzMag + (sqrt((xzMag * xzMag + bottomY * bottomY).toDouble()) + 0.99).toInt()
        boundingSphereRadiusSq = realMagSq
    }

    private fun unshareXZ() {
        if (vertexPositionsX === modelDefinition.vertexPositionsX) {
            vertexPositionsX = vertexPositionsX.clone()
            vertexPositionsZ = vertexPositionsZ.clone()
        }
    }

    private fun unshareY() {
        if (vertexPositionsY === modelDefinition.vertexPositionsY)
            vertexPositionsY = vertexPositionsY.clone()
    }

    fun contourGround(
        regionLoader: RegionLoader,
        xOff: Int,
        height: Int,
        yOff: Int,
        z: Int,
        baseX: Int,
        baseY: Int,
        clipType: Int
    ) {
        computeBounds()
        var left = xOff - xzMag
        var right = xOff + xzMag
        var top = yOff - xzMag
        var bottom = yOff + xzMag
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
        if (height == topLeft && height == topRight && height == bottomLeft && height == bottomRight) {
            return
        }

        unshareY()

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
                vertexPositionsY[var12] =
                    var21 + modelDefinition.vertexPositionsY[var12] - height
                ++var12
            }
        } else {
            var12 = 0
            while (var12 < modelDefinition.vertexCount) {
                var13 = (-modelDefinition.vertexPositionsY[var12] shl 16) / height
                if (var13 < clipType) {
                    var14 = xOff + modelDefinition.vertexPositionsX[var12]
                    var15 = yOff + modelDefinition.vertexPositionsZ[var12]
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
                    vertexPositionsY[var12] =
                        (clipType - var13) * (var22 - height) / clipType + modelDefinition.vertexPositionsY[var12]
                }
                ++var12
            }
        }

        // Not sure why this needs radius 0. Still don't understand those shaders.
        xyzMag = 0
    }

    fun rotate(angle: Int) {
        unshareXZ()

        val var2: Int = Constants.SINE[angle]
        val var3: Int = Constants.COSINE[angle]
        for (var4 in 0 until modelDefinition.vertexCount) {
            val var5: Int = var2 * this.vertexPositionsZ[var4] + var3 * this.vertexPositionsX[var4] shr 16
            this.vertexPositionsZ[var4] = var3 * this.vertexPositionsZ[var4] - var2 * this.vertexPositionsX[var4] shr 16
            this.vertexPositionsX[var4] = var5
        }
    }

    // note: not threadsafe due to shared caches
    fun mergeNormals(other: Model, xOffset: Int, yOffset: Int, zOffset: Int, hideOccludedFaces: Boolean) {
        assert(!isLit)
        computeBounds()
        modelDefinition.computeNormals()
        other.computeBounds()
        other.modelDefinition.computeNormals()
        ++normalsGeneration

        val myNormals = this.modelDefinition.vertexNormals!!
        val otherNormals = other.modelDefinition.vertexNormals!!

        val myMatchingNormalsCache = getMatchingNormalsCache(1, modelDefinition.vertexCount)
        val otherMatchingNormalsCache = getMatchingNormalsCache(2, other.modelDefinition.vertexCount)
        assert(myMatchingNormalsCache !== otherMatchingNormalsCache)

        var mergedVertices = 0
        for (i in 0 until modelDefinition.vertexCount) {
            val normal1 = myNormals[i]
            if (normal1.magnitude == 0) continue
            val y = vertexPositionsY[i] - yOffset
            if (y > other.bottomY) continue
            val x = vertexPositionsX[i] - xOffset
            if (x < other.minXCoord || x > other.maxXCoord) continue
            val z = vertexPositionsZ[i] - zOffset
            if (z < other.minZCoord || z > other.maxZCoord) continue

            for (j in 0 until other.modelDefinition.vertexCount) {
                val normal2 = otherNormals[j]
                if (x == other.vertexPositionsX[j] &&
                    z == other.vertexPositionsZ[j] &&
                    y == other.vertexPositionsY[j] &&
                    normal2.magnitude != 0
                ) {
                    getOrPutVertexNormal(i, normal1) += normal2
                    other.getOrPutVertexNormal(j, normal2) += normal1

                    ++mergedVertices
                    myMatchingNormalsCache[i] = normalsGeneration
                    otherMatchingNormalsCache[j] = normalsGeneration
                }
            }
        }

        if (mergedVertices >= 3 && hideOccludedFaces) {
            for (i in 0 until modelDefinition.faceCount) {
                if (myMatchingNormalsCache[modelDefinition.faceVertexIndices1[i]] == normalsGeneration &&
                    myMatchingNormalsCache[modelDefinition.faceVertexIndices2[i]] == normalsGeneration &&
                    myMatchingNormalsCache[modelDefinition.faceVertexIndices3[i]] == normalsGeneration
                ) {
                    ensureFaceRenderTypes()[i] = 2.toByte()
                }
            }
            for (i in 0 until other.modelDefinition.faceCount) {
                if (otherMatchingNormalsCache[other.modelDefinition.faceVertexIndices1[i]] == normalsGeneration &&
                    otherMatchingNormalsCache[other.modelDefinition.faceVertexIndices2[i]] == normalsGeneration &&
                    otherMatchingNormalsCache[other.modelDefinition.faceVertexIndices3[i]] == normalsGeneration
                ) {
                    other.ensureFaceRenderTypes()[i] = 2.toByte()
                }
            }
        }
    }

    private fun ensureFaceRenderTypes(): ByteArray =
        faceRenderTypes ?: (modelDefinition.faceRenderTypes?.clone() ?: ByteArray(modelDefinition.faceCount)).also { faceRenderTypes = it }

    private fun ensureVertexNormals(): Array<VertexNormal?> =
        vertexNormals ?: arrayOfNulls<VertexNormal>(modelDefinition.vertexCount).also { vertexNormals = it }

    private fun getVertexNormal(i: Int): VertexNormal =
        vertexNormals?.get(i) ?: modelDefinition.vertexNormals!![i]

    private fun getOrPutVertexNormal(i: Int, default: VertexNormal): VertexNormal {
        val vertexNormals = ensureVertexNormals()
        return vertexNormals[i] ?: VertexNormal(default).also { vertexNormals[i] = it }
    }

    fun light() {
        assert(!isLit)
        val def = modelDefinition
        val x = -50
        val y = -10
        val z = -50
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
            val faceRenderTypes = this.faceRenderTypes ?: modelDefinition.faceRenderTypes
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
                        vertexNormal = getVertexNormal(def.faceVertexIndices1[faceIdx])
                        tmp =
                            (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient
                        faceColors1[faceIdx] = multiplyHslBrightness(var15, tmp)
                        vertexNormal = getVertexNormal(def.faceVertexIndices2[faceIdx])
                        tmp =
                            (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient
                        faceColors2[faceIdx] = multiplyHslBrightness(var15, tmp)
                        vertexNormal = getVertexNormal(def.faceVertexIndices3[faceIdx])
                        tmp =
                            (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient
                        faceColors3[faceIdx] = multiplyHslBrightness(var15, tmp)
                    }
                    1 -> {
                        faceNormal = def.faceNormals!![faceIdx]!!
                        tmp =
                            (y * faceNormal.y + z * faceNormal.z + x * faceNormal.x) / (var7 / 2 + var7) + ambient
                        faceColors1[faceIdx] =
                            multiplyHslBrightness(
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
                        vertexNormal = getVertexNormal(def.faceVertexIndices1[faceIdx])
                        tmp =
                            (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient
                        faceColors1[faceIdx] = tmp.clamp(2, 126)
                        vertexNormal = getVertexNormal(def.faceVertexIndices2[faceIdx])
                        tmp =
                            (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient
                        faceColors2[faceIdx] = tmp.clamp(2, 126)
                        vertexNormal = getVertexNormal(def.faceVertexIndices3[faceIdx])
                        tmp =
                            (y * vertexNormal.y + z * vertexNormal.z + x * vertexNormal.x) / (var7 * vertexNormal.magnitude) + ambient
                        faceColors3[faceIdx] = tmp.clamp(2, 126)
                    }
                    1 -> {
                        faceNormal = def.faceNormals!![faceIdx]!!
                        tmp =
                            (y * faceNormal.y + z * faceNormal.z + x * faceNormal.x) / (var7 / 2 + var7) + ambient
                        faceColors1[faceIdx] = tmp.clamp(2, 126)
                        faceColors3[faceIdx] = -1
                    }
                    else -> {
                        faceColors3[faceIdx] = -2
                    }
                }
            }
        }
        isLit = true
    }

    companion object {
        private var normalsGeneration = 0
        private var matchingNormalsRef1 = SoftReference<IntArray>(null)
        private var matchingNormalsRef2 = SoftReference<IntArray>(null)

        fun getMatchingNormalsCache(which: Int, capacity: Int): IntArray {
            var cache = (if (which == 2) matchingNormalsRef2 else matchingNormalsRef1).get()
            if (cache != null && cache.size >= capacity) return cache

            cache = IntArray(capacity)
            if (which == 2)
                matchingNormalsRef2 = SoftReference(cache)
            else
                matchingNormalsRef1 = SoftReference(cache)
            return cache
        }

        fun lightFromDefinition(def: ModelDefinition, ambient: Int, contrast: Int) =
            Model(def, ambient + 64, contrast + 768).apply { light() }

        fun unlitFromDefinition(modelDefinition: ModelDefinition, ambient: Int, contrast: Int): Model {
            val faceColors = IntArray(modelDefinition.faceCount) { 960 /* red */ }
            return Model(
                modelDefinition,
                faceColors1 = faceColors,
                faceColors2 = faceColors.clone(),
                faceColors3 = faceColors.clone(),
                ambient = ambient + 64,
                contrast = contrast + 768,
            )
        }
    }

    override fun toString(): String {
        return "Model(${super.toString()}, definition: $modelDefinition)"
    }
}
