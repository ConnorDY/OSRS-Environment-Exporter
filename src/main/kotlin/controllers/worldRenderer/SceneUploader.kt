package controllers.worldRenderer

import cache.definitions.RegionDefinition
import cache.utils.put
import controllers.worldRenderer.entities.Entity
import controllers.worldRenderer.entities.StaticObject
import controllers.worldRenderer.entities.TileModel
import controllers.worldRenderer.entities.TilePaint
import models.DebugOptionsModel
import models.scene.REGION_SIZE
import models.scene.Scene
import models.scene.SceneTile
import java.nio.FloatBuffer

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
class SceneUploader(private val debugOptionsModel: DebugOptionsModel) {
    var sceneId = System.nanoTime().toInt()

    fun upload(scene: Scene, priorityRenderer: PriorityRenderer) {
        ++sceneId
        for (rx in 0 until scene.cols) {
            for (ry in 0 until scene.rows) {
                val region = scene.getRegion(rx, ry) ?: continue

                for (z in 0 until RegionDefinition.Z) {
                    for (x in 0 until REGION_SIZE) {
                        for (y in 0 until REGION_SIZE) {
                            val tile = region.tiles[z][x][y]
                            tile?.let {
                                upload(it, priorityRenderer)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun upload(tile: SceneTile, priorityRenderer: PriorityRenderer) {
// 		Tile bridge = tile.getBridge();
// 		if (bridge != null)
// 		{
// 			upload(bridge, vertexBuffer, uvBuffer);
// 		}
        val sceneTilePaint = tile.tilePaint
        if (sceneTilePaint != null && debugOptionsModel.showTilePaint.value.get()) {
            upload(sceneTilePaint, priorityRenderer)
        }

        val sceneTileModel = tile.tileModel
        if (sceneTileModel != null && debugOptionsModel.showTileModels.value.get()) {
            upload(sceneTileModel, priorityRenderer)
        }

        val wall = tile.wall
        if (wall != null) {
            val entity = wall.entity
            if (entity is StaticObject) {
                uploadModel(entity, priorityRenderer)
            }
            val entity2 = wall.entity2
            if (entity2 is StaticObject) {
                uploadModel(entity2, priorityRenderer)
            }
        }

        val wallDecoration = tile.wallDecoration
        if (wallDecoration != null) {
            val entity = wallDecoration.entity
            if (entity is StaticObject) {
                uploadModel(entity, priorityRenderer)
            }
            val entity2 = wallDecoration.entity2
            if (entity2 is StaticObject) {
                uploadModel(entity2, priorityRenderer)
            }
        }

        val floorDecoration = tile.floorDecoration
        if (floorDecoration != null) {
            val entity = floorDecoration.entity
            if (entity is StaticObject) {
                uploadModel(entity, priorityRenderer)
            }
        }

        for (gameObject in tile.gameObjects) {
            val entity = gameObject.entity
            if (entity is StaticObject) {
                uploadModel(entity, priorityRenderer)
            }
        }
    }

    private fun upload(tile: TilePaint, priorityRenderer: PriorityRenderer) {
        val swHeight = tile.swHeight
        val seHeight = tile.seHeight
        val neHeight = tile.neHeight
        val nwHeight = tile.nwHeight
        val neColor = tile.neColor
        val nwColor = tile.nwColor
        val seColor = tile.seColor
        val swColor = tile.swColor
        if (neColor == 12345678) {
            return
        }

        val (vertexBuffer, uvBuffer) =
            priorityRenderer.getBuffersForRenderable(tile, 2, tile.texture != -1)
        val strictUVs = priorityRenderer.needsStrictUVs || tile.texture != -1

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

        if (strictUVs) {
            val tex = tile.texture + 1f
            uvBuffer.put(tex, 1.0f, 1.0f, 0f)
            uvBuffer.put(tex, 0.0f, 1.0f, 0f)
            uvBuffer.put(tex, 1.0f, 0.0f, 0f)
            uvBuffer.put(tex, 0.0f, 0.0f, 0f)
            uvBuffer.put(tex, 1.0f, 0.0f, 0f)
            uvBuffer.put(tex, 0.0f, 1.0f, 0f)
        }
    }

    private fun upload(tileModel: TileModel, priorityRenderer: PriorityRenderer) {
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

        val trueFaceCount = triangleColorA.count { it != 12345678 }
        val (vertexBuffer, uvBuffer) =
            priorityRenderer.getBuffersForRenderable(tileModel, trueFaceCount, triangleTextures != null)
        val strictUVs = priorityRenderer.needsStrictUVs || triangleTextures != null

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
            val vertexXA = vertexX[triangleA]
            val vertexZA = vertexZ[triangleA]
            val vertexXB = vertexX[triangleB]
            val vertexZB = vertexZ[triangleB]
            val vertexXC = vertexX[triangleC]
            val vertexZC = vertexZ[triangleC]
            vertexBuffer.put(vertexXA, vertexY[triangleA], vertexZA, colorA)
            vertexBuffer.put(vertexXB, vertexY[triangleB], vertexZB, colorB)
            vertexBuffer.put(vertexXC, vertexY[triangleC], vertexZC, colorC)
            if (triangleTextures != null && triangleTextures[i] != -1) {
                val tex = triangleTextures[i] + 1f
                uvBuffer.put(tex, vertexXA / 128f, vertexZA / 128f, 0f)
                uvBuffer.put(tex, vertexXB / 128f, vertexZB / 128f, 0f)
                uvBuffer.put(tex, vertexXC / 128f, vertexZC / 128f, 0f)
            } else if (strictUVs) {
                pushNullUvs(uvBuffer)
            }
        }
    }

    private fun uploadModel(entity: Entity, priorityRenderer: PriorityRenderer) {
        val model = entity.model

        val showOnly = debugOptionsModel.showOnlyModelType.value.get()
        if (showOnly != null && showOnly != model.debugType) {
            return // Model is hidden for debug reasons
        }

        if (model.sceneId == sceneId) {
            return // Model has already been uploaded
        }
        model.sceneId = sceneId

        val modelDefinition = model.modelDefinition
        val triangleCount: Int = model.modelDefinition.faceCount

        val vertexX: IntArray = model.vertexPositionsX
        val vertexY: IntArray = model.vertexPositionsY
        val vertexZ: IntArray = model.vertexPositionsZ
        val trianglesX: IntArray = modelDefinition.faceVertexIndices1
        val trianglesY: IntArray = modelDefinition.faceVertexIndices2
        val trianglesZ: IntArray = modelDefinition.faceVertexIndices3
        val color1s: IntArray = model.faceColors1
        val color2s: IntArray = model.faceColors2
        val color3s: IntArray = model.faceColors3
        val transparencies: ByteArray? = modelDefinition.faceAlphas
        val faceTextures: ShortArray? = modelDefinition.faceTextures
        val facePriorities: ByteArray? = modelDefinition.faceRenderPriorities
        val uv: FloatArray? = modelDefinition.faceTextureUVCoordinates

        val (vertexBuffer, uvBuffer) =
            priorityRenderer.getBuffersForRenderable(model, triangleCount, faceTextures != null)
        val strictUVs = priorityRenderer.needsStrictUVs || faceTextures != null

        for (face in 0 until triangleCount) {
            val alphaPriority = packAlphaPriority(faceTextures, transparencies, facePriorities, face)

            val color1 = color1s[face]
            var color2 = color2s[face]
            var color3 = color3s[face]

            if (color3 == -1) {
                color3 = color1
                color2 = color3
            } else if (color3 == -2) {
                vertexBuffer.put(0, 0, 0, 0)
                vertexBuffer.put(0, 0, 0, 0)
                vertexBuffer.put(0, 0, 0, 0)
                if (strictUVs) {
                    pushNullUvs(uvBuffer)
                }
                continue
            }

            val triangleA = trianglesX[face]
            val triangleB = trianglesY[face]
            val triangleC = trianglesZ[face]
            vertexBuffer.put(vertexX[triangleA], vertexY[triangleA], vertexZ[triangleA], alphaPriority or color1)
            vertexBuffer.put(vertexX[triangleB], vertexY[triangleB], vertexZ[triangleB], alphaPriority or color2)
            vertexBuffer.put(vertexX[triangleC], vertexY[triangleC], vertexZ[triangleC], alphaPriority or color3)
            if (faceTextures != null) {
                pushUvForFace(faceTextures, uv, face, uvBuffer)
            } else if (strictUVs) {
                pushNullUvs(uvBuffer)
            }
        }
    }

    private fun pushUvForFace(faceTextures: ShortArray, uv: FloatArray?, face: Int, uvBuffer: FloatBuffer) {
        if (faceTextures[face] != (-1).toShort() && uv != null) {
            val texture = faceTextures[face] + 1f
            val idx = face * 6
            /* ktlint-disable no-multi-spaces */
            uvBuffer.put(texture, uv[idx    ], uv[idx + 1], 0f)
            uvBuffer.put(texture, uv[idx + 2], uv[idx + 3], 0f)
            uvBuffer.put(texture, uv[idx + 4], uv[idx + 5], 0f)
            /* ktlint-enable no-multi-spaces */
        } else {
            pushNullUvs(uvBuffer)
        }
    }

    private fun pushNullUvs(uvBuffer: FloatBuffer) {
        uvBuffer.put(0f, 0f, 0f, 0f)
        uvBuffer.put(0f, 0f, 0f, 0f)
        uvBuffer.put(0f, 0f, 0f, 0f)
    }

    private fun packAlphaPriority(faceTextures: ShortArray?, transparencies: ByteArray?, facePriorities: ByteArray?, face: Int): Int {
        var alpha = 0
        if (transparencies != null && (faceTextures == null || faceTextures[face].toInt() == -1)) {
            alpha = transparencies[face].toInt() and 0xFF shl 24
        }
        var priority = 0
        if (facePriorities != null) {
            priority = facePriorities[face].toInt() and 0xff shl 16
        }
        return alpha or priority
    }
}
