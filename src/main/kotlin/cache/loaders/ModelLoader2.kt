package cache.loaders

import cache.IndexType
import cache.definitions.ModelDefinition
import cache.utils.readShortSmart
import cache.utils.readUnsignedByte
import cache.utils.readUnsignedShort
import com.displee.cache.CacheLibrary
import com.displee.cache.index.archive.Archive
import com.displee.cache.index.ReferenceTable
import com.google.inject.Inject
import java.nio.ByteBuffer
import kotlin.jvm.internal.Intrinsics

class ModelLoader2 @Inject constructor(
    private val cacheLibrary: CacheLibrary,
    private val modelDefinitionCache: HashMap<Int, ModelDefinition>
) {
    operator fun get(modelId: Int): ModelDefinition? {
        var modelDefinition = modelDefinitionCache[modelId]
        if (modelDefinition != null) {
            return modelDefinition
        }

        val index = cacheLibrary.index(IndexType.MODELS.id)
        val archive: Archive = index.archive(modelId and 0xffff) ?: return null

        val var6 = archive.files[0]
        val b = var6?.data!!
        cacheLibrary.index(IndexType.MODELS.id).unCache()
        modelDefinition = ModelDefinition()
        modelDefinition.id = modelId
        if (b[b.size - 1].toInt() == -3 && b[b.size - 2].toInt() == -1) {
            decodeType3(modelDefinition, b)
        } else if (b[b.size - 1].toInt() == -2 && b[b.size - 2].toInt() == -1) {
            decodeType2(modelDefinition, b)
        } else if (b[b.size - 1].toInt() == -1 && b[b.size - 2].toInt() == -1) {
            load1(modelDefinition, b)
        } else {
            loadOldFormat(modelDefinition, b)
        }
        modelDefinition.computeNormals()
        modelDefinition.computeTextureUVCoordinates()
        modelDefinition.computeAnimationTables()
        modelDefinitionCache[modelId] = modelDefinition
        return modelDefinition
    }

    private fun load1(model: ModelDefinition, var1: ByteArray) {
        val var2 = ByteBuffer.wrap(var1)
        val var24 = ByteBuffer.wrap(var1)
        val var3 = ByteBuffer.wrap(var1)
        val var28 = ByteBuffer.wrap(var1)
        val var6 = ByteBuffer.wrap(var1)
        val var55 = ByteBuffer.wrap(var1)
        val var51 = ByteBuffer.wrap(var1)
        var2.position(var1.size - 23)
        Intrinsics.checkExpressionValueIsNotNull(var2, "var2")
        val verticeCount = var2.readUnsignedShort()
        val triangleCount = var2.readUnsignedShort()
        val textureTriangleCount = var2.readUnsignedByte()
        val var13 = var2.readUnsignedByte()
        val modelPriority = var2.readUnsignedByte()
        val var50 = var2.readUnsignedByte()
        val var17 = var2.readUnsignedByte()
        val modelTexture = var2.readUnsignedByte()
        val modelVertexSkins = var2.readUnsignedByte()
        val var20 = var2.readUnsignedShort()
        val var21 = var2.readUnsignedShort()
        val var42 = var2.readUnsignedShort()
        val var22 = var2.readUnsignedShort()
        val var38 = var2.readUnsignedShort()
        var textureAmount = 0
        var var7 = 0
        var var29 = 0
        var renderTypePos: Int
        var position: Int
        if (textureTriangleCount > 0) {
            model.textureRenderTypes = ByteArray(textureTriangleCount)
            var2.position(0)
            for (position in 0 until textureTriangleCount) {
                model.textureRenderTypes!![position] = var2.get()
                renderTypePos = model.textureRenderTypes!![position].toInt()
                if (renderTypePos == 0) {
                    ++textureAmount
                }
                if (renderTypePos >= 1 && renderTypePos <= 3) {
                    ++var7
                }
                if (renderTypePos == 2) {
                    ++var29
                }
            }
        }
        position = textureTriangleCount + verticeCount
        renderTypePos = position
        if (var13 == 1) {
            position += triangleCount
        }
        val var49 = position
        position += triangleCount
        val priorityPos = position
        if (modelPriority == 255) {
            position += triangleCount
        }
        val triangleSkinPos = position
        if (var17 == 1) {
            position += triangleCount
        }
        val var35 = position
        if (modelVertexSkins == 1) {
            position += verticeCount
        }
        val alphaPos = position
        if (var50 == 1) {
            position += triangleCount
        }
        val var11 = position
        position += var22
        val texturePos = position
        if (modelTexture == 1) {
            position += triangleCount * 2
        }
        val textureCoordPos = position
        position += var38
        val colorPos = position
        position += triangleCount * 2
        val var40 = position
        position += var20
        val var41 = position
        position += var21
        val var8 = position
        position += var42
        val var43 = position
        position += textureAmount * 6
        val var37 = position
        position += var7 * 6
        val var48 = position
        position += var7 * 6
        val var56 = position
        position += var7 * 2
        val var45 = position
        position += var7
        val var46 = position
        position += var7 * 2 + var29 * 2
        model.vertexCount = verticeCount
        model.faceCount = triangleCount
        model.textureTriangleCount = textureTriangleCount
        model.vertexPositionsX = IntArray(verticeCount)
        model.vertexPositionsY = IntArray(verticeCount)
        model.vertexPositionsZ = IntArray(verticeCount)
        model.faceVertexIndices1 = IntArray(triangleCount)
        model.faceVertexIndices2 = IntArray(triangleCount)
        model.faceVertexIndices3 = IntArray(triangleCount)
        if (modelVertexSkins == 1) {
            model.vertexSkins = IntArray(verticeCount)
        }
        if (var13 == 1) {
            model.faceRenderTypes = ByteArray(triangleCount)
        }
        if (modelPriority == 255) {
            model.faceRenderPriorities = ByteArray(triangleCount)
        } else {
            model.priority = modelPriority.toByte()
        }
        if (var50 == 1) {
            model.faceAlphas = ByteArray(triangleCount)
        }
        if (var17 == 1) {
            model.faceSkins = IntArray(triangleCount)
        }
        if (modelTexture == 1) {
            model.faceTextures = ShortArray(triangleCount)
        }
        if (modelTexture == 1 && textureTriangleCount > 0) {
            model.textureCoordinates = ByteArray(triangleCount)
        }
        model.faceColors = ShortArray(triangleCount)
        if (textureTriangleCount > 0) {
            model.textureTriangleVertexIndices1 =
                ShortArray(textureTriangleCount)
            model.textureTriangleVertexIndices2 =
                ShortArray(textureTriangleCount)
            model.textureTriangleVertexIndices3 =
                ShortArray(textureTriangleCount)
            if (var7 > 0) {
                model.aShortArray2574 = ShortArray(var7)
                model.aShortArray2575 = ShortArray(var7)
                model.aShortArray2586 = ShortArray(var7)
                model.aShortArray2577 = ShortArray(var7)
                model.aByteArray2580 = ByteArray(var7)
                model.aShortArray2578 = ShortArray(var7)
            }
            if (var29 > 0) {
                model.texturePrimaryColors = ShortArray(var29)
            }
        }
        var2.position(textureTriangleCount)
        var24.position(var40)
        var3.position(var41)
        var28.position(var8)
        var6.position(var35)
        var vX = 0
        var vY = 0
        var vZ = 0
        var vertexZOffset: Int
        var vertexYOffset: Int
        var trianglePointX: Int
        var trianglePointY: Int
        var var66: IntArray?
        for (point in 0 until verticeCount) {
            trianglePointX = var2.readUnsignedByte()
            trianglePointY = 0
            if (trianglePointX and 1 != 0) {
                Intrinsics.checkExpressionValueIsNotNull(var24, "var24")
                trianglePointY = var24.readShortSmart()
            }
            vertexYOffset = 0
            if (trianglePointX and 2 != 0) {
                Intrinsics.checkExpressionValueIsNotNull(var3, "var3")
                vertexYOffset = var3.readShortSmart()
            }
            vertexZOffset = 0
            if (trianglePointX and 4 != 0) {
                Intrinsics.checkExpressionValueIsNotNull(var28, "var28")
                vertexZOffset = var28.readShortSmart()
            }
            model.vertexPositionsX[point] = vX + trianglePointY
            model.vertexPositionsY[point] = vY + vertexYOffset
            model.vertexPositionsZ[point] = vZ + vertexZOffset
            vX = model.vertexPositionsX[point]
            vY = model.vertexPositionsY[point]
            vZ = model.vertexPositionsZ[point]
            if (modelVertexSkins == 1) {
                var66 = model.vertexSkins
                if (var66 != null) {
                    Intrinsics.checkExpressionValueIsNotNull(var6, "var6")
                    var66[point] = var6.readUnsignedByte()
                }
            }
        }
        var2.position(colorPos)
        var24.position(renderTypePos)
        var3.position(priorityPos)
        var28.position(alphaPos)
        var6.position(triangleSkinPos)
        var55.position(texturePos)
        var51.position(textureCoordPos)
        for (point in 0 until triangleCount) {
            model.faceColors!![point] = var2.readUnsignedShort().toShort()
            if (var13 == 1) {
                model.faceRenderTypes?.set(point, var24.get())
            }
            if (modelPriority == 255) {
                model.faceRenderPriorities?.set(point, var3.get())
            }
            if (var50 == 1) {
                model.faceAlphas!![point] = var28.get()
            }
            if (var17 == 1) {
                model.faceSkins!![point] = var6.readUnsignedByte()
            }
            if (modelTexture == 1) {
                model.faceTextures?.set(point, (var55.readUnsignedShort() - 1).toShort())
            }
            if (model.textureCoordinates != null) {
                if (model.faceTextures!![point].toInt() != -1) {
                    model.textureCoordinates!![point] =
                        (var51.readUnsignedByte() - 1).toByte()
                }
            }
        }
        var2.position(var11)
        var24.position(var49)
        trianglePointX = 0
        trianglePointY = 0
        var trianglePointZ = 0
        vertexYOffset = 0
        var texIndex: Int
        var var57: Int
        vertexZOffset = 0
        while (vertexZOffset < triangleCount) {
            texIndex = var24.readUnsignedByte()
            if (texIndex == 1) {
                trianglePointX = var2.readShortSmart() + vertexYOffset
                trianglePointY = var2.readShortSmart() + trianglePointX
                trianglePointZ = var2.readShortSmart() + trianglePointY
                vertexYOffset = trianglePointZ
                model.faceVertexIndices1!![vertexZOffset] = trianglePointX
                model.faceVertexIndices2!![vertexZOffset] = trianglePointY
                model.faceVertexIndices3!![vertexZOffset] = trianglePointZ
            }
            if (texIndex == 2) {
                trianglePointY = trianglePointZ
                trianglePointZ = var2.readShortSmart() + vertexYOffset
                vertexYOffset = trianglePointZ
                model.faceVertexIndices1!![vertexZOffset] = trianglePointX
                model.faceVertexIndices2!![vertexZOffset] = trianglePointY
                model.faceVertexIndices3!![vertexZOffset] = trianglePointZ
            }
            if (texIndex == 3) {
                trianglePointX = trianglePointZ
                trianglePointZ = var2.readShortSmart() + vertexYOffset
                vertexYOffset = trianglePointZ
                model.faceVertexIndices1!![vertexZOffset] = trianglePointX
                model.faceVertexIndices2!![vertexZOffset] = trianglePointY
                model.faceVertexIndices3!![vertexZOffset] = trianglePointZ
            }
            if (texIndex == 4) {
                var57 = trianglePointX
                trianglePointX = trianglePointY
                trianglePointY = var57
                trianglePointZ = var2.readShortSmart() + vertexYOffset
                vertexYOffset = trianglePointZ
                model.faceVertexIndices1!![vertexZOffset] = trianglePointX
                model.faceVertexIndices2!![vertexZOffset] = var57
                model.faceVertexIndices3!![vertexZOffset] = trianglePointZ
            }
            ++vertexZOffset
        }
        var2.position(var43)
        var24.position(var37)
        var3.position(var48)
        var28.position(var56)
        var6.position(var45)
        var55.position(var46)
        texIndex = 0
        var57 = textureTriangleCount
        while (texIndex < var57) {
            val type = model.textureRenderTypes!![texIndex].toInt() and 255
            if (type == 0) {
                model.textureTriangleVertexIndices1!![texIndex] = var2.readUnsignedShort().toShort()
                model.textureTriangleVertexIndices2!![texIndex] = var2.readUnsignedShort().toShort()
                model.textureTriangleVertexIndices3!![texIndex] = var2.readUnsignedShort().toShort()
            }
            if (type == 1) {
                model.textureTriangleVertexIndices1!![texIndex] = var24.readUnsignedShort().toShort()
                model.textureTriangleVertexIndices2!![texIndex] = var24.readUnsignedShort().toShort()
                model.textureTriangleVertexIndices3!![texIndex] = var24.readUnsignedShort().toShort()
                model.aShortArray2574[texIndex] = var3.readUnsignedShort().toShort()
                model.aShortArray2575[texIndex] = var3.readUnsignedShort().toShort()
                model.aShortArray2586[texIndex] = var3.readUnsignedShort().toShort()
                model.aShortArray2577[texIndex] = var28.readUnsignedShort().toShort()
                model.aByteArray2580[texIndex] = var6.get()
                model.aShortArray2578[texIndex] = var55.readUnsignedShort().toShort()
            }
            if (type == 2) {
                model.textureTriangleVertexIndices1!![texIndex] = var24.readUnsignedShort().toShort()
                model.textureTriangleVertexIndices2!![texIndex] = var24.readUnsignedShort().toShort()
                model.textureTriangleVertexIndices3!![texIndex] = var24.readUnsignedShort().toShort()
                model.aShortArray2574[texIndex] = var3.readUnsignedShort().toShort()
                model.aShortArray2575[texIndex] = var3.readUnsignedShort().toShort()
                model.aShortArray2586[texIndex] = var3.readUnsignedShort().toShort()
                model.aShortArray2577[texIndex] = var28.readUnsignedShort().toShort()
                model.aByteArray2580[texIndex] = var6.get()
                model.aShortArray2578[texIndex] = var55.readUnsignedShort().toShort()
                model.texturePrimaryColors!![texIndex] = var55.readUnsignedShort().toShort()
            }
            if (type == 3) {
                model.textureTriangleVertexIndices1!![texIndex] = var24.readUnsignedShort().toShort()
                model.textureTriangleVertexIndices2!![texIndex] = var24.readUnsignedShort().toShort()
                model.textureTriangleVertexIndices3!![texIndex] = var24.readUnsignedShort().toShort()
                model.aShortArray2574[texIndex] = var3.readUnsignedShort().toShort()
                model.aShortArray2575[texIndex] = var3.readUnsignedShort().toShort()
                model.aShortArray2586[texIndex] = var3.readUnsignedShort().toShort()
                model.aShortArray2577[texIndex] = var28.readUnsignedShort().toShort()
                model.aByteArray2580[texIndex] = var6.get()
                model.aShortArray2578[texIndex] = var55.readUnsignedShort().toShort()
            }
            ++texIndex
        }
        var2.position(position)
        vertexZOffset = var2.readUnsignedByte()
        if (vertexZOffset != 0) {
            var2.readUnsignedShort()
            var2.readUnsignedShort()
            var2.readUnsignedShort()
            var2.int
        }
    }

    private fun loadOldFormat(model: ModelDefinition, var1: ByteArray?) {
        var var2 = false
        var var43 = false
        val var5 = ByteBuffer.wrap(var1)
        val var39 = ByteBuffer.wrap(var1)
        val var26 = ByteBuffer.wrap(var1)
        val var9 = ByteBuffer.wrap(var1)
        val var3 = ByteBuffer.wrap(var1)
        var5.position(var1!!.size - 18)
        val vertexCount = var5.readUnsignedShort()
        val faceCount = var5.readUnsignedShort()
        val textureCount = var5.readUnsignedByte()
        val isTextured = var5.readUnsignedByte()
        val faceRenderPriority = var5.readUnsignedByte()
        val hasFaceTransparencies = var5.readUnsignedByte()
        val hasPackedTransparencyVertexGroups = var5.readUnsignedByte()
        val hasPackedVertexGroups = var5.readUnsignedByte()
        val vertexXDataByteCount = var5.readUnsignedShort()
        val vertexYDataByteCount = var5.readUnsignedShort()
        var5.readUnsignedShort()
        val faceIndexDataByteCount = var5.readUnsignedShort()
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
        model.vertexCount = vertexCount
        model.faceCount = faceCount
        model.textureTriangleCount = textureCount
        model.vertexPositionsX = IntArray(vertexCount)
        model.vertexPositionsY = IntArray(vertexCount)
        model.vertexPositionsZ = IntArray(vertexCount)
        model.faceVertexIndices1 = IntArray(faceCount)
        model.faceVertexIndices2 = IntArray(faceCount)
        model.faceVertexIndices3 = IntArray(faceCount)
        if (textureCount > 0) {
            model.textureRenderTypes = ByteArray(textureCount)
            model.textureTriangleVertexIndices1 = ShortArray(textureCount)
            model.textureTriangleVertexIndices2 = ShortArray(textureCount)
            model.textureTriangleVertexIndices3 = ShortArray(textureCount)
        }
        if (hasPackedVertexGroups == 1) {
            model.vertexSkins = IntArray(vertexCount)
        }
        if (isTextured == 1) {
            model.faceRenderTypes = ByteArray(faceCount)
            model.textureCoordinates = ByteArray(faceCount)
            model.faceTextures = ShortArray(faceCount)
        }
        if (faceRenderPriority == 255) {
            model.faceRenderPriorities = ByteArray(faceCount)
        } else {
            model.priority = faceRenderPriority.toByte()
        }
        if (hasFaceTransparencies == 1) {
            model.faceAlphas = ByteArray(faceCount)
        }
        if (hasPackedTransparencyVertexGroups == 1) {
            model.faceSkins = IntArray(faceCount)
        }
        model.faceColors = ShortArray(faceCount)
        var5.position(offsetOfVertexFlags.toInt())
        var39.position(offsetOfVertexXData)
        var26.position(offsetOfVertexYData)
        var9.position(dataOffset)
        var3.position(offsetOfPackedVertexGroups)
        var var41 = 0
        var var33 = 0
        var var19 = 0
        for (var18 in 0 until vertexCount) {
            val var8 = var5.readUnsignedByte()
            var var31 = 0
            if (var8 and 1 != 0) {
                var31 = var39.readShortSmart()
            }
            var var6 = 0
            if (var8 and 2 != 0) {
                var6 = var26.readShortSmart()
            }
            var var7 = 0
            if (var8 and 4 != 0) {
                var7 = var9.readShortSmart()
            }
            model.vertexPositionsX[var18] = var41 + var31
            model.vertexPositionsY[var18] = var33 + var6
            model.vertexPositionsZ[var18] = var19 + var7
            var41 = model.vertexPositionsX[var18]
            var33 = model.vertexPositionsY[var18]
            var19 = model.vertexPositionsZ[var18]
            if (hasPackedVertexGroups == 1) {
                model.vertexSkins!![var18] = var3.readUnsignedByte()
            }
        }
        var5.position(offsetOfFaceColorsOrFaceTextures)
        var39.position(offsetOfFaceTextureFlags)
        var26.position(offsetOfFaceRenderPriorities)
        var9.position(offsetOfFaceTransparencies)
        var3.position(offsetOfPackedTransparencyVertexGroups)
        for (var18 in 0 until faceCount) {
            model.faceColors!![var18] = var5.readUnsignedShort().toShort()
            if (isTextured == 1) {
                val var8 = var39.readUnsignedByte()
                if (var8 and 1 == 1) {
                    model.faceRenderTypes?.set(var18, 1)
                    var2 = true
                } else {
                    model.faceRenderTypes?.set(var18, 0)
                }
                if (var8 and 2 == 2) {
                    model.textureCoordinates?.set(var18, (var8 shr 2).toByte())
                    model.faceTextures?.set(var18, model.faceColors!![var18])
                    model.faceColors!![var18] = 127
                    if (model.faceTextures!![var18].toInt() != -1) {
                        var43 = true
                    }
                } else {
                    model.textureCoordinates?.set(var18, -1)
                    model.faceTextures?.set(var18, -1)
                }
            }
            if (faceRenderPriority == 255) {
                model.faceRenderPriorities?.set(var18, var26.get())
            }
            if (hasFaceTransparencies == 1) {
                model.faceAlphas!![var18] = var9.get()
            }
            if (hasPackedTransparencyVertexGroups == 1) {
                model.faceSkins!![var18] = var3.readUnsignedByte()
            }
        }
        var5.position(offsetOfFaceIndexData)
        var39.position(offsetOfFaceIndexCompressionTypes)
        var var18 = 0
        var var8 = 0
        var var31 = 0
        var var6 = 0
        var var21: Int
        var var22: Int
        for (var7 in 0 until faceCount) {
            var22 = var39.readUnsignedByte()
            if (var22 == 1) {
                var18 = var5.readShortSmart() + var6
                var8 = var5.readShortSmart() + var18
                var31 = var5.readShortSmart() + var8
                var6 = var31
                model.faceVertexIndices1!![var7] = var18
                model.faceVertexIndices2!![var7] = var8
                model.faceVertexIndices3!![var7] = var31
            }
            if (var22 == 2) {
                var8 = var31
                var31 = var5.readShortSmart() + var6
                var6 = var31
                model.faceVertexIndices1!![var7] = var18
                model.faceVertexIndices2!![var7] = var8
                model.faceVertexIndices3!![var7] = var31
            }
            if (var22 == 3) {
                var18 = var31
                var31 = var5.readShortSmart() + var6
                var6 = var31
                model.faceVertexIndices1!![var7] = var18
                model.faceVertexIndices2!![var7] = var8
                model.faceVertexIndices3!![var7] = var31
            }
            if (var22 == 4) {
                var21 = var18
                var18 = var8
                var8 = var21
                var31 = var5.readShortSmart() + var6
                var6 = var31
                model.faceVertexIndices1!![var7] = var18
                model.faceVertexIndices2!![var7] = var21
                model.faceVertexIndices3!![var7] = var31
            }
        }
        var5.position(offsetOfTextureIndices)
        for (var7 in 0 until textureCount) {
            model.textureRenderTypes?.set(var7, 0)
            model.textureTriangleVertexIndices1!![var7] = var5.readUnsignedShort().toShort()
            model.textureTriangleVertexIndices2!![var7] = var5.readUnsignedShort().toShort()
            model.textureTriangleVertexIndices3!![var7] = var5.readUnsignedShort().toShort()
        }
        if (model.textureCoordinates != null) {
            var var45 = false
            for (var22 in 0 until faceCount) {
                var21 = model.textureCoordinates!![var22].toInt() and 255
                if (var21 != 255) {
                    if (model.textureTriangleVertexIndices1!![var21].toInt() and 0xffff == model.faceVertexIndices1!![var22]) {
                        if (model.textureTriangleVertexIndices2!![var21].toInt() and 0xffff == model.faceVertexIndices2!![var22]) {
                            if (model.textureTriangleVertexIndices3!![var21].toInt() and 0xffff == model.faceVertexIndices3!![var22]) {
                                model.textureCoordinates!![var22] = -1
                                continue
                            }
                        }
                    }
                    var45 = true
                }
            }
            if (!var45) {
                model.textureCoordinates = null
            }
        }
        if (!var43) {
            model.faceTextures = null
        }
        if (!var2) {
            model.faceRenderTypes = null
        }
    }

    fun decodeType3(def: ModelDefinition, var1: ByteArray) {
        val var2 = ByteBuffer.wrap(var1)
        val var3 = ByteBuffer.wrap(var1)
        val var4 = ByteBuffer.wrap(var1)
        val var5 = ByteBuffer.wrap(var1)
        val var6 = ByteBuffer.wrap(var1)
        val var7 = ByteBuffer.wrap(var1)
        val var8 = ByteBuffer.wrap(var1)
        var2.position(var1.size - 26)
        val var9 = var2.readUnsignedShort()
        val var10 = var2.readUnsignedShort()
        val var11 = var2.readUnsignedByte()
        val var12 = var2.readUnsignedByte()
        val var13 = var2.readUnsignedByte()
        val var14 = var2.readUnsignedByte()
        val var15 = var2.readUnsignedByte()
        val var16 = var2.readUnsignedByte()
        val var17 = var2.readUnsignedByte()
        val var18 = var2.readUnsignedByte()
        val var19 = var2.readUnsignedShort()
        val var20 = var2.readUnsignedShort()
        val var21 = var2.readUnsignedShort()
        val var22 = var2.readUnsignedShort()
        val var23 = var2.readUnsignedShort()
        val var24 = var2.readUnsignedShort()
        var var25 = 0
        var var26 = 0
        var var27 = 0
        var var58: Int
        var var28: Int
        if (var11 > 0) {
            def.textureRenderTypes = ByteArray(var11)
            var2.position(0)
            var28 = 0
            while (var28 < var11) {
                def.textureRenderTypes!![var28] = var2.get()
                var58 = def.textureRenderTypes!![var28].toInt()
                if (var58 == 0) {
                    ++var25
                }
                if (var58 >= 1 && var58 <= 3) {
                    ++var26
                }
                if (var58 == 2) {
                    ++var27
                }
                ++var28
            }
        }
        var28 = var11 + var9
        var58 = var28
        if (var12 == 1) {
            var28 += var10
        }
        val var30 = var28
        var28 += var10
        val var31 = var28
        if (var13 == 255) {
            var28 += var10
        }
        val var32 = var28
        if (var15 == 1) {
            var28 += var10
        }
        val var33 = var28
        var28 += var24
        val var34 = var28
        if (var14 == 1) {
            var28 += var10
        }
        val var35 = var28
        var28 += var22
        val var36 = var28
        if (var16 == 1) {
            var28 += var10 * 2
        }
        val var37 = var28
        var28 += var23
        val var38 = var28
        var28 += var10 * 2
        val var39 = var28
        var28 += var19
        val var40 = var28
        var28 += var20
        val var41 = var28
        var28 += var21
        val var42 = var28
        var28 += var25 * 6
        val var43 = var28
        var28 += var26 * 6
        val var44 = var28
        var28 += var26 * 6
        val var45 = var28
        var28 += var26 * 2
        val var46 = var28
        var28 += var26
        val var47 = var28
        var28 = var28 + var26 * 2 + var27 * 2
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
        var3.position(var39)
        var4.position(var40)
        var5.position(var41)
        var6.position(var33)
        var var48 = 0
        var var49 = 0
        var var50 = 0
        var var51: Int
        var var52: Int
        var var53: Int
        var var54: Int
        var var55: Int
        var var67: IntArray?
        var51 = 0
        while (var51 < var9) {
            var52 = var2.readUnsignedByte()
            var53 = 0
            if (var52 and 1 != 0) {
                var53 = var3.readShortSmart()
            }
            var54 = 0
            if (var52 and 2 != 0) {
                var54 = var4.readShortSmart()
            }
            var55 = 0
            if (var52 and 4 != 0) {
                var55 = var5.readShortSmart()
            }
            def.vertexPositionsX[var51] = var48 + var53
            def.vertexPositionsY[var51] = var49 + var54
            def.vertexPositionsZ[var51] = var50 + var55
            var48 = def.vertexPositionsX[var51]
            var49 = def.vertexPositionsY[var51]
            var50 = def.vertexPositionsZ[var51]
            if (var17 == 1) {
                var67 = def.vertexSkins
                if (var67 != null) {
                    var67[var51] = var6.readUnsignedByte()
                }
            }
            ++var51
        }
        if (var18 == 1) {
            var51 = 0
            while (var51 < var9) {
                var52 = var6.readUnsignedByte()
                var53 = 0
                while (var53 < var52) {
                    var6.readUnsignedByte()
                    var6.readUnsignedByte()
                    ++var53
                }
                ++var51
            }
        }
        var2.position(var38)
        var3.position(var58)
        var4.position(var31)
        var5.position(var34)
        var6.position(var32)
        var7.position(var36)
        var8.position(var37)
        var var69: ShortArray?
        var51 = 0
        while (var51 < var10) {
            var69 = def.faceColors
            var69!![var51] = var2.readUnsignedShort().toShort()
            if (var12 == 1) {
                def.faceRenderTypes!![var51] = var3.get()
            }
            if (var13 == 255) {
                def.faceRenderPriorities!![var51] = var4.get()
            }
            if (var14 == 1) {
                def.faceAlphas!![var51] = var5.get()
            }
            if (var15 == 1) {
                var67 = def.faceSkins
                var67!![var51] = var6.readUnsignedByte()
            }
            if (var16 == 1) {
                var69 = def.faceTextures
                var69!![var51] = (var7.readUnsignedShort() - 1).toShort()
            }
            if (def.textureCoordinates != null) {
                var69 = def.faceTextures
                if (var69!![var51].toInt() != -1) {
                    def.textureCoordinates!![var51] =
                        (var8.readUnsignedByte() - 1).toByte()
                }
            }
            ++var51
        }
        var2.position(var35)
        var3.position(var30)
        var51 = 0
        var52 = 0
        var53 = 0
        var54 = 0
        var var56: Int
        var55 = 0
        while (var55 < var10) {
            var56 = var3.readUnsignedByte()
            if (var56 == 1) {
                var51 = var2.readShortSmart() + var54
                var52 = var2.readShortSmart() + var51
                var53 = var2.readShortSmart() + var52
                var54 = var53
                def.faceVertexIndices1!![var55] = var51
                def.faceVertexIndices2!![var55] = var52
                def.faceVertexIndices3!![var55] = var53
            }
            if (var56 == 2) {
                var52 = var53
                var53 = var2.readShortSmart() + var54
                var54 = var53
                def.faceVertexIndices1!![var55] = var51
                def.faceVertexIndices2!![var55] = var52
                def.faceVertexIndices3!![var55] = var53
            }
            if (var56 == 3) {
                var51 = var53
                var53 = var2.readShortSmart() + var54
                var54 = var53
                def.faceVertexIndices1!![var55] = var51
                def.faceVertexIndices2!![var55] = var52
                def.faceVertexIndices3!![var55] = var53
            }
            if (var56 == 4) {
                val var57 = var51
                var51 = var52
                var52 = var57
                var53 = var2.readShortSmart() + var54
                var54 = var53
                def.faceVertexIndices1!![var55] = var51
                def.faceVertexIndices2!![var55] = var57
                def.faceVertexIndices3!![var55] = var53
            }
            ++var55
        }
        var2.position(var42)
        var3.position(var43)
        var4.position(var44)
        var5.position(var45)
        var6.position(var46)
        var7.position(var47)
        var55 = 0
        while (var55 < var11) {
            var56 = def.textureRenderTypes!![var55].toInt() and 255
            if (var56 == 0) {
                var69 = def.textureTriangleVertexIndices1
                var69!![var55] = var2.readUnsignedShort().toShort()
                var69 = def.textureTriangleVertexIndices2
                var69!![var55] = var2.readUnsignedShort().toShort()
                var69 = def.textureTriangleVertexIndices3
                var69!![var55] = var2.readUnsignedShort().toShort()
            }
            ++var55
        }
        var2.position(var28)
        var55 = var2.readUnsignedByte()
        if (var55 != 0) {
            var2.readUnsignedShort()
            var2.readUnsignedShort()
            var2.readUnsignedShort()
            var2.int
        }
    }

    fun decodeType2(def: ModelDefinition, var1: ByteArray) {
        var var2 = false
        var var3 = false
        val var4 = ByteBuffer.wrap(var1)
        val var5 = ByteBuffer.wrap(var1)
        val var6 = ByteBuffer.wrap(var1)
        val var7 = ByteBuffer.wrap(var1)
        val var8 = ByteBuffer.wrap(var1)
        var4.position(var1.size - 23)
        val var9 = var4.readUnsignedShort()
        val var10 = var4.readUnsignedShort()
        val var11 = var4.readUnsignedByte()
        val var12 = var4.readUnsignedByte()
        val var13 = var4.readUnsignedByte()
        val var14 = var4.readUnsignedByte()
        val var15 = var4.readUnsignedByte()
        val var16 = var4.readUnsignedByte()
        val var17 = var4.readUnsignedByte()
        val var18 = var4.readUnsignedShort()
        val var19 = var4.readUnsignedShort()
        val var20 = var4.readUnsignedShort()
        val var21 = var4.readUnsignedShort()
        val var22 = var4.readUnsignedShort()
        val var23: Byte = 0
        var var24 = var23 + var9
        val var25 = var24
        var24 += var10
        val var26 = var24
        if (var13 == 255) {
            var24 += var10
        }
        val var27 = var24
        if (var15 == 1) {
            var24 += var10
        }
        val var28 = var24
        if (var12 == 1) {
            var24 += var10
        }
        val var29 = var24
        var24 += var22
        val var30 = var24
        if (var14 == 1) {
            var24 += var10
        }
        val var31 = var24
        var24 += var21
        val var32 = var24
        var24 += var10 * 2
        val var33 = var24
        var24 += var11 * 6
        val var34 = var24
        var24 += var18
        val var35 = var24
        var24 += var19
        def.vertexCount = var9
        def.faceCount = var10
        def.textureTriangleCount = var11
        def.vertexPositionsX = IntArray(var9)
        def.vertexPositionsY = IntArray(var9)
        def.vertexPositionsZ = IntArray(var9)
        def.faceVertexIndices1 = IntArray(var10)
        def.faceVertexIndices2 = IntArray(var10)
        def.faceVertexIndices3 = IntArray(var10)
        if (var11 > 0) {
            def.textureRenderTypes = ByteArray(var11)
            def.textureTriangleVertexIndices1 = ShortArray(var11)
            def.textureTriangleVertexIndices2 = ShortArray(var11)
            def.textureTriangleVertexIndices3 = ShortArray(var11)
        }
        if (var16 == 1) {
            def.vertexSkins = IntArray(var9)
        }
        if (var12 == 1) {
            def.faceRenderTypes = ByteArray(var10)
            def.textureCoordinates = ByteArray(var10)
            def.faceTextures = ShortArray(var10)
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
        def.faceColors = ShortArray(var10)
        var4.position(var23.toInt())
        var5.position(var34)
        var6.position(var35)
        var7.position(var24)
        var8.position(var29)
        var var37 = 0
        var var38 = 0
        var var39 = 0
        var var40: Int
        var var41: Int
        var var42: Int
        var var43: Int
        var var44: Int
        var40 = 0
        while (var40 < var9) {
            var41 = var4.readUnsignedByte()
            var42 = 0
            if (var41 and 1 != 0) {
                var42 = var5.readShortSmart()
            }
            var43 = 0
            if (var41 and 2 != 0) {
                var43 = var6.readShortSmart()
            }
            var44 = 0
            if (var41 and 4 != 0) {
                var44 = var7.readShortSmart()
            }
            def.vertexPositionsX[var40] = var37 + var42
            def.vertexPositionsY[var40] = var38 + var43
            def.vertexPositionsZ[var40] = var39 + var44
            var37 = def.vertexPositionsX[var40]
            var38 = def.vertexPositionsY[var40]
            var39 = def.vertexPositionsZ[var40]
            if (var16 == 1) {
                def.vertexSkins!![var40] = var8.readUnsignedByte()
            }
            ++var40
        }
        if (var17 == 1) {
            var40 = 0
            while (var40 < var9) {
                var41 = var8.readUnsignedByte()
                var42 = 0
                while (var42 < var41) {
                    var8.readUnsignedByte()
                    var8.readUnsignedByte()
                    ++var42
                }
                ++var40
            }
        }
        var4.position(var32)
        var5.position(var28)
        var6.position(var26)
        var7.position(var30)
        var8.position(var27)
        var var58: ShortArray?
        var var59: ByteArray?
        var40 = 0
        while (var40 < var10) {
            var58 = def.faceColors
            var58!![var40] = var4.readUnsignedShort().toShort()
            if (var12 == 1) {
                var41 = var5.readUnsignedByte()
                if (var41 and 1 == 1) {
                    var59 = def.faceRenderTypes
                    var59!![var40] = 1
                    var2 = true
                } else {
                    var59 = def.faceRenderTypes
                    var59!![var40] = 0
                }
                if (var41 and 2 == 2) {
                    var59 = def.textureCoordinates
                    if (var59 != null) {
                        var59[var40] = (var41 shr 2).toByte()
                    }
                    var58 = def.faceTextures
                    var58!![var40] = def.faceColors!![var40]
                    var58 = def.faceColors
                    var58!![var40] = 127
                    var58 = def.faceTextures
                    if (var58!![var40].toInt() != -1) {
                        var3 = true
                    }
                } else {
                    var59 = def.textureCoordinates
                    if (var59 != null) {
                        var59[var40] = -1
                    }
                    var58 = def.faceTextures
                    var58!![var40] = -1
                }
            }
            if (var13 == 255) {
                var59 = def.faceRenderPriorities
                var59!![var40] = var6.get()
            }
            if (var14 == 1) {
                var59 = def.faceAlphas
                var59!![var40] = var7.get()
            }
            if (var15 == 1) {
                def.faceSkins!![var40] = var8.readUnsignedByte()
            }
            ++var40
        }
        var4.position(var31)
        var5.position(var25)
        var40 = 0
        var41 = 0
        var42 = 0
        var43 = 0
        var var45: Int
        var var46: Int
        var44 = 0
        while (var44 < var10) {
            var45 = var5.readUnsignedByte()
            if (var45 == 1) {
                var40 = var4.readShortSmart() + var43
                var41 = var4.readShortSmart() + var40
                var42 = var4.readShortSmart() + var41
                var43 = var42
                def.faceVertexIndices1!![var44] = var40
                def.faceVertexIndices2!![var44] = var41
                def.faceVertexIndices3!![var44] = var42
            }
            if (var45 == 2) {
                var41 = var42
                var42 = var4.readShortSmart() + var43
                var43 = var42
                def.faceVertexIndices1!![var44] = var40
                def.faceVertexIndices2!![var44] = var41
                def.faceVertexIndices3!![var44] = var42
            }
            if (var45 == 3) {
                var40 = var42
                var42 = var4.readShortSmart() + var43
                var43 = var42
                def.faceVertexIndices1!![var44] = var40
                def.faceVertexIndices2!![var44] = var41
                def.faceVertexIndices3!![var44] = var42
            }
            if (var45 == 4) {
                var46 = var40
                var40 = var41
                var41 = var46
                var42 = var4.readShortSmart() + var43
                var43 = var42
                def.faceVertexIndices1!![var44] = var40
                def.faceVertexIndices2!![var44] = var46
                def.faceVertexIndices3!![var44] = var42
            }
            ++var44
        }
        var4.position(var33)
        var44 = 0
        while (var44 < var11) {
            var59 = def.textureRenderTypes
            var59!![var44] = 0
            var58 = def.textureTriangleVertexIndices1
            var58!![var44] = var4.readUnsignedShort().toShort()
            var58 = def.textureTriangleVertexIndices2
            var58!![var44] = var4.readUnsignedShort().toShort()
            var58 = def.textureTriangleVertexIndices3
            var58!![var44] = var4.readUnsignedShort().toShort()
            ++var44
        }
        if (def.textureCoordinates != null) {
            var var47 = false
            var45 = 0
            while (var45 < var10) {
                var59 = def.textureCoordinates
                var46 = var59!![var45].toInt() and 255
                if (var46 != 255) {
                    var58 = def.textureTriangleVertexIndices1
                    var var61 = var58!![var46]
                    if (var61.toInt() == def.faceVertexIndices1!![var45] and 0xffff) {
                        var58 = def.textureTriangleVertexIndices2
                        var61 = var58!![var46]
                        if (var61.toInt() == def.faceVertexIndices2!![var45] and 0xffff) {
                            var58 = def.textureTriangleVertexIndices3
                            var61 = var58!![var46]
                            if (var61.toInt() == def.faceVertexIndices3!![var45] and 0xffff) {
                                var59 = def.textureCoordinates
                                var59!![var45] = -1
                                ++var45
                                continue
                            }
                        }
                    }
                    var47 = true
                }
                ++var45
            }
            if (!var47) {
                def.textureCoordinates = null
            }
        }
        if (!var3) {
            def.faceTextures = null
        }
        if (!var2) {
            def.faceRenderTypes = null
        }
    }
}
