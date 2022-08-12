package controllers.worldRenderer

import cache.definitions.RegionDefinition
import controllers.worldRenderer.entities.Entity
import controllers.worldRenderer.entities.Model
import controllers.worldRenderer.entities.StaticObject
import controllers.worldRenderer.entities.TileModel
import controllers.worldRenderer.entities.TilePaint
import controllers.worldRenderer.helpers.GpuFloatBuffer
import controllers.worldRenderer.helpers.GpuIntBuffer
import models.scene.REGION_SIZE
import models.scene.Scene
import models.scene.SceneTile

/*
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
class SceneUploader {
    var sceneId = (System.currentTimeMillis() / 1000L).toInt()
    private var offset = 0
    private var uvOffset = 0
    fun resetOffsets() {
        offset = 0
        uvOffset = 0
    }

    fun upload(scene: Scene, vertexbuffer: GpuIntBuffer, uvBuffer: GpuFloatBuffer) {
        ++sceneId
        resetOffsets()
        vertexbuffer.clear()
        uvBuffer.clear()
        for (rx in 0 until scene.cols) {
            for (ry in 0 until scene.rows) {
                val region = scene.getRegion(rx, ry) ?: continue

                for (z in 0 until RegionDefinition.Z) {
                    for (x in 0 until REGION_SIZE) {
                        for (y in 0 until REGION_SIZE) {
                            val tile = region.tiles[z][x][y]
                            tile?.let { reset(it) }
                        }
                    }
                }
            }
        }
        for (rx in 0 until scene.cols) {
            for (ry in 0 until scene.rows) {
                val region = scene.getRegion(rx, ry) ?: continue

                for (z in 0 until RegionDefinition.Z) {
                    for (x in 0 until REGION_SIZE) {
                        for (y in 0 until REGION_SIZE) {
                            val tile = region.tiles[z][x][y]
                            tile?.let { upload(it, vertexbuffer, uvBuffer) }
                        }
                    }
                }
            }
        }
    }

    private fun reset(tile: SceneTile) {
// 		Tile bridge = tile.getBridge();
// 		if (bridge != null)
// 		{
// 			reset(bridge);
// 		}
        tile.tilePaint?.computeObj?.offset = -1

        tile.tileModel?.computeObj?.offset = -1

        tile.wall?.entity?.model?.computeObj?.offset = -1
        tile.wall?.entity2?.model?.computeObj?.offset = -1

        tile.wallDecoration?.entity?.model?.computeObj?.offset = -1

        tile.floorDecoration?.entity?.model?.computeObj?.offset = -1

        for (gameObject in tile.gameObjects) {
            gameObject.entity.model.computeObj.offset = -1
        }
    }

    fun upload(tile: SceneTile, vertexBuffer: GpuIntBuffer, uvBuffer: GpuFloatBuffer) {
// 		Tile bridge = tile.getBridge();
// 		if (bridge != null)
// 		{
// 			upload(bridge, vertexBuffer, uvBuffer);
// 		}
        val sceneTilePaint = tile.tilePaint
        if (sceneTilePaint != null) {
            sceneTilePaint.computeObj.offset = offset
            if (sceneTilePaint.texture != -1) {
                sceneTilePaint.computeObj.uvOffset = uvOffset
            } else {
                sceneTilePaint.computeObj.uvOffset = -1
            }
            val len = upload(sceneTilePaint, vertexBuffer, uvBuffer)
            sceneTilePaint.computeObj.size = len / 3
            offset += len
            if (sceneTilePaint.texture != -1) {
                uvOffset += len
            }
        }

        val sceneTileModel = tile.tileModel
        if (sceneTileModel != null) {
            sceneTileModel.computeObj.offset = offset
            if (sceneTileModel.triangleTextureId != null) {
                sceneTileModel.computeObj.uvOffset = uvOffset
            } else {
                sceneTileModel.computeObj.uvOffset = -1
            }
            val len = upload(sceneTileModel, vertexBuffer, uvBuffer)
            sceneTileModel.computeObj.size = len / 3
            offset += len
            if (sceneTileModel.triangleTextureId != null) {
                uvOffset += len
            }
        }

        val wall = tile.wall
        if (wall != null) {
            val entity = wall.entity
            if (entity is StaticObject) {
                uploadModel(entity, vertexBuffer, uvBuffer)
            }
            val entity2 = wall.entity2
            if (entity2 is StaticObject) {
                uploadModel(entity2, vertexBuffer, uvBuffer)
            }
        }

        val wallDecoration = tile.wallDecoration
        if (wallDecoration != null) {
            val entity = wallDecoration.entity
            if (entity is StaticObject) {
                uploadModel(entity, vertexBuffer, uvBuffer)
            }
        }

        val floorDecoration = tile.floorDecoration
        if (floorDecoration != null) {
            val entity = floorDecoration.entity
            if (entity is StaticObject) {
                uploadModel(entity, vertexBuffer, uvBuffer)
            }
        }

        for (gameObject in tile.gameObjects) {
            val entity = gameObject.entity
            if (entity is StaticObject) {
                uploadModel(entity, vertexBuffer, uvBuffer)
            }
        }
    }

    fun upload(tile: TilePaint, vertexBuffer: GpuIntBuffer, uvBuffer: GpuFloatBuffer): Int {
        val swHeight = tile.swHeight
        val seHeight = tile.seHeight
        val neHeight = tile.neHeight
        val nwHeight = tile.nwHeight
        val neColor = tile.neColor
        val nwColor = tile.nwColor
        val seColor = tile.seColor
        val swColor = tile.swColor
        if (neColor == 12345678) {
            return 0
        }

        vertexBuffer.ensureCapacity(24)
        uvBuffer.ensureCapacity(24)

        // 0,0
        val vertexDx = 0
        val vertexDy = 0

        // 1,0
        val vertexCx = Constants.LOCAL_TILE_SIZE
        val vertexCy = 0

        // 1,1
        val vertexAx = Constants.LOCAL_TILE_SIZE
        val vertexAy = Constants.LOCAL_TILE_SIZE

        // 0,1
        val vertexBx = 0
        val vertexBy = Constants.LOCAL_TILE_SIZE

        vertexBuffer.put(vertexAx, neHeight, vertexAy, neColor)
        vertexBuffer.put(vertexBx, nwHeight, vertexBy, nwColor)
        vertexBuffer.put(vertexCx, seHeight, vertexCy, seColor)

        vertexBuffer.put(vertexDx, swHeight, vertexDy, swColor)
        vertexBuffer.put(vertexCx, seHeight, vertexCy, seColor)
        vertexBuffer.put(vertexBx, nwHeight, vertexBy, nwColor)

        if (tile.texture != -1) {
            val tex = tile.texture + 1f
            uvBuffer.put(tex, 1.0f, 1.0f, 0f)
            uvBuffer.put(tex, 0.0f, 1.0f, 0f)
            uvBuffer.put(tex, 1.0f, 0.0f, 0f)
            uvBuffer.put(tex, 0.0f, 0.0f, 0f)
            uvBuffer.put(tex, 1.0f, 0.0f, 0f)
            uvBuffer.put(tex, 0.0f, 1.0f, 0f)
        }
        return 6
    }

    fun upload(tileModel: TileModel, vertexBuffer: GpuIntBuffer, uvBuffer: GpuFloatBuffer): Int {
        val faceX = tileModel.faceX
        val faceY = tileModel.faceY
        val faceZ = tileModel.faceZ
        val vertexX = tileModel.vertexX
        val vertexY = tileModel.vertexY
        val vertexZ = tileModel.vertexZ
        val triangleColorA = tileModel.triangleColorA
        val triangleColorB = tileModel.triangleColorB
        val triangleColorC = tileModel.triangleColorC
        val triangleTextures = tileModel.triangleTextureId
        val faceCount = faceX.size
        vertexBuffer.ensureCapacity(faceCount * 12)
        uvBuffer.ensureCapacity(faceCount * 12)
        var cnt = 0
        for (i in 0 until faceCount) {
            val triangleA = faceX[i]
            val triangleB = faceY[i]
            val triangleC = faceZ[i]
            val colorA = triangleColorA[i]
            val colorB = triangleColorB[i]
            val colorC = triangleColorC[i]
            if (colorA == 12345678) {
                continue
            }
            cnt += 3
            val vertexXA = vertexX[triangleA]
            val vertexZA = vertexZ[triangleA]
            val vertexXB = vertexX[triangleB]
            val vertexZB = vertexZ[triangleB]
            val vertexXC = vertexX[triangleC]
            val vertexZC = vertexZ[triangleC]
            vertexBuffer.put(vertexXA, vertexY[triangleA], vertexZA, colorA)
            vertexBuffer.put(vertexXB, vertexY[triangleB], vertexZB, colorB)
            vertexBuffer.put(vertexXC, vertexY[triangleC], vertexZC, colorC)
            if (triangleTextures != null) {
                if (triangleTextures[i] != -1) {
                    val tex = triangleTextures[i] + 1f
                    uvBuffer.put(tex, vertexXA / 128f, vertexZA / 128f, 0f)
                    uvBuffer.put(tex, vertexXB / 128f, vertexZB / 128f, 0f)
                    uvBuffer.put(tex, vertexXC / 128f, vertexZC / 128f, 0f)
                } else {
                    uvBuffer.put(0f, 0f, 0f, 0f)
                    uvBuffer.put(0f, 0f, 0f, 0f)
                    uvBuffer.put(0f, 0f, 0f, 0f)
                }
            }
        }
        return cnt
    }

    fun uploadModel(entity: Entity, vertexBuffer: GpuIntBuffer, uvBuffer: GpuFloatBuffer): Int {
        val model = entity.model

        if (model.computeObj.offset >= 0) {
//            // this model is shared between gameobjects and has already been uploaded
//            // copy the computeObj so that we can maintain a reference to the vertexs on the GPU
//            // but also modify the position of this specific model
//            model.computeObj = model.computeObj.copy()
//            return -1
        }

        model.computeObj.offset = offset
        if (model.modelDefinition.faceTextures != null) {
            model.computeObj.uvOffset = uvOffset
        } else {
            model.computeObj.uvOffset = -1
        }
        vertexBuffer.ensureCapacity(model.modelDefinition.faceCount * 12)
        uvBuffer.ensureCapacity(model.modelDefinition.faceCount * 12)
        val triangleCount: Int = model.modelDefinition.faceCount
        var len = 0
        for (i in 0 until triangleCount) {
            len += pushFace(model, i, vertexBuffer, uvBuffer)
        }
        model.computeObj.size = len / 3
        offset += len
        if (model.modelDefinition.faceTextures != null) {
            uvOffset += len
        }

        return len
    }

    fun pushFace(model: Model, face: Int, vertexBuffer: GpuIntBuffer, uvBuffer: GpuFloatBuffer): Int {
        val modelDefinition = model.modelDefinition
        val vertexX: IntArray = model.vertexPositionsX
        val vertexY: IntArray = model.vertexPositionsY
        val vertexZ: IntArray = model.vertexPositionsZ
        val trianglesX: IntArray = modelDefinition.faceVertexIndices1!!
        val trianglesY: IntArray = modelDefinition.faceVertexIndices2!!
        val trianglesZ: IntArray = modelDefinition.faceVertexIndices3!!
        val color1s: IntArray = model.faceColors1!!
        val color2s: IntArray = model.faceColors2!!
        val color3s: IntArray = model.faceColors3!!
        val transparencies: ByteArray? = modelDefinition.faceAlphas
        val faceTextures: ShortArray? = modelDefinition.faceTextures
        val facePriorities: ByteArray? = modelDefinition.faceRenderPriorities
        val triangleA = trianglesX[face]
        val triangleB = trianglesY[face]
        val triangleC = trianglesZ[face]
        val color1 = color1s[face]
        var color2 = color2s[face]
        var color3 = color3s[face]

        var alpha = 0
        if (transparencies != null && (faceTextures == null || faceTextures[face].toInt() == -1)) {
            alpha = transparencies[face].toInt() and 0xFF shl 24
        }
        var priority = 0
        if (facePriorities != null) {
            priority = facePriorities[face].toInt() and 0xff shl 16
        }
        if (color3 == -1) {
            color3 = color1
            color2 = color3
        } else if (color3 == -2) {
            vertexBuffer.put(0, 0, 0, 0)
            vertexBuffer.put(0, 0, 0, 0)
            vertexBuffer.put(0, 0, 0, 0)
            if (faceTextures != null) {
                uvBuffer.put(0f, 0f, 0f, 0f)
                uvBuffer.put(0f, 0f, 0f, 0f)
                uvBuffer.put(0f, 0f, 0f, 0f)
            }
            return 3
        }
        var a: Int
        var b: Int
        var c: Int
        a = vertexX[triangleA]
        b = vertexY[triangleA]
        c = vertexZ[triangleA]
        vertexBuffer.put(a, b, c, alpha or priority or color1)
        a = vertexX[triangleB]
        b = vertexY[triangleB]
        c = vertexZ[triangleB]
        vertexBuffer.put(a, b, c, alpha or priority or color2)
        a = vertexX[triangleC]
        b = vertexY[triangleC]
        c = vertexZ[triangleC]
        vertexBuffer.put(a, b, c, alpha or priority or color3)
        val u: Array<FloatArray?>? = modelDefinition.faceTextureUCoordinates
        val v: Array<FloatArray?>? = modelDefinition.faceTextureVCoordinates
        var uf: FloatArray? = null
        var vf: FloatArray? = null
        if (faceTextures != null) {
            if (u != null && v != null && u[face].also { uf = it } != null && v[face].also { vf = it } != null
            ) {
                val texture = faceTextures[face] + 1f
                uvBuffer.put(texture, uf!![0], vf!![0], 0f)
                uvBuffer.put(texture, uf!![1], vf!![1], 0f)
                uvBuffer.put(texture, uf!![2], vf!![2], 0f)
            } else {
                uvBuffer.put(0f, 0f, 0f, 0f)
                uvBuffer.put(0f, 0f, 0f, 0f)
                uvBuffer.put(0f, 0f, 0f, 0f)
            }
        }
        return 3
    }
}
