package cache.definitions

import cache.definitions.data.CircularAngle
import cache.definitions.data.FaceNormal
import cache.definitions.data.VertexNormal
import java.util.*

open class ModelDefinition {
    var id = 0
    var tag: Long = 0
    var vertexCount = 0
    var vertexPositionsX: IntArray = IntArray(0)
    var vertexPositionsY: IntArray = IntArray(0)
    var vertexPositionsZ: IntArray = IntArray(0)

    @Transient
    var vertexNormals: Array<VertexNormal?>? = null
    var faceCount = 0
    var faceVertexIndices1: IntArray? = null
    var faceVertexIndices2: IntArray? = null
    var faceVertexIndices3: IntArray? = null
    var faceAlphas: ByteArray? = null
    var faceColors: ShortArray? = null
    var faceRenderPriorities: ByteArray? = null
    var faceRenderTypes: ByteArray? = null

    @Transient
    var faceNormals: Array<FaceNormal?>? = null
    var textureTriangleCount = 0
    var textureTriangleVertexIndices1: ShortArray? = null
    var textureTriangleVertexIndices2: ShortArray? = null
    var textureTriangleVertexIndices3: ShortArray? = null

    @Transient
    var faceTextureUCoordinates: Array<FloatArray?>? = null

    @Transient
    var faceTextureVCoordinates: Array<FloatArray?>? = null
    var texturePrimaryColors: ShortArray? = null
    var faceTextures: ShortArray? = null
    var textureCoordinates: ByteArray? = null
    var textureRenderTypes: ByteArray? = null
    var vertexSkins: IntArray? = null
    var faceSkins: IntArray? = null
    var priority: Byte = 0

    @Transient
    private var vertexGroups: Array<IntArray?>? = null

    @Transient
    private var origVX: IntArray? = null

    @Transient
    private lateinit var origVY: IntArray

    @Transient
    private lateinit var origVZ: IntArray

    @Transient
    var maxPriority = 0

    lateinit var aShortArray2574: ShortArray
    lateinit var aShortArray2575: ShortArray
    lateinit var aShortArray2577: ShortArray
    lateinit var aShortArray2578: ShortArray
    lateinit var aByteArray2580: ByteArray
    lateinit var aShortArray2586: ShortArray

    constructor()
    constructor(
        original: ModelDefinition,
        shallowCopyVerts: Boolean,
        shallowCopyFaceColors: Boolean,
        shallowCopyFaceTextures: Boolean
    ) {
        vertexCount = original.vertexCount
        faceCount = original.faceCount
        textureTriangleCount = original.textureTriangleCount
        if (shallowCopyVerts) {
            vertexPositionsX = original.vertexPositionsX
            vertexPositionsY = original.vertexPositionsY
            vertexPositionsZ = original.vertexPositionsZ
        } else {
            vertexPositionsX = IntArray(vertexCount)
            vertexPositionsY = IntArray(vertexCount)
            vertexPositionsZ = IntArray(vertexCount)
            for (i in 0 until vertexCount) {
                vertexPositionsX[i] = original.vertexPositionsX[i]
                vertexPositionsY[i] = original.vertexPositionsY[i]
                vertexPositionsZ[i] = original.vertexPositionsZ[i]
            }
        }
        if (shallowCopyFaceColors) {
            faceColors = original.faceColors
        } else {
            faceColors = ShortArray(faceCount)
            for (i in 0 until faceCount) {
                faceColors!![i] = original.faceColors!![i]
            }
        }
        if (!shallowCopyFaceTextures && faceTextures != null) {
            faceTextures = ShortArray(faceCount)
            for (i in 0 until faceCount) {
                faceTextures!![i] = original.faceTextures!![i]
            }
        } else {
            faceTextures = original.faceTextures
        }
        id = original.id
        tag = original.tag
        vertexCount = original.vertexCount
        faceCount = original.faceCount
        faceVertexIndices1 = original.faceVertexIndices1
        faceVertexIndices2 = original.faceVertexIndices2
        faceVertexIndices3 = original.faceVertexIndices3
        faceRenderTypes = original.faceRenderTypes
        faceRenderPriorities = original.faceRenderPriorities
        faceAlphas = original.faceAlphas
        priority = original.priority
        textureTriangleVertexIndices1 = original.textureTriangleVertexIndices1
        textureTriangleVertexIndices2 = original.textureTriangleVertexIndices2
        textureTriangleVertexIndices3 = original.textureTriangleVertexIndices3
        faceTextureUCoordinates = original.faceTextureUCoordinates
        faceTextureVCoordinates = original.faceTextureVCoordinates
        vertexNormals = original.vertexNormals
        faceNormals = original.faceNormals
        textureCoordinates = original.textureCoordinates
        vertexSkins = original.vertexSkins
        vertexGroups = original.vertexGroups
        faceSkins = original.faceSkins
        textureRenderTypes = original.textureRenderTypes
    }

    fun computeNormals() {
        if (vertexNormals != null) {
            return
        }
        vertexNormals = arrayOfNulls(vertexCount)
        var var1: Int
        var1 = 0
        while (var1 < vertexCount) {
            vertexNormals!![var1] = VertexNormal()
            ++var1
        }
        var1 = 0
        while (var1 < faceCount) {
            val vertexA = faceVertexIndices1!![var1]
            val vertexB = faceVertexIndices2!![var1]
            val vertexC = faceVertexIndices3!![var1]
            val xA = vertexPositionsX[vertexB] - vertexPositionsX[vertexA]
            val yA = vertexPositionsY[vertexB] - vertexPositionsY[vertexA]
            val zA = vertexPositionsZ[vertexB] - vertexPositionsZ[vertexA]
            val xB = vertexPositionsX[vertexC] - vertexPositionsX[vertexA]
            val yB = vertexPositionsY[vertexC] - vertexPositionsY[vertexA]
            val zB = vertexPositionsZ[vertexC] - vertexPositionsZ[vertexA]

            // Compute cross product
            var var11 = yA * zB - yB * zA
            var var12 = zA * xB - zB * xA
            var var13 = xA * yB - xB * yA
            while (var11 > 8192 || var12 > 8192 || var13 > 8192 || var11 < -8192 || var12 < -8192 || var13 < -8192) {
                var11 = var11 shr 1
                var12 = var12 shr 1
                var13 = var13 shr 1
            }
            var length =
                Math.sqrt((var11 * var11 + var12 * var12 + var13 * var13).toDouble()).toInt()
            if (length <= 0) {
                length = 1
            }
            var11 = var11 * 256 / length
            var12 = var12 * 256 / length
            var13 = var13 * 256 / length
            var var15: Byte
            var15 = if (faceRenderTypes == null) {
                0
            } else {
                faceRenderTypes!![var1]
            }
            if (var15.toInt() == 0) {
                var var16: VertexNormal = vertexNormals!![vertexA]!!
                var16.x += var11
                var16.y += var12
                var16.z += var13
                ++var16.magnitude
                var16 = vertexNormals!![vertexB]!!
                var16.x += var11
                var16.y += var12
                var16.z += var13
                ++var16.magnitude
                var16 = vertexNormals!![vertexC]!!
                var16.x += var11
                var16.y += var12
                var16.z += var13
                ++var16.magnitude
            } else if (var15.toInt() == 1) {
                if (faceNormals == null) {
                    faceNormals = arrayOfNulls(faceCount)
                }
                faceNormals!![var1] = FaceNormal()
                val var17: FaceNormal = faceNormals!![var1]!!
                var17.x = var11
                var17.y = var12
                var17.z = var13
            }
            ++var1
        }
    }

    /**
     * Computes the UV coordinates for every three-vertex face that has a
     * texture.
     */
    fun computeTextureUVCoordinates() {
        faceTextureUCoordinates = arrayOfNulls(faceCount)
        faceTextureVCoordinates = arrayOfNulls(faceCount)
        for (i in 0 until faceCount) {
            var textureCoordinate: Int
            textureCoordinate = if (textureCoordinates == null) {
                -1
            } else {
                textureCoordinates!![i].toInt()
            }
            var textureIdx: Int
            if (faceTextures == null) {
                textureIdx = -1
            } else {
                textureIdx = faceTextures!![i].toInt() and 0xFFFF
            }
            if (textureIdx != -1) {
                val u = FloatArray(3)
                val v = FloatArray(3)
                if (textureCoordinate == -1) {
                    u[0] = 0.0f
                    v[0] = 1.0f
                    u[1] = 1.0f
                    v[1] = 1.0f
                    u[2] = 0.0f
                    v[2] = 0.0f
                } else {
                    textureCoordinate = textureCoordinate and 0xFF
                    var textureRenderType: Byte = 0
                    if (textureRenderTypes != null) {
                        textureRenderType = textureRenderTypes!![textureCoordinate]
                    }
                    if (textureRenderType.toInt() == 0) {
                        val faceVertexIdx1 = faceVertexIndices1!![i]
                        val faceVertexIdx2 = faceVertexIndices2!![i]
                        val faceVertexIdx3 = faceVertexIndices3!![i]
                        val triangleVertexIdx1 = textureTriangleVertexIndices1!![textureCoordinate]
                        val triangleVertexIdx2 = textureTriangleVertexIndices2!![textureCoordinate]
                        val triangleVertexIdx3 = textureTriangleVertexIndices3!![textureCoordinate]
                        val triangleX = vertexPositionsX[triangleVertexIdx1.toInt()].toFloat()
                        val triangleY = vertexPositionsY[triangleVertexIdx1.toInt()].toFloat()
                        val triangleZ = vertexPositionsZ[triangleVertexIdx1.toInt()].toFloat()
                        val f_882_ =
                            vertexPositionsX[triangleVertexIdx2.toInt()].toFloat() - triangleX
                        val f_883_ =
                            vertexPositionsY[triangleVertexIdx2.toInt()].toFloat() - triangleY
                        val f_884_ =
                            vertexPositionsZ[triangleVertexIdx2.toInt()].toFloat() - triangleZ
                        val f_885_ =
                            vertexPositionsX[triangleVertexIdx3.toInt()].toFloat() - triangleX
                        val f_886_ =
                            vertexPositionsY[triangleVertexIdx3.toInt()].toFloat() - triangleY
                        val f_887_ =
                            vertexPositionsZ[triangleVertexIdx3.toInt()].toFloat() - triangleZ
                        val f_888_ = vertexPositionsX[faceVertexIdx1].toFloat() - triangleX
                        val f_889_ = vertexPositionsY[faceVertexIdx1].toFloat() - triangleY
                        val f_890_ = vertexPositionsZ[faceVertexIdx1].toFloat() - triangleZ
                        val f_891_ = vertexPositionsX[faceVertexIdx2].toFloat() - triangleX
                        val f_892_ = vertexPositionsY[faceVertexIdx2].toFloat() - triangleY
                        val f_893_ = vertexPositionsZ[faceVertexIdx2].toFloat() - triangleZ
                        val f_894_ = vertexPositionsX[faceVertexIdx3].toFloat() - triangleX
                        val f_895_ = vertexPositionsY[faceVertexIdx3].toFloat() - triangleY
                        val f_896_ = vertexPositionsZ[faceVertexIdx3].toFloat() - triangleZ
                        val f_897_ = f_883_ * f_887_ - f_884_ * f_886_
                        val f_898_ = f_884_ * f_885_ - f_882_ * f_887_
                        val f_899_ = f_882_ * f_886_ - f_883_ * f_885_
                        var f_900_ = f_886_ * f_899_ - f_887_ * f_898_
                        var f_901_ = f_887_ * f_897_ - f_885_ * f_899_
                        var f_902_ = f_885_ * f_898_ - f_886_ * f_897_
                        var f_903_ = 1.0f / (f_900_ * f_882_ + f_901_ * f_883_ + f_902_ * f_884_)
                        u[0] = (f_900_ * f_888_ + f_901_ * f_889_ + f_902_ * f_890_) * f_903_
                        u[1] = (f_900_ * f_891_ + f_901_ * f_892_ + f_902_ * f_893_) * f_903_
                        u[2] = (f_900_ * f_894_ + f_901_ * f_895_ + f_902_ * f_896_) * f_903_
                        f_900_ = f_883_ * f_899_ - f_884_ * f_898_
                        f_901_ = f_884_ * f_897_ - f_882_ * f_899_
                        f_902_ = f_882_ * f_898_ - f_883_ * f_897_
                        f_903_ = 1.0f / (f_900_ * f_885_ + f_901_ * f_886_ + f_902_ * f_887_)
                        v[0] = (f_900_ * f_888_ + f_901_ * f_889_ + f_902_ * f_890_) * f_903_
                        v[1] = (f_900_ * f_891_ + f_901_ * f_892_ + f_902_ * f_893_) * f_903_
                        v[2] = (f_900_ * f_894_ + f_901_ * f_895_ + f_902_ * f_896_) * f_903_
                    }
                }
                faceTextureUCoordinates!![i] = u
                faceTextureVCoordinates!![i] = v
            }
        }
    }

    fun computeAnimationTables() {
        if (vertexSkins != null) {
            val groupCounts = IntArray(256)
            var numGroups = 0
            var var3: Int
            var var4: Int
            var3 = 0
            while (var3 < vertexCount) {
                var4 = vertexSkins!![var3]
                ++groupCounts[var4]
                if (var4 > numGroups) {
                    numGroups = var4
                }
                ++var3
            }
            vertexGroups = arrayOfNulls(numGroups + 1)
            var3 = 0
            while (var3 <= numGroups) {
                vertexGroups!![var3] = IntArray(groupCounts[var3])
                groupCounts[var3] = 0
                ++var3
            }
            var3 = 0
            while (var3 < vertexCount) {
                var4 = vertexSkins!![var3]
                vertexGroups!![var4]!![groupCounts[var4]++] = var3++
            }
            vertexSkins = null
        }

        // triangleSkinValues is here
    }

    fun rotate(orientation: Int) {
        val sin: Int = CircularAngle.SINE.get(orientation)
        val cos: Int = CircularAngle.COSINE.get(orientation)
        assert(vertexPositionsX.size == vertexPositionsY.size)
        assert(vertexPositionsY.size == vertexPositionsZ.size)
        for (i in vertexPositionsX.indices) {
            vertexPositionsX[i] = vertexPositionsX[i] * cos + vertexPositionsZ[i] * sin shr 16
            vertexPositionsZ[i] = vertexPositionsZ[i] * cos - vertexPositionsX[i] * sin shr 16
        }
        reset()
    }

    fun resetAnim() {
        if (origVX == null) {
            return
        }
        System.arraycopy(origVX, 0, vertexPositionsX, 0, origVX!!.size)
        System.arraycopy(origVY, 0, vertexPositionsY, 0, origVY.size)
        System.arraycopy(origVZ, 0, vertexPositionsZ, 0, origVZ.size)
    }

    // FrameMapDefinition = Skeleton
    // FrameDefition = Animation
//    fun animate(frames: FramesDefinition, frame: Int) {
//        if (vertexGroups == null || frame == -1) {
//            return
//        }
//        val animation: AnimationDefinition = frames.frames.get(frame)
//        val skeleton: SkeletonDefinition = animation.skeleton
//        for (i in 0 until animation.transformCount) {
//            val lbl: Int = animation.transformSkeletonLabels.get(i)
//            transform(
//                skeleton.transformTypes.get(lbl),
//                skeleton.labels.get(lbl),
//                animation.tranformXs.get(i),
//                animation.tranformYs.get(i),
//                animation.transformZs.get(i)
//            )
//        }
//    }

    fun transform(type: Int, frameMap: IntArray, dx: Int, dy: Int, dz: Int) {
        if (origVX == null) {
            origVX = Arrays.copyOf(vertexPositionsX, vertexPositionsX.size)
            origVY = Arrays.copyOf(vertexPositionsY, vertexPositionsY.size)
            origVZ = Arrays.copyOf(vertexPositionsZ, vertexPositionsZ.size)
        }
        val verticesX = vertexPositionsX
        val verticesY = vertexPositionsY
        val verticesZ = vertexPositionsZ
        val var6 = frameMap.size
        var var7: Int
        var var8: Int
        var var11: Int
        var var12: Int
        if (type == 0) {
            var7 = 0
            animOffsetX = 0
            animOffsetY = 0
            animOffsetZ = 0
            var8 = 0
            while (var8 < var6) {
                val var9 = frameMap[var8]
                if (var9 < vertexGroups!!.size) {
                    val var10 = vertexGroups!![var9]
                    var11 = 0
                    while (var11 < var10!!.size) {
                        var12 = var10[var11]
                        animOffsetX += verticesX[var12]
                        animOffsetY += verticesY[var12]
                        animOffsetZ += verticesZ[var12]
                        ++var7
                        ++var11
                    }
                }
                ++var8
            }
            if (var7 > 0) {
                animOffsetX = dx + animOffsetX / var7
                animOffsetY = dy + animOffsetY / var7
                animOffsetZ = dz + animOffsetZ / var7
            } else {
                animOffsetX = dx
                animOffsetY = dy
                animOffsetZ = dz
            }
        } else {
            var var18: IntArray?
            var var19: Int
            if (type == 1) {
                var7 = 0
                while (var7 < var6) {
                    var8 = frameMap[var7]
                    if (var8 < vertexGroups!!.size) {
                        var18 = vertexGroups!![var8]
                        var19 = 0
                        while (var19 < var18!!.size) {
                            var11 = var18[var19]
                            verticesX[var11] += dx
                            verticesY[var11] += dy
                            verticesZ[var11] += dz
                            ++var19
                        }
                    }
                    ++var7
                }
            } else if (type == 2) {
                var7 = 0
                while (var7 < var6) {
                    var8 = frameMap[var7]
                    if (var8 < vertexGroups!!.size) {
                        var18 = vertexGroups!![var8]
                        var19 = 0
                        while (var19 < var18!!.size) {
                            var11 = var18[var19]
                            verticesX[var11] -= animOffsetX
                            verticesY[var11] -= animOffsetY
                            verticesZ[var11] -= animOffsetZ
                            var12 = (dx and 255) * 8
                            val var13 = (dy and 255) * 8
                            val var14 = (dz and 255) * 8
                            var var15: Int
                            var var16: Int
                            var var17: Int
                            if (var14 != 0) {
                                var15 = CircularAngle.SINE.get(var14)
                                var16 = CircularAngle.COSINE.get(var14)
                                var17 = var15 * verticesY[var11] + var16 * verticesX[var11] shr 16
                                verticesY[var11] =
                                    var16 * verticesY[var11] - var15 * verticesX[var11] shr 16
                                verticesX[var11] = var17
                            }
                            if (var12 != 0) {
                                var15 = CircularAngle.SINE.get(var12)
                                var16 = CircularAngle.COSINE.get(var12)
                                var17 = var16 * verticesY[var11] - var15 * verticesZ[var11] shr 16
                                verticesZ[var11] =
                                    var15 * verticesY[var11] + var16 * verticesZ[var11] shr 16
                                verticesY[var11] = var17
                            }
                            if (var13 != 0) {
                                var15 = CircularAngle.SINE.get(var13)
                                var16 = CircularAngle.COSINE.get(var13)
                                var17 = var15 * verticesZ[var11] + var16 * verticesX[var11] shr 16
                                verticesZ[var11] =
                                    var16 * verticesZ[var11] - var15 * verticesX[var11] shr 16
                                verticesX[var11] = var17
                            }
                            verticesX[var11] += animOffsetX
                            verticesY[var11] += animOffsetY
                            verticesZ[var11] += animOffsetZ
                            ++var19
                        }
                    }
                    ++var7
                }
            } else if (type == 3) {
                var7 = 0
                while (var7 < var6) {
                    var8 = frameMap[var7]
                    if (var8 < vertexGroups!!.size) {
                        var18 = vertexGroups!![var8]
                        var19 = 0
                        while (var19 < var18!!.size) {
                            var11 = var18[var19]
                            verticesX[var11] -= animOffsetX
                            verticesY[var11] -= animOffsetY
                            verticesZ[var11] -= animOffsetZ
                            verticesX[var11] = dx * verticesX[var11] / 128
                            verticesY[var11] = dy * verticesY[var11] / 128
                            verticesZ[var11] = dz * verticesZ[var11] / 128
                            verticesX[var11] += animOffsetX
                            verticesY[var11] += animOffsetY
                            verticesZ[var11] += animOffsetZ
                            ++var19
                        }
                    }
                    ++var7
                }
            } else if (type == 5) {
                // alpha animation
            }
        }
    }

    fun rotateMulti() {
        var var1: Int
        var1 = 0
        while (var1 < vertexCount) {
            vertexPositionsZ[var1] = -vertexPositionsZ[var1]
            ++var1
        }
        var1 = 0
        while (var1 < faceCount) {
            val var2 = faceVertexIndices1!![var1]
            faceVertexIndices1!![var1] = faceVertexIndices3!![var1]
            faceVertexIndices3!![var1] = var2
            ++var1
        }
        reset()
    }

    fun rotateY90Ccw() {
        for (var1 in 0 until vertexCount) {
            val var2 = vertexPositionsX[var1]
            vertexPositionsX[var1] = vertexPositionsZ[var1]
            vertexPositionsZ[var1] = -var2
        }
        reset()
    }

    fun rotateY180() {
        for (var1 in 0 until vertexCount) {
            vertexPositionsX[var1] = -vertexPositionsX[var1]
            vertexPositionsZ[var1] = -vertexPositionsZ[var1]
        }
        reset()
    }

    fun rotateY270Ccw() {
        for (var1 in 0 until vertexCount) {
            val var2 = vertexPositionsZ[var1]
            vertexPositionsZ[var1] = vertexPositionsX[var1]
            vertexPositionsX[var1] = -var2
        }
        reset()
    }

    private fun reset() {
        vertexNormals = null
        faceNormals = null
//        faceTextureVCoordinates = null
//        faceTextureUCoordinates = null
    }

    fun resize(var1: Int, var2: Int, var3: Int) {
        for (var4 in 0 until vertexCount) {
            vertexPositionsX[var4] = vertexPositionsX[var4] * var1 / 128
            vertexPositionsY[var4] = var2 * vertexPositionsY[var4] / 128
            vertexPositionsZ[var4] = var3 * vertexPositionsZ[var4] / 128
        }
        reset()
    }

    fun recolor(var1: Short, var2: Short) {
        for (var3 in 0 until faceCount) {
            if (faceColors!![var3] == var1) {
                faceColors!![var3] = var2
            }
        }
    }

    fun retexture(var1: Short, var2: Short) {
        if (faceTextures != null) {
            for (var3 in 0 until faceCount) {
                if (faceTextures!![var3] == var1) {
                    faceTextures!![var3] = var2
                }
            }
        }
    }

    fun move(xOffset: Int, yOffset: Int, zOffset: Int) {
        for (i in 0 until vertexCount) {
            vertexPositionsX[i] += xOffset
            vertexPositionsY[i] += yOffset
            vertexPositionsZ[i] += zOffset
        }
        reset()
    }

    fun computeMaxPriority() {
        if (faceRenderPriorities == null) {
            return
        }
        for (i in 0 until faceCount) {
            if (faceRenderPriorities!![i] > maxPriority) {
                maxPriority = faceRenderPriorities!![i].toInt()
            }
        }
    }

    companion object {
        @Transient
        var animOffsetX = 0

        @Transient
        var animOffsetY = 0

        @Transient
        var animOffsetZ = 0
    }

    override fun toString(): String {
        return "ModelDefinition(id: $id, tag: $tag)"
    }
}





