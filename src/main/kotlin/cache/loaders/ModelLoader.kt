/*
Adapted from RuneLite source code, which provides this to us under the following
license:

BSD 2-Clause License

Copyright (c) 2016-2017, Adam <Adam@sigterm.info>
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package cache.loaders

import cache.IndexType
import cache.definitions.ModelDefinition
import cache.utils.readByteArray
import cache.utils.readShortSmart
import cache.utils.readUnsignedByte
import cache.utils.readUnsignedShort
import com.displee.cache.CacheLibrary
import java.io.IOException
import java.nio.ByteBuffer

class ModelLoader(private val cacheLibrary: CacheLibrary) : ThreadsafeLazyLoader<ModelDefinition>() {
    @Throws(IOException::class)
    override operator fun get(id: Int): ModelDefinition? {
        val def = super.get(id) ?: return null
        // Make a defensive copy because these get mutated a lot
        return ModelDefinition(def)
    }

    override fun load(id: Int): ModelDefinition? {
        val index = cacheLibrary.index(IndexType.MODELS.id)
        val archive = index.archive(id and 0xffff) ?: return null
        val stream = ByteBuffer.wrap(archive.files[0]!!.data)
        archive.restore() // Drop cached archive
        stream.position(stream.limit() - 2)
        val def = when (stream.short.toInt()) {
            -3 -> readModelCommon(id, stream, 26, true, true)
            -2 -> readModelCommon(id, stream, 23, false, true)
            -1 -> readModelCommon(id, stream, 23, true, false)
            else -> readModelCommon(id, stream, 18, false, false)
        }
        def.computeNormals()
        def.computeTextureUVCoordinates()
        def.computeAnimationTables()
        return def
    }

    private fun readModelCommon(
        modelId: Int,
        stream1: ByteBuffer,
        headerOffset: Int,
        isNewStyleTextures: Boolean,
        canHaveAnimayaGroups: Boolean
    ): ModelDefinition {
        stream1.position(stream1.limit() - headerOffset)
        val vertexCount = stream1.readUnsignedShort()
        val faceCount = stream1.readUnsignedShort()
        val textureCount = stream1.readUnsignedByte()
        val oldStyleIsTextured =
            if (!isNewStyleTextures) stream1.readUnsignedByte() else 0
        val hasFaceRenderTypes =
            if (isNewStyleTextures) stream1.readUnsignedByte() else 0
        val faceRenderPriority = stream1.readUnsignedByte()
        val hasFaceTransparencies = stream1.readUnsignedByte()
        val hasPackedTransparencyVertexGroups = stream1.readUnsignedByte()
        val hasFaceTextures =
            if (isNewStyleTextures) stream1.readUnsignedByte() else 0
        val hasVertexSkins = stream1.readUnsignedByte()
        val hasAnimayaGroups =
            if (canHaveAnimayaGroups) stream1.readUnsignedByte() else 0

        val priority =
            if (faceRenderPriority == 255) 0
            else faceRenderPriority.toByte()

        stream1.rewind()
        val textureRenderTypes: ByteArray =
            if (!isNewStyleTextures) ByteArray(textureCount)
            else stream1.readByteArray(textureCount)

        val vertexFlags = stream1.readByteArray(vertexCount)
        var faceRenderTypes: ByteArray? = null
        if (hasFaceRenderTypes == 1) {
            faceRenderTypes = stream1.readByteArray(faceCount)
        }
        val faceIndexCompressionTypes = stream1.readByteArray(faceCount)
        val faceRenderPriorities =
            if (faceRenderPriority == 255) stream1.readByteArray(faceCount)
            else null

        val faceSkins =
            if (hasPackedTransparencyVertexGroups == 1) readIntArrayOfUnsignedBytes(stream1, faceCount)
            else null

        var faceTextureFlags: ByteArray? = null
        if (oldStyleIsTextured == 1) {
            faceTextureFlags = stream1.readByteArray(faceCount)
        }
        val vertexSkins =
            if (hasVertexSkins == 1) readIntArrayOfUnsignedBytes(stream1, vertexCount)
            else null

        if (hasAnimayaGroups == 1 && isNewStyleTextures) {
            readAnimayaGroups(stream1, vertexCount)
        }

        val faceAlphas =
            if (hasFaceTransparencies == 1) stream1.readByteArray(faceCount)
            else null

        val faceVertexIndices = readFaceIndexData(stream1, faceIndexCompressionTypes)

        var faceTextures: ShortArray? = null
        var textureCoordinates: ByteArray? = null
        if (hasFaceTextures == 1) {
            faceTextures = readFaceTextures(stream1, faceCount)
            if (textureCount > 0) {
                textureCoordinates = readTextureCoordinates(
                    stream1,
                    faceCount,
                    faceTextures
                )
            }
        }

        val faceColors = readFaceColors(stream1, faceCount)

        if (faceTextureFlags != null) {
            val triple = processFaceTextureFlags(faceColors, faceTextureFlags, faceCount)
            faceTextures = triple.first
            faceRenderTypes = triple.second
            textureCoordinates = triple.third
        }

        val positions: Triple<IntArray, IntArray, IntArray>
        val textureTriangleVertexIndices: Triple<ShortArray, ShortArray, ShortArray>
        if (isNewStyleTextures) {
            positions = readVertexData(stream1, vertexFlags)
            textureTriangleVertexIndices = readTextureTriangleVertexIndices(textureRenderTypes, stream1, textureCount, false)
        } else {
            textureTriangleVertexIndices = readTextureTriangleVertexIndices(textureRenderTypes, stream1, textureCount, true)
            positions = readVertexData(stream1, vertexFlags)
        }

        if (hasAnimayaGroups == 1 && !isNewStyleTextures) {
            readAnimayaGroups(stream1, vertexCount)
        }

        textureCoordinates = discardUnusedTextures(
            textureCoordinates,
            faceVertexIndices.first,
            faceVertexIndices.second,
            faceVertexIndices.third,
            textureTriangleVertexIndices.first,
            textureTriangleVertexIndices.second,
            textureTriangleVertexIndices.third,
            faceCount,
            oldStyleIsTextured
        )

        return ModelDefinition(
            id = modelId,
            vertexCount = vertexCount,
            vertexPositionsX = positions.first,
            vertexPositionsY = positions.second,
            vertexPositionsZ = positions.third,
            faceCount = faceCount,
            faceVertexIndices1 = faceVertexIndices.first,
            faceVertexIndices2 = faceVertexIndices.second,
            faceVertexIndices3 = faceVertexIndices.third,
            faceAlphas = faceAlphas,
            faceColors = faceColors,
            faceRenderPriorities = faceRenderPriorities,
            faceRenderTypes = faceRenderTypes,
            textureTriangleCount = textureCount,
            textureTriangleVertexIndices1 = textureTriangleVertexIndices.first,
            textureTriangleVertexIndices2 = textureTriangleVertexIndices.second,
            textureTriangleVertexIndices3 = textureTriangleVertexIndices.third,
            faceTextures = faceTextures,
            textureCoordinates = textureCoordinates,
            textureRenderTypes = textureRenderTypes,
            vertexSkins = vertexSkins,
            faceSkins = faceSkins,
            priority = priority
        )
    }

    private fun processFaceTextureFlags(
        faceColors: ShortArray,
        faceTextureFlags: ByteArray,
        faceCount: Int
    ): Triple<ShortArray?, ByteArray?, ByteArray> {
        val faceTextures = ShortArray(faceCount)
        val faceRenderTypes = ByteArray(faceCount)
        val textureCoordinates = ByteArray(faceCount)
        var usesFaceRenderTypes = false
        var usesFaceTextures = false
        for (i in 0 until faceCount) {
            val faceTextureFlag = faceTextureFlags[i].toInt() and 0xFF
            if (faceTextureFlag and 1 == 1) {
                faceRenderTypes[i] = 1
                usesFaceRenderTypes = true
            } else {
                faceRenderTypes[i] = 0
            }
            if (faceTextureFlag and 2 == 2) {
                textureCoordinates[i] = (faceTextureFlag shr 2).toByte()
                faceTextures[i] = faceColors[i]
                faceColors[i] = 127
                if (faceTextures[i].toInt() != -1) {
                    usesFaceTextures = true
                }
            } else {
                textureCoordinates[i] = -1
                faceTextures[i] = -1
            }
        }
        return Triple(
            if (usesFaceTextures) faceTextures else null,
            if (usesFaceRenderTypes) faceRenderTypes else null,
            textureCoordinates,
        )
    }

    private fun discardUnusedTextures(
        textureCoordinates: ByteArray?,
        fvi1: IntArray,
        fvi2: IntArray,
        fvi3: IntArray,
        tvi1: ShortArray,
        tvi2: ShortArray,
        tvi3: ShortArray,
        faceCount: Int,
        isTextured: Int
    ): ByteArray? {
        if (isTextured != 1) return textureCoordinates
        textureCoordinates!!
        var usesTextureCoords = false
        for (i in 0 until faceCount) {
            val coord = textureCoordinates[i].toInt() and 255
            if (coord != 255) {
                if (fvi1[i] == tvi1[coord].toInt() and 0xffff && fvi2[i] == tvi2[coord].toInt() and 0xffff && fvi3[i] == tvi3[coord].toInt() and 0xffff) {
                    textureCoordinates[i] = -1
                } else {
                    usesTextureCoords = true
                }
            }
        }
        return if (usesTextureCoords) textureCoordinates else null
    }

    private fun readFaceIndexData(
        stream1: ByteBuffer,
        faceIndexCompressionTypes: ByteArray
    ): Triple<IntArray, IntArray, IntArray> {
        val faceCount = faceIndexCompressionTypes.size
        val faceVertexIndices1 = IntArray(faceCount)
        val faceVertexIndices2 = IntArray(faceCount)
        val faceVertexIndices3 = IntArray(faceCount)
        var previousIndex1 = 0
        var previousIndex2 = 0
        var previousIndex3 = 0
        for (i in 0 until faceCount) {
            when (faceIndexCompressionTypes[i].toInt()) {
                1 -> {
                    previousIndex1 = stream1.readShortSmart() + previousIndex3
                    previousIndex2 = stream1.readShortSmart() + previousIndex1
                    previousIndex3 = stream1.readShortSmart() + previousIndex2
                }
                2 -> {
                    previousIndex2 = previousIndex3
                    previousIndex3 = stream1.readShortSmart() + previousIndex3
                }
                3 -> {
                    previousIndex1 = previousIndex3
                    previousIndex3 = stream1.readShortSmart() + previousIndex3
                }
                4 -> {
                    val swap = previousIndex1
                    previousIndex1 = previousIndex2
                    previousIndex2 = swap
                    previousIndex3 = stream1.readShortSmart() + previousIndex3
                }
            }
            faceVertexIndices1[i] = previousIndex1
            faceVertexIndices2[i] = previousIndex2
            faceVertexIndices3[i] = previousIndex3
        }
        return Triple(faceVertexIndices1, faceVertexIndices2, faceVertexIndices3)
    }

    private fun readVertexData(
        stream: ByteBuffer,
        vertexFlags: ByteArray
    ): Triple<IntArray, IntArray, IntArray> {
        val vertexPositionsX = readVertexGroup(stream, vertexFlags, HAS_DELTA_X)
        val vertexPositionsY = readVertexGroup(stream, vertexFlags, HAS_DELTA_Y)
        val vertexPositionsZ = readVertexGroup(stream, vertexFlags, HAS_DELTA_Z)
        return Triple(vertexPositionsX, vertexPositionsY, vertexPositionsZ)
    }

    private fun readFaceColors(
        stream: ByteBuffer,
        faceCount: Int
    ): ShortArray {
        val faceColors = ShortArray(faceCount)
        stream.asShortBuffer()[faceColors]
        stream.position(stream.position() + faceCount * 2)
        return faceColors
    }

    private fun readFaceTextures(
        stream: ByteBuffer,
        faceCount: Int
    ): ShortArray {
        val faceTextures = ShortArray(faceCount)
        stream.asShortBuffer()[faceTextures]
        stream.position(stream.position() + faceCount * 2)
        for (i in 0 until faceCount) {
            faceTextures[i]--
        }
        return faceTextures
    }

    private fun readIntArrayOfUnsignedBytes(
        stream: ByteBuffer,
        length: Int
    ): IntArray {
        val array = IntArray(length)
        for (i in 0 until length) {
            array[i] = stream.readUnsignedByte()
        }
        return array
    }

    private fun readVertexGroup(
        stream: ByteBuffer,
        vertexFlags: ByteArray,
        deltaMask: Byte
    ): IntArray {
        var position = 0
        val vertexCount = vertexFlags.size
        val vertices = IntArray(vertexCount)
        for (i in 0 until vertexCount) {
            if (vertexFlags[i].toInt() and deltaMask.toInt() != 0) {
                position += stream.readShortSmart()
            }
            vertices[i] = position
        }
        return vertices
    }

    private fun readTextureCoordinates(
        stream: ByteBuffer,
        faceCount: Int,
        faceTextures: ShortArray
    ): ByteArray {
        val textureCoordinates = ByteArray(faceCount)
        for (i in 0 until faceCount) {
            if (faceTextures[i].toInt() != -1) {
                textureCoordinates[i] = (stream.readUnsignedByte() - 1).toByte()
            }
        }
        return textureCoordinates
    }

    private fun readAnimayaGroups(stream: ByteBuffer, vertexCount: Int) {
        /*
        val animayaGroups = arrayOfNulls<IntArray>(vertexCount)
        val animayaScales = arrayOfNulls<IntArray>(vertexCount)

        for (i in 0 until vertexCount) {
            val animayaLength = stream.readUnsignedByte()
            val thisAnimayaGroup = IntArray(animayaLength)
            val thisAnimayaScale = IntArray(animayaLength)

            for (j in 0 until animayaLength) {
                thisAnimayaGroup[j] = stream.readUnsignedByte()
                thisAnimayaScale[j] = stream.readUnsignedByte()
            }

            animayaGroups[i] = thisAnimayaGroup
            animayaScales[i] = thisAnimayaScale
        }

        def.animayaGroups = animayaGroups
        def.animayaScales = animayaScales
         */

        // Dummy implementation
        for (i in 0 until vertexCount) {
            val animayaLength = stream.readUnsignedByte()
            stream.position(stream.position() + 2 * animayaLength)
        }
    }

    private fun readTextureTriangleVertexIndices(
        textureRenderTypes: ByteArray,
        stream: ByteBuffer,
        textureCount: Int,
        always: Boolean
    ): Triple<ShortArray, ShortArray, ShortArray> {
        val textureTriangleVertexIndices1 = ShortArray(textureCount)
        val textureTriangleVertexIndices2 = ShortArray(textureCount)
        val textureTriangleVertexIndices3 = ShortArray(textureCount)
        for (i in 0 until textureCount) {
            if (always || textureRenderTypes[i].toInt() and 255 == 0) {
                textureTriangleVertexIndices1[i] = stream.short
                textureTriangleVertexIndices2[i] = stream.short
                textureTriangleVertexIndices3[i] = stream.short
            }
        }
        return Triple(textureTriangleVertexIndices1, textureTriangleVertexIndices2, textureTriangleVertexIndices3)
    }

    companion object {
        private const val HAS_DELTA_X: Byte = 1
        private const val HAS_DELTA_Y: Byte = 2
        private const val HAS_DELTA_Z: Byte = 4
    }
}
