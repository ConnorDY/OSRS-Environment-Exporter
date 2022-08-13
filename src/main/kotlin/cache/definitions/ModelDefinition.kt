package cache.definitions

import cache.definitions.data.FaceNormal
import cache.definitions.data.VertexNormal
import controllers.worldRenderer.Constants.COSINE
import controllers.worldRenderer.Constants.SINE

open class ModelDefinition(
    val id: Int,
    val vertexCount: Int,
    val vertexPositionsX: IntArray,
    val vertexPositionsY: IntArray,
    val vertexPositionsZ: IntArray,
    val faceCount: Int,
    val faceVertexIndices1: IntArray,
    val faceVertexIndices2: IntArray,
    val faceVertexIndices3: IntArray,
    val faceAlphas: ByteArray?,
    val faceColors: ShortArray,
    val faceRenderPriorities: ByteArray?,
    val faceRenderTypes: ByteArray?,
    val textureTriangleCount: Int,
    val textureTriangleVertexIndices1: ShortArray,
    val textureTriangleVertexIndices2: ShortArray,
    val textureTriangleVertexIndices3: ShortArray,
    val faceTextures: ShortArray?,
    val textureCoordinates: ByteArray?,
    val textureRenderTypes: ByteArray,
    var vertexSkins: IntArray?,
    val faceSkins: IntArray?,
    val priority: Byte
) {
    var tag: Long = 0

    @Transient
    var vertexNormals: Array<VertexNormal?>? = null

    @Transient
    var faceNormals: Array<FaceNormal?>? = null

    @Transient
    var faceTextureUVCoordinates: FloatArray? = null

    @Transient
    private var vertexGroups: Array<IntArray?>? = null

    constructor(
        original: ModelDefinition
    ) : this(
        id = original.id,
        vertexCount = original.vertexCount,
        vertexPositionsX = original.vertexPositionsX.clone(),
        vertexPositionsY = original.vertexPositionsY.clone(),
        vertexPositionsZ = original.vertexPositionsZ.clone(),
        faceCount = original.faceCount,
        faceVertexIndices1 = original.faceVertexIndices1.clone(),
        faceVertexIndices2 = original.faceVertexIndices2.clone(),
        faceVertexIndices3 = original.faceVertexIndices3.clone(),
        faceAlphas = original.faceAlphas,
        faceColors = original.faceColors.clone(),
        faceRenderPriorities = original.faceRenderPriorities,
        faceRenderTypes = original.faceRenderTypes,
        textureTriangleCount = original.textureTriangleCount,
        textureTriangleVertexIndices1 = original.textureTriangleVertexIndices1,
        textureTriangleVertexIndices2 = original.textureTriangleVertexIndices2,
        textureTriangleVertexIndices3 = original.textureTriangleVertexIndices3,
        faceTextures = original.faceTextures?.clone(),
        textureCoordinates = original.textureCoordinates,
        textureRenderTypes = original.textureRenderTypes,
        vertexSkins = original.vertexSkins,
        faceSkins = original.faceSkins,
        priority = original.priority
    ) {
        tag = original.tag
        faceTextureUVCoordinates = original.faceTextureUVCoordinates
        vertexNormals = original.vertexNormals
        faceNormals = original.faceNormals
        vertexGroups = original.vertexGroups
    }

    fun computeNormals() {
        if (vertexNormals != null) {
            return
        }
        vertexNormals = arrayOfNulls(vertexCount)
        var var1 = 0
        while (var1 < vertexCount) {
            vertexNormals!![var1] = VertexNormal()
            ++var1
        }
        var1 = 0
        while (var1 < faceCount) {
            val vertexA = faceVertexIndices1[var1]
            val vertexB = faceVertexIndices2[var1]
            val vertexC = faceVertexIndices3[var1]
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
            val faceRenderTypes = faceRenderTypes
            val var15: Byte =
                if (faceRenderTypes == null) 0
                else faceRenderTypes[var1]
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
        val uv = FloatArray(6 * faceCount)
        for (face in 0 until faceCount) {
            var textureCoordinate =
                if (textureCoordinates == null) -1
                else textureCoordinates[face].toInt()
            val textureIdx: Int =
                if (faceTextures == null) -1
                else faceTextures[face].toInt() and 0xFFFF
            if (textureIdx != -1) {
                val idx = face * 6
                if (textureCoordinate == -1) {
                    /* ktlint-disable no-multi-spaces */
                    uv[idx    ] = 0.0f
                    uv[idx + 1] = 1.0f

                    uv[idx + 2] = 1.0f
                    uv[idx + 3] = 1.0f

                    uv[idx + 4] = 0.0f
                    uv[idx + 5] = 0.0f
                    /* ktlint-enable no-multi-spaces */
                } else {
                    textureCoordinate = textureCoordinate and 0xFF
                    val textureRenderType: Byte = textureRenderTypes[textureCoordinate]
                    if (textureRenderType.toInt() == 0) {
                        val faceVertexIdx1 = faceVertexIndices1[face]
                        val faceVertexIdx2 = faceVertexIndices2[face]
                        val faceVertexIdx3 = faceVertexIndices3[face]
                        val triangleVertexIdx1 = textureTriangleVertexIndices1[textureCoordinate]
                        val triangleVertexIdx2 = textureTriangleVertexIndices2[textureCoordinate]
                        val triangleVertexIdx3 = textureTriangleVertexIndices3[textureCoordinate]
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
                        /* ktlint-disable no-multi-spaces */
                        uv[idx    ] = (f_900_ * f_888_ + f_901_ * f_889_ + f_902_ * f_890_) * f_903_
                        uv[idx + 2] = (f_900_ * f_891_ + f_901_ * f_892_ + f_902_ * f_893_) * f_903_
                        uv[idx + 4] = (f_900_ * f_894_ + f_901_ * f_895_ + f_902_ * f_896_) * f_903_
                        f_900_ = f_883_ * f_899_ - f_884_ * f_898_
                        f_901_ = f_884_ * f_897_ - f_882_ * f_899_
                        f_902_ = f_882_ * f_898_ - f_883_ * f_897_
                        f_903_ = 1.0f / (f_900_ * f_885_ + f_901_ * f_886_ + f_902_ * f_887_)
                        uv[idx + 1] = (f_900_ * f_888_ + f_901_ * f_889_ + f_902_ * f_890_) * f_903_
                        uv[idx + 3] = (f_900_ * f_891_ + f_901_ * f_892_ + f_902_ * f_893_) * f_903_
                        uv[idx + 5] = (f_900_ * f_894_ + f_901_ * f_895_ + f_902_ * f_896_) * f_903_
                        /* ktlint-enable no-multi-spaces */
                    }
                }
            }
        }
        this.faceTextureUVCoordinates = uv
    }

    fun computeAnimationTables() {
        if (vertexSkins != null) {
            val groupCounts = IntArray(256)
            var maxGroupIndex = 0
            for (var3 in 0 until vertexCount) {
                val groupIndex = vertexSkins!![var3]
                ++groupCounts[groupIndex]
                if (groupIndex > maxGroupIndex) {
                    maxGroupIndex = groupIndex
                }
            }
            vertexGroups = Array(maxGroupIndex + 1) {
                val item = IntArray(groupCounts[it])
                groupCounts[it] = 0
                item
            }
            for (var3 in 0 until vertexCount) {
                val var4 = vertexSkins!![var3]
                vertexGroups!![var4]!![groupCounts[var4]++] = var3
            }
            vertexSkins = null
        }

        // triangleSkinValues is here
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

    fun rotateMulti() {
        for (var1 in 0 until vertexCount) {
            vertexPositionsZ[var1] = -vertexPositionsZ[var1]
        }
        for (var1 in 0 until faceCount) {
            val var2 = faceVertexIndices1[var1]
            faceVertexIndices1[var1] = faceVertexIndices3[var1]
            faceVertexIndices3[var1] = var2
        }
        reset()
    }

    fun rotate(angle: Int) {
        // method2663
        val var2: Int = SINE[angle]
        val var3: Int = COSINE[angle]
        for (var4 in 0 until vertexCount) {
            val var5: Int = var2 * this.vertexPositionsZ[var4] + var3 * this.vertexPositionsX[var4] shr 16
            this.vertexPositionsZ[var4] = var3 * this.vertexPositionsZ[var4] - var2 * this.vertexPositionsX[var4] shr 16
            this.vertexPositionsX[var4] = var5
        }
        this.reset()
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
    }

    fun recolor(var1: Short, var2: Short) {
        val faceColors = faceColors
        for (var3 in 0 until faceCount) {
            if (faceColors[var3] == var1) {
                faceColors[var3] = var2
            }
        }
    }

    fun retexture(var1: Short, var2: Short) {
        val faceTextures = faceTextures
        if (faceTextures != null) {
            for (var3 in 0 until faceCount) {
                if (faceTextures[var3] == var1) {
                    faceTextures[var3] = var2
                }
            }
        }
    }

    fun scale(x: Int, y: Int, z: Int) {
        if (x != 128 || y != 128 || z != 128) {
            for (n in 0 until vertexCount) {
                vertexPositionsX[n] = vertexPositionsX[n] * x / 128
                vertexPositionsY[n] = vertexPositionsY[n] * y / 128
                vertexPositionsZ[n] = vertexPositionsZ[n] * z / 128
            }
            reset()
        }
    }

    fun translate(x: Int, y: Int, z: Int) {
        if (x != 0 || y != 0 || z != 0) {
            for (n in 0 until vertexCount) {
                vertexPositionsX[n] += x
                vertexPositionsY[n] += y
                vertexPositionsZ[n] += z
            }
            reset()
        }
    }

    override fun toString(): String {
        return "ModelDefinition(id: $id, tag: $tag)"
    }

    companion object {
        private fun addOrNull(first: ByteArray?, second: ByteArray?, firstLen: Int, secondLen: Int, default: Byte = 0): ByteArray? {
            if (first == null && second == null) return null
            return (first ?: ByteArray(firstLen) { default }) + (second ?: ByteArray(secondLen) { default })
        }

        private fun addOrNull(first: ShortArray?, second: ShortArray?, firstLen: Int, secondLen: Int, default: Short = 0): ShortArray? {
            if (first == null && second == null) return null
            return (first ?: ShortArray(firstLen) { default }) + (second ?: ShortArray(secondLen) { default })
        }

        private fun addOrNull(first: IntArray?, second: IntArray?): IntArray? {
            if (first == null) return second
            if (second == null) return first
            return first + second
        }

        private fun concatIndices(first: IntArray, second: IntArray, shift: Int): IntArray {
            return first + second.map { it + shift }
        }

        private fun concatIndices(first: ShortArray, second: ShortArray, shift: Int): ShortArray {
            val shift = first.size
            return first + second.map { (it + shift).toShort() }
        }

        fun combine(
            first: ModelDefinition,
            second: ModelDefinition
        ): ModelDefinition {
            val def = ModelDefinition(
                id = first.id,
                vertexCount = first.vertexCount + second.vertexCount,
                vertexPositionsX = first.vertexPositionsX + second.vertexPositionsX,
                vertexPositionsY = first.vertexPositionsY + second.vertexPositionsY,
                vertexPositionsZ = first.vertexPositionsZ + second.vertexPositionsZ,
                faceCount = first.faceCount + second.faceCount,
                faceVertexIndices1 = concatIndices(first.faceVertexIndices1, second.faceVertexIndices1, first.vertexCount),
                faceVertexIndices2 = concatIndices(first.faceVertexIndices2, second.faceVertexIndices2, first.vertexCount),
                faceVertexIndices3 = concatIndices(first.faceVertexIndices3, second.faceVertexIndices3, first.vertexCount),
                faceAlphas = addOrNull(first.faceAlphas, second.faceAlphas, first.faceCount, second.faceCount),
                faceColors = first.faceColors + second.faceColors,
                faceRenderPriorities = addOrNull(first.faceRenderPriorities, second.faceRenderPriorities, first.faceCount, second.faceCount),
                faceRenderTypes = addOrNull(first.faceRenderTypes, second.faceRenderTypes, first.faceCount, second.faceCount),
                textureTriangleCount = first.textureTriangleCount + second.textureTriangleCount,
                textureTriangleVertexIndices1 = concatIndices(first.textureTriangleVertexIndices1, second.textureTriangleVertexIndices1, first.vertexCount),
                textureTriangleVertexIndices2 = concatIndices(first.textureTriangleVertexIndices2, second.textureTriangleVertexIndices2, first.vertexCount),
                textureTriangleVertexIndices3 = concatIndices(first.textureTriangleVertexIndices3, second.textureTriangleVertexIndices3, first.vertexCount),
                faceTextures = addOrNull(first.faceTextures, second.faceTextures, first.faceCount, second.faceCount, -1),
                textureCoordinates = addOrNull(first.textureCoordinates, second.textureCoordinates, first.faceCount, second.faceCount, -1),
                textureRenderTypes = first.textureRenderTypes + second.textureRenderTypes,
                vertexSkins = addOrNull(first.vertexSkins, second.vertexSkins),
                faceSkins = addOrNull(first.faceSkins, second.faceSkins),
                priority = first.priority
            )
            def.computeNormals()
            def.computeTextureUVCoordinates()
            def.computeAnimationTables()
            return def
        }
    }
}
