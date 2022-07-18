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
import cache.utils.readShortSmart
import cache.utils.readUnsignedByte
import cache.utils.readUnsignedShort
import com.displee.cache.CacheLibrary
import java.io.IOException
import java.nio.ByteBuffer

class ModelLoader(private val cacheLibrary: CacheLibrary) {
    private val modelDefinitionCache: MutableMap<Int, ModelDefinition> =
        HashMap()

    @Throws(IOException::class)
    operator fun get(modelId: Int): ModelDefinition? {
        var def = modelDefinitionCache[modelId]
        if (def == null) {
            val index = cacheLibrary.index(IndexType.MODELS.id)
            val archive = index.archive(modelId and 0xffff) ?: return null
            val stream = ByteBuffer.wrap(archive.files[0]!!.data)
            archive.restore() // Drop cached archive
            def = ModelDefinition()
            def.id = modelId
            stream.position(stream.limit() - 2)
            when (stream.short.toInt()) {
                -3 -> readModelCommon(def, stream, 26, true, true)
                -2 -> readModelCommon(def, stream, 23, false, true)
                -1 -> readModelCommon(def, stream, 23, true, false)
                else -> readModelCommon(def, stream, 18, false, false)
            }
            def.computeNormals()
            def.computeTextureUVCoordinates()
            def.computeAnimationTables()
            modelDefinitionCache[modelId] = def
        }
        return ModelDefinition(def)
    }

    private fun readModelCommon(
        def: ModelDefinition,
        stream1: ByteBuffer,
        headerOffset: Int,
        isNewStyleTextures: Boolean,
        canHaveAnimayaGroups: Boolean
    ) {
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
        def.vertexCount = vertexCount
        def.faceCount = faceCount
        def.textureTriangleCount = textureCount
        if (faceRenderPriority != 255) {
            def.priority = faceRenderPriority.toByte()
        }
        stream1.rewind()
        if (textureCount > 0) {
            if (!isNewStyleTextures) {
                def.textureRenderTypes = ByteArray(textureCount)
            } else {
                def.textureRenderTypes = readByteArray(stream1, textureCount)
            }
        }
        val vertexFlags = readByteArray(stream1, vertexCount)
        if (hasFaceRenderTypes == 1) {
            def.faceRenderTypes = readByteArray(stream1, faceCount)
        }
        val faceIndexCompressionTypes = readByteArray(stream1, faceCount)
        if (faceRenderPriority == 255) {
            def.faceRenderPriorities = readByteArray(stream1, faceCount)
        }
        if (hasPackedTransparencyVertexGroups == 1) {
            readFaceSkins(def, stream1, faceCount)
        }
        var faceTextureFlags: ByteArray? = null
        if (oldStyleIsTextured == 1) {
            faceTextureFlags = readByteArray(stream1, faceCount)
        }
        if (hasVertexSkins == 1) {
            readVertexSkins(def, stream1, vertexCount)
        }
        if (hasAnimayaGroups == 1 && isNewStyleTextures) {
            readAnimayaGroups(stream1, vertexCount)
        }
        if (hasFaceTransparencies == 1) {
            def.faceAlphas = readByteArray(stream1, faceCount)
        }
        readFaceIndexData(def, stream1, faceIndexCompressionTypes)
        if (hasFaceTextures == 1) {
            readFaceTextures(def, stream1, faceCount)
            if (textureCount > 0) {
                readTextureCoordinates(
                    def,
                    stream1,
                    faceCount,
                    def.faceTextures!!
                )
            }
        }
        readFaceColors(def, stream1, faceCount)
        processFaceTextureFlags(
            def,
            faceTextureFlags,
            faceCount
        )
        if (isNewStyleTextures) {
            readVertexData(def, stream1, vertexFlags)
            readTextureTriangleVertexIndices(def, stream1, textureCount, false)
        } else {
            readTextureTriangleVertexIndices(def, stream1, textureCount, true)
            readVertexData(def, stream1, vertexFlags)
        }
        if (hasAnimayaGroups == 1 && !isNewStyleTextures) {
            readAnimayaGroups(stream1, vertexCount)
        }
        discardUnusedTextures(def, faceCount, oldStyleIsTextured)
    }

    private fun processFaceTextureFlags(
        def: ModelDefinition,
        faceTextureFlags: ByteArray?,
        faceCount: Int
    ) {
        if (faceTextureFlags == null) return
        val faceColors = def.faceColors!!
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
        if (usesFaceTextures) {
            def.faceTextures = faceTextures
        }
        if (usesFaceRenderTypes) {
            def.faceRenderTypes = faceRenderTypes
        }
        def.textureCoordinates = textureCoordinates
    }

    private fun discardUnusedTextures(
        def: ModelDefinition,
        faceCount: Int,
        isTextured: Int
    ) {
        if (isTextured != 1) return
        var usesTextureCoords = false
        val textureCoordinates = def.textureCoordinates!!
        val fvi1 = def.faceVertexIndices1!!
        val fvi2 = def.faceVertexIndices2!!
        val fvi3 = def.faceVertexIndices3!!
        val tvi1 = def.textureTriangleVertexIndices1!!
        val tvi2 = def.textureTriangleVertexIndices2!!
        val tvi3 = def.textureTriangleVertexIndices3!!
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
        if (!usesTextureCoords) {
            def.textureCoordinates = null
        }
    }

    private fun readFaceIndexData(
        def: ModelDefinition,
        stream1: ByteBuffer,
        faceIndexCompressionTypes: ByteArray
    ) {
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
        def.faceVertexIndices1 = faceVertexIndices1
        def.faceVertexIndices2 = faceVertexIndices2
        def.faceVertexIndices3 = faceVertexIndices3
    }

    private fun readVertexData(
        def: ModelDefinition,
        stream: ByteBuffer,
        vertexFlags: ByteArray
    ) {
        def.vertexPositionsX = readVertexGroup(stream, vertexFlags, HAS_DELTA_X)
        def.vertexPositionsY = readVertexGroup(stream, vertexFlags, HAS_DELTA_Y)
        def.vertexPositionsZ = readVertexGroup(stream, vertexFlags, HAS_DELTA_Z)
    }

    private fun readFaceColors(
        def: ModelDefinition,
        stream: ByteBuffer,
        faceCount: Int
    ) {
        val faceColors = ShortArray(faceCount)
        stream.asShortBuffer()[faceColors]
        stream.position(stream.position() + faceCount * 2)
        def.faceColors = faceColors
    }

    private fun readFaceTextures(
        def: ModelDefinition,
        stream: ByteBuffer,
        faceCount: Int
    ) {
        val faceTextures = ShortArray(faceCount)
        stream.asShortBuffer()[faceTextures]
        stream.position(stream.position() + faceCount * 2)
        for (i in 0 until faceCount) {
            faceTextures[i]--
        }
        def.faceTextures = faceTextures
    }

    private fun readVertexSkins(
        def: ModelDefinition,
        stream: ByteBuffer,
        vertexCount: Int
    ) {
        val vertexSkins = IntArray(vertexCount)
        for (i in 0 until vertexCount) {
            vertexSkins[i] = stream.readUnsignedByte()
        }
        def.vertexSkins = vertexSkins
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

    private fun readFaceSkins(
        def: ModelDefinition,
        stream: ByteBuffer,
        faceCount: Int
    ) {
        val faceSkins = IntArray(faceCount)
        for (i in 0 until faceCount) {
            faceSkins[i] = stream.readUnsignedByte()
        }
        def.faceSkins = faceSkins
    }

    private fun readTextureCoordinates(
        def: ModelDefinition,
        stream: ByteBuffer,
        faceCount: Int,
        faceTextures: ShortArray
    ) {
        val textureCoordinates = ByteArray(faceCount)
        for (i in 0 until faceCount) {
            if (faceTextures[i].toInt() != -1) {
                textureCoordinates[i] = (stream.readUnsignedByte() - 1).toByte()
            }
        }
        def.textureCoordinates = textureCoordinates
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
        def: ModelDefinition,
        stream: ByteBuffer,
        textureCount: Int,
        always: Boolean
    ) {
        val textureRenderTypes = def.textureRenderTypes
        val textureTriangleVertexIndices1 = ShortArray(textureCount)
        val textureTriangleVertexIndices2 = ShortArray(textureCount)
        val textureTriangleVertexIndices3 = ShortArray(textureCount)
        if (textureRenderTypes != null) {
            for (i in 0 until textureCount) {
                if (always || textureRenderTypes[i].toInt() and 255 == 0) {
                    textureTriangleVertexIndices1[i] = stream.short
                    textureTriangleVertexIndices2[i] = stream.short
                    textureTriangleVertexIndices3[i] = stream.short
                }
            }
        }
        def.textureTriangleVertexIndices1 = textureTriangleVertexIndices1
        def.textureTriangleVertexIndices2 = textureTriangleVertexIndices2
        def.textureTriangleVertexIndices3 = textureTriangleVertexIndices3
    }

    private fun readByteArray(stream: ByteBuffer, length: Int): ByteArray {
        val array = ByteArray(length)
        stream[array]
        return array
    }

    companion object {
        private const val HAS_DELTA_X: Byte = 1
        private const val HAS_DELTA_Y: Byte = 2
        private const val HAS_DELTA_Z: Byte = 4
    }
}
