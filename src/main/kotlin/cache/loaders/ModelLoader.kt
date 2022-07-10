package cache.loaders

import cache.IndexType
import cache.definitions.ModelDefinition
import cache.utils.read24BitInt
import cache.utils.readShortSmart
import cache.utils.readUnsignedByte
import cache.utils.readUnsignedShort
import com.displee.cache.CacheLibrary
import com.google.inject.Inject
import java.nio.ByteBuffer


class ModelLoader @Inject constructor(
    private val cacheLibrary: CacheLibrary,
    private val modelDefinitionCache: HashMap<Int, ModelDefinition> = HashMap()
) {
    fun get(modelId: Int): ModelDefinition? {
        var modelDefinition = modelDefinitionCache[modelId]
        if (modelDefinition != null) {
            //return modelDefinition
        }

        val index = cacheLibrary.index(IndexType.MODELS.id)
        val archive = index.archive(modelId and 0xFFFF) ?: return null
        val b = archive.files[0]?.data!!
        cacheLibrary.index(IndexType.MODELS.id).unCache() // free memory

        modelDefinition = ModelDefinition()
        modelDefinition.id = modelId
        if (b[b.size - 1].toInt() == -3 && b[b.size - 2].toInt() == -1) {
            decodeType3(modelDefinition, b)
        } else if (b[b.size - 1].toInt() == -2 && b[b.size - 2].toInt() == -1) {
            decodeType2(modelDefinition, b)
        } else if (b[b.size - 1].toInt() == -1 && b[b.size - 2].toInt() == -1) {
            decodeType1(modelDefinition, b)
        } else {
            decodeOldFormat(modelDefinition, b)
        }

        modelDefinition.computeNormals()
        modelDefinition.computeTextureUVCoordinates()
        // modelDefinition.computeAnimationTables()
        modelDefinitionCache[modelId] = modelDefinition
        return modelDefinition
    }

    fun decodeType3(model: ModelDefinition, var1: ByteArray) {
    }

    fun decodeType2(model: ModelDefinition, var1: ByteArray) {
    }


    fun decodeType1(def: ModelDefinition, var1: ByteArray) {
        val var2 = ByteBuffer.wrap(var1)
        val var3 = ByteBuffer.wrap(var1)
        val var4 = ByteBuffer.wrap(var1)
        val var5 = ByteBuffer.wrap(var1)
        val var6 = ByteBuffer.wrap(var1)
        val var7 = ByteBuffer.wrap(var1)
        val var8 = ByteBuffer.wrap(var1)
        var2.position(var1.size - 23)
        val var9: Int = var2.readUnsignedShort()
        val var10: Int = var2.readUnsignedShort()
        val var11: Int = var2.readUnsignedByte()
        val var12: Int = var2.readUnsignedByte()
        val var13: Int = var2.readUnsignedByte()
        val var14: Int = var2.readUnsignedByte()
        val var15: Int = var2.readUnsignedByte()
        val var16: Int = var2.readUnsignedByte()
        val var17: Int = var2.readUnsignedByte()
        val var18: Int = var2.readUnsignedShort()
        val var19: Int = var2.readUnsignedShort()
        val var20: Int = var2.readUnsignedShort()
        val var21: Int = var2.readUnsignedShort()
        val var22: Int = var2.readUnsignedShort()
        var var23 = 0
        var var24 = 0
        var var25 = 0
        if (var11 > 0) {
            def.textureRenderTypes = ByteArray(var11)
            var2.position(0)
            for (var26 in 0 until var11) {
                def.textureRenderTypes!![var26] = var2.get()
                val var27 = def.textureRenderTypes!![var26]
                if (var27.toInt() == 0) {
                    ++var23
                }
                if (var27 in 1..3) {
                    ++var24
                }
                if (var27.toInt() == 2) {
                    ++var25
                }
            }
        }
        val var28 = var11 + var9
        var var26 = var28
        if (var12 == 1) {
            var26 += var10
        }
        val var29 = var26
        val var30 = var26 + var10
        var26 = var30
        if (var13 == 255) {
            var26 += var10
        }
        val var31 = var26
        if (var15 == 1) {
            var26 += var10
        }
        val var32 = var26
        if (var17 == 1) {
            var26 += var9
        }
        val var33 = var26
        if (var14 == 1) {
            var26 += var10
        }
        val var34 = var26
        val var35 = var26 + var21
        var26 = var35
        if (var16 == 1) {
            var26 += var10 * 2
        }
        val var36 = var26
        val var37 = var26 + var22
        var26 = var37
        val var38 = var26 + var10 * 2
        var26 = var38
        val var39 = var26 + var18
        var26 = var39
        val var40 = var26 + var19
        var26 = var40
        val var41 = var26 + var20
        var26 = var41
        val var42 = var26 + var23 * 6
        var26 = var42
        val var43 = var26 + var24 * 6
        var26 = var43
        val var44 =var26 + var24 * 6
        var26 = var44
        val var45 =var26 + var24 * 2
        var26 = var45
        val var46 = var26 + var24
        var26 = var46
        var26 += var24 * 2 + var25 * 2
        def.vertexCount = var9
        def.faceCount = var10
        def.textureTriangleCount = var11
        def.vertexPositionsX = IntArray(var9)
        def.vertexPositionsY = IntArray(var9)
        def.vertexPositionsZ = IntArray(var9)
        def.faceVertexIndices1 = IntArray(var10)
        def.faceVertexIndices2 = IntArray(var10)
        def.faceVertexIndices3 = IntArray(var10)
        if (var17 == 1) {
            def.vertexSkins = IntArray(var9)
        }
        if (var12 == 1) {
            def.faceRenderTypes = ByteArray(var10)
        }
        if (var13 == 255) {
            def.faceRenderPriorities = ByteArray(var10)
        } else {
            def.priority = var13.toByte()
        }
        if (var14 == 1) {
            def.faceAlphas = ByteArray(var10)
        }
        if (var15 == 1) {
            def.faceSkins = IntArray(var10)
        }
        if (var16 == 1) {
            def.faceTextures = ShortArray(var10)
        }
        if (var16 == 1 && var11 > 0) {
            def.textureCoordinates = ByteArray(var10)
        }
        def.faceColors = ShortArray(var10)
        if (var11 > 0) {
            def.textureTriangleVertexIndices1 = ShortArray(var11)
            def.textureTriangleVertexIndices2 = ShortArray(var11)
            def.textureTriangleVertexIndices3 = ShortArray(var11)
        }
        var2.position(var11)
        var3.position(var38)
        var4.position(var39)
        var5.position(var40)
        var6.position(var32)
        var var47 = 0
        var var48 = 0
        var var49 = 0
        for (var50 in 0 until var9) {
            val var51 = var2.readUnsignedByte()
            var var52 = 0
            if (var51 and 0x1 != 0x0) {
                var52 = var3.readShortSmart()
            }
            var var53 = 0
            if (var51 and 0x2 != 0x0) {
                var53 = var4.readShortSmart()
            }
            var var54 = 0
            if (var51 and 0x4 != 0x0) {
                var54 = var5.readShortSmart()
            }
            def.vertexPositionsX[var50] = var47 + var52
            def.vertexPositionsY[var50] = var48 + var53
            def.vertexPositionsZ[var50] = var49 + var54
            var47 = def.vertexPositionsX[var50]
            var48 = def.vertexPositionsY[var50]
            var49 = def.vertexPositionsZ[var50]
            if (var17 == 1) {
                def.vertexSkins?.set(var50, var6.readUnsignedByte())
            }
        }
        var2.position(var37)
        var3.position(var28)
        var4.position(var30)
        var5.position(var33)
        var6.position(var31)
        var7.position(var35)
        var8.position(var36)
        for (var50 in 0 until var10) {
            def.faceColors!![var50] = var2.readUnsignedShort().toShort()
            if (var12 == 1) {
                def.faceRenderTypes!![var50] = var3.get()
            }
            if (var13 == 255) {
                def.faceRenderPriorities!![var50] = var4.get()
            }
            if (var14 == 1) {
                def.faceAlphas?.set(var50, var5.get())
            }
            if (var15 == 1) {
                def.faceSkins?.set(var50, var6.readUnsignedByte())
            }
            if (var16 == 1) {
                def.faceTextures!![var50] = (var7.readUnsignedShort() - 1).toShort()
            }
            if (def.textureCoordinates != null) {
                if (def.faceTextures!![var50].toInt() != -1) {
                    def.textureCoordinates!![var50] =
                        (var8.readUnsignedByte()-1).toByte()
                }
            }
        }
        var2.position(var34)
        var3.position(var29)
        var var50 = 0
        var var51 = 0
        var var52 = 0
        var var53 = 0
        for (var54 in 0 until var10) {
            val var55 = var3.readUnsignedByte()
            if (var55 == 1) {
                var50 = var2.readShortSmart() + var53
                var51 = var2.readShortSmart() + var50
                var52 = var2.readShortSmart() + var51.also {
                    var53 = it
                }
                def.faceVertexIndices1!![var54] = var50
                def.faceVertexIndices2!![var54] = var51
                def.faceVertexIndices3!![var54] = var52
            }
            if (var55 == 2) {
                var51 = var52
                var52 = var2.readShortSmart()
                    .let { var53 += it; var53 }
                def.faceVertexIndices1!![var54] = var50
                def.faceVertexIndices2!![var54] = var51
                def.faceVertexIndices3!![var54] = var52
            }
            if (var55 == 3) {
                var50 = var52
                var52 = var2.readShortSmart()
                    .let { var53 += it; var53 }
                def.faceVertexIndices1!![var54] = var50
                def.faceVertexIndices2!![var54] = var51
                def.faceVertexIndices3!![var54] = var52
            }
            if (var55 == 4) {
                val var56 = var50
                var50 = var51
                var51 = var56
                var52 = var2.readShortSmart()
                    .let { var53 += it; var53 }
                def.faceVertexIndices1!![var54] = var50
                def.faceVertexIndices2!![var54] = var56
                def.faceVertexIndices3!![var54] = var52
            }
        }
        var2.position(var41)
        var3.position(var42)
        var4.position(var43)
        var5.position(var44)
        var6.position(var45)
        var7.position(var46)
        for (var54 in 0 until var11) {
            if (def.textureRenderTypes!![var54].toInt() and 0xFF == 0) {
                def.textureTriangleVertexIndices1?.set(
                    var54,
                    var2.readUnsignedShort().toShort()
                )
                def.textureTriangleVertexIndices2?.set(
                    var54,
                    var2.readUnsignedShort().toShort()
                )
                def.textureTriangleVertexIndices3?.set(
                    var54,
                    var2.readUnsignedShort().toShort()
                )
            }
        }
        var2.position(var26)
        val var54 = var2.readUnsignedByte()
        if (var54 != 0) {
            var2.readUnsignedShort()
            var2.readUnsignedShort()
            var2.readUnsignedShort()
            var2.read24BitInt()
        }
    }

    fun decodeOldFormat(def: ModelDefinition, inputData: ByteArray) {
        var usesFaceRenderTypes = false
        var usesFaceTextures = false
        val stream1 = ByteBuffer.wrap(inputData)
        val stream2 = ByteBuffer.wrap(inputData)
        val stream3 = ByteBuffer.wrap(inputData)
        val stream4 = ByteBuffer.wrap(inputData)
        val stream5 = ByteBuffer.wrap(inputData)
        stream1.position(inputData.size - 18)
        val vertexCount = stream1.readUnsignedShort()
        val faceCount = stream1.readUnsignedShort()
        val textureCount = stream1.readUnsignedByte()
        val isTextured = stream1.readUnsignedByte()
        val faceRenderPriority = stream1.readUnsignedByte()
        val hasFaceTransparencies = stream1.readUnsignedByte()
        val hasPackedTransparencyVertexGroups = stream1.readUnsignedByte()
        val hasPackedVertexGroups = stream1.readUnsignedByte()
        val vertexXDataByteCount = stream1.readUnsignedShort()
        val vertexYDataByteCount = stream1.readUnsignedShort()
        val vertexZDataByteCount = stream1.readUnsignedShort()
        val faceIndexDataByteCount = stream1.readUnsignedShort()
        val offsetOfVertexFlags: Byte = 0
        var dataOffset = offsetOfVertexFlags + vertexCount
        val offsetOfFaceIndexCompressionTypes = dataOffset
        dataOffset += faceCount
        val offsetOfFaceRenderPriorities = dataOffset
        if (faceRenderPriority == 255) {
            dataOffset += faceCount
        }
        val offsetOfPackedTransparencyVertexGroups = dataOffset
        if (hasPackedTransparencyVertexGroups == 1) {
            dataOffset += faceCount
        }
        val offsetOfFaceTextureFlags = dataOffset
        if (isTextured == 1) {
            dataOffset += faceCount
        }
        val offsetOfPackedVertexGroups = dataOffset
        if (hasPackedVertexGroups == 1) {
            dataOffset += vertexCount
        }
        val offsetOfFaceTransparencies = dataOffset
        if (hasFaceTransparencies == 1) {
            dataOffset += faceCount
        }
        val offsetOfFaceIndexData = dataOffset
        dataOffset += faceIndexDataByteCount
        val offsetOfFaceColorsOrFaceTextures = dataOffset
        dataOffset += faceCount * 2
        val offsetOfTextureIndices = dataOffset
        dataOffset += textureCount * 6
        val offsetOfVertexXData = dataOffset
        dataOffset += vertexXDataByteCount
        val offsetOfVertexYData = dataOffset
        dataOffset += vertexYDataByteCount
        dataOffset + vertexZDataByteCount  // TODO: is this a bug? should be +=?
        def.vertexCount = vertexCount
        def.faceCount = faceCount
        def.textureTriangleCount = textureCount
        def.vertexPositionsX = IntArray(vertexCount)
        def.vertexPositionsY = IntArray(vertexCount)
        def.vertexPositionsZ = IntArray(vertexCount)
        def.faceVertexIndices1 = IntArray(faceCount)
        def.faceVertexIndices2 = IntArray(faceCount)
        def.faceVertexIndices3 = IntArray(faceCount)
        if (textureCount > 0) {
            def.textureRenderTypes = ByteArray(textureCount)
            def.textureTriangleVertexIndices1 = ShortArray(textureCount)
            def.textureTriangleVertexIndices2 = ShortArray(textureCount)
            def.textureTriangleVertexIndices3 = ShortArray(textureCount)
        }
        if (hasPackedVertexGroups == 1) {
            def.vertexSkins = IntArray(vertexCount)
        }
        if (isTextured == 1) {
            def.faceRenderTypes = ByteArray(faceCount)
            def.textureCoordinates = ByteArray(faceCount)
            def.faceTextures = ShortArray(faceCount)
        }
        if (faceRenderPriority == 255) {
            def.faceRenderPriorities = ByteArray(faceCount)
        } else {
            def.priority = faceRenderPriority.toByte()
        }
        if (hasFaceTransparencies == 1) {
            def.faceAlphas = ByteArray(faceCount)
        }
        if (hasPackedTransparencyVertexGroups == 1) {
            def.faceSkins = IntArray(faceCount)
        }
        def.faceColors = ShortArray(faceCount)
        stream1.position(offsetOfVertexFlags.toInt())
        stream2.position(offsetOfVertexXData)
        stream3.position(offsetOfVertexYData)
        stream4.position(dataOffset)
        stream5.position(offsetOfPackedVertexGroups)
        var var41 = 0
        var var33 = 0
        var var19 = 0
        var deltaY: Int
        var deltaZ: Int
        var previousIndex3: Int
        var i: Int
        var deltaX: Int
        i = 0
        while (i < vertexCount) {
            previousIndex3 = stream1.readUnsignedByte()
            deltaX = 0
            if (previousIndex3 and 1 != 0) {
                deltaX = stream2.readShortSmart()
            }
            deltaY = 0
            if (previousIndex3 and 2 != 0) {
                deltaY = stream3.readShortSmart()
            }
            deltaZ = 0
            if (previousIndex3 and 4 != 0) {
                deltaZ = stream4.readShortSmart()
            }
            def.vertexPositionsX[i] = var41 + deltaX
            def.vertexPositionsY[i] = var33 + deltaY
            def.vertexPositionsZ[i] = var19 + deltaZ
            var41 = def.vertexPositionsX[i]
            var33 = def.vertexPositionsY[i]
            var19 = def.vertexPositionsZ[i]
            if (hasPackedVertexGroups == 1) {
                def.vertexSkins!![i] = stream5.readUnsignedByte()
            }
            ++i
        }
        stream1.position(offsetOfFaceColorsOrFaceTextures)
        stream2.position(offsetOfFaceTextureFlags)
        stream3.position(offsetOfFaceRenderPriorities)
        stream4.position(offsetOfFaceTransparencies)
        stream5.position(offsetOfPackedTransparencyVertexGroups)
        i = 0
        while (i < faceCount) {
            def.faceColors!![i] = stream1.readUnsignedShort().toShort()
            if (isTextured == 1) {
                previousIndex3 = stream2.readUnsignedByte()
                if (previousIndex3 and 1 == 1) {
                    def.faceRenderTypes?.set(i, 1)
                    usesFaceRenderTypes = true
                } else {
                    def.faceRenderTypes?.set(i, 0)
                }
                if (previousIndex3 and 2 == 2) {
                    def.textureCoordinates?.set(i, (previousIndex3 shr 2).toByte())
                    def.faceTextures?.set(i, def.faceColors!![i])
                    def.faceColors!![i] = 127
                    if (def.faceTextures!![i].toInt() != -1) {
                        usesFaceTextures = true
                    }
                } else {
                    def.textureCoordinates?.set(i, -1)
                    def.faceTextures?.set(i, -1)
                }
            }
            if (faceRenderPriority == 255) {
                def.faceRenderPriorities?.set(i, stream3.get())
            }
            if (hasFaceTransparencies == 1) {
                def.faceAlphas!![i] = stream4.get()
            }
            if (hasPackedTransparencyVertexGroups == 1) {
                def.faceSkins!![i] = stream5.readUnsignedByte()
            }
            ++i
        }
        stream1.position(offsetOfFaceIndexData)
        stream2.position(offsetOfFaceIndexCompressionTypes)
        i = 0
        previousIndex3 = 0
        deltaX = 0
        deltaY = 0
        var var21: Int
        var var22: Int
        deltaZ = 0
        while (deltaZ < faceCount) {
            var22 = stream2.readUnsignedByte()
            if (var22 == 1) {
                i = stream1.readShortSmart() + deltaY
                previousIndex3 = stream1.readShortSmart() + i
                deltaX = stream1.readShortSmart() + previousIndex3
                deltaY = deltaX
                def.faceVertexIndices1!![deltaZ] = i
                def.faceVertexIndices2!![deltaZ] = previousIndex3
                def.faceVertexIndices3!![deltaZ] = deltaX
            }
            if (var22 == 2) {
                previousIndex3 = deltaX
                deltaX = stream1.readShortSmart() + deltaY
                deltaY = deltaX
                def.faceVertexIndices1!![deltaZ] = i
                def.faceVertexIndices2!![deltaZ] = previousIndex3
                def.faceVertexIndices3!![deltaZ] = deltaX
            }
            if (var22 == 3) {
                i = deltaX
                deltaX = stream1.readShortSmart() + deltaY
                deltaY = deltaX
                def.faceVertexIndices1!![deltaZ] = i
                def.faceVertexIndices2!![deltaZ] = previousIndex3
                def.faceVertexIndices3!![deltaZ] = deltaX
            }
            if (var22 == 4) {
                var21 = i
                i = previousIndex3
                previousIndex3 = var21
                deltaX = stream1.readShortSmart() + deltaY
                deltaY = deltaX
                def.faceVertexIndices1!![deltaZ] = i
                def.faceVertexIndices2!![deltaZ] = var21
                def.faceVertexIndices3!![deltaZ] = deltaX
            }
            ++deltaZ
        }
        stream1.position(offsetOfTextureIndices)
        deltaZ = 0
        while (deltaZ < textureCount) {
            def.textureRenderTypes?.set(deltaZ, 0)
            def.textureTriangleVertexIndices1!![deltaZ] = stream1.readUnsignedShort().toShort()
            def.textureTriangleVertexIndices2!![deltaZ] = stream1.readUnsignedShort().toShort()
            def.textureTriangleVertexIndices3!![deltaZ] = stream1.readUnsignedShort().toShort()
            ++deltaZ
        }
        if (def.textureCoordinates != null) {
            var usesTextureCoords = false
            var22 = 0
            while (var22 < faceCount) {
                var21 = def.textureCoordinates!![var22].toInt() and 255
                if (var21 != 255) {
                    if (def.textureTriangleVertexIndices1!![var21].toInt() and '\uffff'.toInt() == def.faceVertexIndices1!![var22] && def.textureTriangleVertexIndices2!![var21].toInt() and '\uffff'.toInt() == def.faceVertexIndices2!![var22] && def.textureTriangleVertexIndices3!![var21].toInt() and '\uffff'.toInt() == def.faceVertexIndices3!![var22]
                    ) {
                        def.textureCoordinates!![var22] = -1
                    } else {
                        usesTextureCoords = true
                    }
                }
                ++var22
            }
            if (!usesTextureCoords) {
                def.textureCoordinates = null
            }
        }
        if (!usesFaceTextures) {
            def.faceTextures = null
        }
        if (!usesFaceRenderTypes) {
            def.faceRenderTypes = null
        }
    }
}
