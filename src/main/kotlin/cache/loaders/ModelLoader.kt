package cache.loaders

import cache.IndexType
import cache.definitions.ModelDefinition
import com.displee.cache.CacheLibrary
import com.google.inject.Inject
import cache.utils.readShortSmart
import cache.utils.readUnsignedByte
import cache.utils.readUnsignedShort
import java.nio.ByteBuffer

class ModelLoader @Inject constructor(
    private val cacheLibrary: CacheLibrary,
    private val modelDefinitionCache: HashMap<Int, ModelDefinition> = HashMap()
) {
    fun get(modelId: Int): ModelDefinition? {
        var modelDefinition = modelDefinitionCache[modelId]
        if (modelDefinition != null) {
            return modelDefinition
        }

        val index = cacheLibrary.index(IndexType.MODELS.id)
        val archive = index.archive(modelId and 0xFFFF) ?: return null
        val b = archive.files[0]?.data!!
        cacheLibrary.index(IndexType.MODELS.id).unCache() // free memory

        modelDefinition = ModelDefinition()
        modelDefinition.id = modelId
        if (b[b.size - 1].toInt() == -1 && b[b.size - 2].toInt() == -1) {
            load1(modelDefinition, b)
        } else {
            load2(modelDefinition, b)
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
        var position: Int
        if (textureTriangleCount > 0) {
            model.textureRenderTypes = ByteArray(textureTriangleCount)
            var2.position(0)
            position = 0
            while (position < textureTriangleCount) {
                model.textureRenderTypes!![position] = var2.get()
                val renderType: Byte = model.textureRenderTypes!![position]
                if (renderType.toInt() == 0) {
                    ++textureAmount
                }
                if (renderType >= 1 && renderType <= 3) {
                    ++var7
                }
                if (renderType.toInt() == 2) {
                    ++var29
                }
                ++position
            }
        }
        position = textureTriangleCount + verticeCount
        val renderTypePos = position
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
            model.textureTriangleVertexIndices1 = ShortArray(textureTriangleCount)
            model.textureTriangleVertexIndices2 = ShortArray(textureTriangleCount)
            model.textureTriangleVertexIndices3 = ShortArray(textureTriangleCount)
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
        var var10: Int
        var vertexYOffset: Int
        var var15: Int
        var point: Int
        point = 0
        while (point < verticeCount) {
            val vertexFlags: Int = var2.readUnsignedByte()
            var vertexXOffset = 0
            if (vertexFlags and 1 != 0) {
                vertexXOffset = var24.readShortSmart()
            }
            vertexYOffset = 0
            if (vertexFlags and 2 != 0) {
                vertexYOffset = var3.readShortSmart()
            }
            vertexZOffset = 0
            if (vertexFlags and 4 != 0) {
                vertexZOffset = var28.readShortSmart()
            }
            model.vertexPositionsX[point] = vX + vertexXOffset
            model.vertexPositionsY[point] = vY + vertexYOffset
            model.vertexPositionsZ[point] = vZ + vertexZOffset
            vX = model.vertexPositionsX[point]
            vY = model.vertexPositionsY[point]
            vZ = model.vertexPositionsZ[point]
            if (modelVertexSkins == 1) {
                model.vertexSkins?.set(point, var6.readUnsignedByte())
            }
            ++point
        }
        var2.position(colorPos)
        var24.position(renderTypePos)
        var3.position(priorityPos)
        var28.position(alphaPos)
        var6.position(triangleSkinPos)
        var55.position(texturePos)
        var51.position(textureCoordPos)
        point = 0
        while (point < triangleCount) {
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
                model.faceTextures?.set(point, ((var55.readUnsignedShort() - 1).toShort()))
            }
            if (model.textureCoordinates != null && model.faceTextures!![point].toInt() != -1) {
                model.textureCoordinates!![point] = ((var51.readUnsignedByte() - 1).toByte())
            }
            ++point
        }
        var2.position(var11)
        var24.position(var49)
        var trianglePointX = 0
        var trianglePointY = 0
        var trianglePointZ = 0
        vertexYOffset = 0
        var var16: Int
        vertexZOffset = 0
        while (vertexZOffset < triangleCount) {
            val numFaces: Int = var24.readUnsignedByte()
            if (numFaces == 1) {
                trianglePointX = var2.readShortSmart() + vertexYOffset
                trianglePointY = var2.readShortSmart() + trianglePointX
                trianglePointZ = var2.readShortSmart() + trianglePointY
                vertexYOffset = trianglePointZ
                model.faceVertexIndices1!![vertexZOffset] = trianglePointX
                model.faceVertexIndices2!![vertexZOffset] = trianglePointY
                model.faceVertexIndices3!![vertexZOffset] = trianglePointZ
            }
            if (numFaces == 2) {
                trianglePointY = trianglePointZ
                trianglePointZ = var2.readShortSmart() + vertexYOffset
                vertexYOffset = trianglePointZ
                model.faceVertexIndices1!![vertexZOffset] = trianglePointX
                model.faceVertexIndices2!![vertexZOffset] = trianglePointY
                model.faceVertexIndices3!![vertexZOffset] = trianglePointZ
            }
            if (numFaces == 3) {
                trianglePointX = trianglePointZ
                trianglePointZ = var2.readShortSmart() + vertexYOffset
                vertexYOffset = trianglePointZ
                model.faceVertexIndices1!![vertexZOffset] = trianglePointX
                model.faceVertexIndices2!![vertexZOffset] = trianglePointY
                model.faceVertexIndices3!![vertexZOffset] = trianglePointZ
            }
            if (numFaces == 4) {
                val var57 = trianglePointX
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
        for (texIndex in 0 until textureTriangleCount) {
            val type: Int = model.textureRenderTypes!![texIndex].toInt() and 255
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
        }
        var2.position(position)
        vertexZOffset = var2.readUnsignedByte()
        if (vertexZOffset != 0) {
            //new Class41();
            var2.readUnsignedShort().toShort()
            var2.readUnsignedShort().toShort()
            var2.readUnsignedShort().toShort()
            var2.int
        }
    }

    private fun load2(model: ModelDefinition, var1: ByteArray) {
        var var2 = false
        var var43 = false
        val var5 = ByteBuffer.wrap(var1)
        val var39 = ByteBuffer.wrap(var1)
        val var26 = ByteBuffer.wrap(var1)
        val var9 = ByteBuffer.wrap(var1)
        val var3 = ByteBuffer.wrap(var1)
        var5.position(var1.size - 18)
        val var10 = var5.readUnsignedShort()
        val var11 = var5.readUnsignedShort()
        val var12 = var5.readUnsignedByte()
        val var13 = var5.readUnsignedByte()
        val var14 = var5.readUnsignedByte()
        val var30 = var5.readUnsignedByte()
        val var15 = var5.readUnsignedByte()
        val var28 = var5.readUnsignedByte()
        val var27 = var5.readUnsignedShort()
        val var20 = var5.readUnsignedShort()
        val var36 = var5.readUnsignedShort()
        val var23 = var5.readUnsignedShort()
        val var16: Byte = 0
        var var46 = var16 + var10
        val var24 = var46
        var46 += var11
        val var25 = var46
        if (var14 == 255) {
            var46 += var11
        }
        val var4 = var46
        if (var15 == 1) {
            var46 += var11
        }
        val var42 = var46
        if (var13 == 1) {
            var46 += var11
        }
        val var37 = var46
        if (var28 == 1) {
            var46 += var10
        }
        val var29 = var46
        if (var30 == 1) {
            var46 += var11
        }
        val var44 = var46
        var46 += var23
        val var17 = var46
        var46 += var11 * 2
        val var32 = var46
        var46 += var12 * 6
        val var34 = var46
        var46 += var27
        val var35 = var46
        var46 += var20
        val var10000 = var46 + var36
        model.vertexCount = var10
        model.faceCount = var11
        model.textureTriangleCount = var12
        model.vertexPositionsX = IntArray(var10)
        model.vertexPositionsY = IntArray(var10)
        model.vertexPositionsZ = IntArray(var10)
        model.faceVertexIndices1 = IntArray(var11)
        model.faceVertexIndices2 = IntArray(var11)
        model.faceVertexIndices3 = IntArray(var11)
        if (var12 > 0) {
            model.textureRenderTypes = ByteArray(var12)
            model.textureTriangleVertexIndices1 = ShortArray(var12)
            model.textureTriangleVertexIndices2 = ShortArray(var12)
            model.textureTriangleVertexIndices3 = ShortArray(var12)
        }
        if (var28 == 1) {
            model.vertexSkins = IntArray(var10)
        }
        if (var13 == 1) {
            model.faceRenderTypes = ByteArray(var11)
            model.textureCoordinates = ByteArray(var11)
            model.faceTextures = ShortArray(var11)
        }
        if (var14 == 255) {
            model.faceRenderPriorities = ByteArray(var11)
        } else {
            model.priority = var14.toByte()
        }
        if (var30 == 1) {
            model.faceAlphas = ByteArray(var11)
        }
        if (var15 == 1) {
            model.faceSkins = IntArray(var11)
        }
        model.faceColors = ShortArray(var11)
        var5.position(var16.toInt())
        var39.position(var34)
        var26.position(var35)
        var9.position(var46)
        var3.position(var37)
        var var41 = 0
        var var33 = 0
        var var19 = 0
        var var6: Int
        var var7: Int
        var var8: Int
        var var18: Int
        var var31: Int
        var18 = 0
        while (var18 < var10) {
            var8 = var5.readUnsignedByte()
            var31 = 0
            if (var8 and 1 != 0) {
                var31 = var39.readShortSmart()
            }
            var6 = 0
            if (var8 and 2 != 0) {
                var6 = var26.readShortSmart()
            }
            var7 = 0
            if (var8 and 4 != 0) {
                var7 = var9.readShortSmart()
            }
            model.vertexPositionsX[var18] = var41 + var31
            model.vertexPositionsY[var18] = var33 + var6
            model.vertexPositionsZ[var18] = var19 + var7
            var41 = model.vertexPositionsX[var18]
            var33 = model.vertexPositionsY[var18]
            var19 = model.vertexPositionsZ[var18]
            if (var28 == 1) {
                model.vertexSkins!![var18] = var3.readUnsignedByte()
            }
            ++var18
        }
        var5.position(var17)
        var39.position(var42)
        var26.position(var25)
        var9.position(var29)
        var3.position(var4)
        var18 = 0
        while (var18 < var11) {
            model.faceColors!![var18] = var5.readUnsignedShort().toShort()
            if (var13 == 1) {
                var8 = var39.readUnsignedByte()
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
            if (var14 == 255) {
                model.faceRenderPriorities?.set(var18, var26.get())
            }
            if (var30 == 1) {
                model.faceAlphas!![var18] = var9.get()
            }
            if (var15 == 1) {
                model.faceSkins!![var18] = var3.readUnsignedByte()
            }
            ++var18
        }
        var5.position(var44)
        var39.position(var24)
        var18 = 0
        var8 = 0
        var31 = 0
        var6 = 0
        var var21: Int
        var var22: Int
        var7 = 0
        while (var7 < var11) {
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
            ++var7
        }
        var5.position(var32)
        var7 = 0
        while (var7 < var12) {
            model.textureRenderTypes?.set(var7, 0)
            model.textureTriangleVertexIndices1!![var7] = var5.readUnsignedShort().toShort()
            model.textureTriangleVertexIndices2!![var7] = var5.readUnsignedShort().toShort()
            model.textureTriangleVertexIndices3!![var7] = var5.readUnsignedShort().toShort()
            ++var7
        }
        if (model.textureCoordinates != null) {
            var var45 = false
            var22 = 0
            while (var22 < var11) {
                var21 = model.textureCoordinates!![var22].toInt() and 255
                if (var21 != 255) {
                    if (model.textureTriangleVertexIndices1!![var21].toInt() and '\uffff'.toInt() == model.faceVertexIndices1!![var22] && model.textureTriangleVertexIndices2!![var21].toInt() and '\uffff'.toInt() == model.faceVertexIndices2!![var22] && model.textureTriangleVertexIndices3!![var21].toInt() and '\uffff'.toInt() == model.faceVertexIndices3!![var22]
                    ) {
                        model.textureCoordinates!![var22] = -1
                    } else {
                        var45 = true
                    }
                }
                ++var22
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
}