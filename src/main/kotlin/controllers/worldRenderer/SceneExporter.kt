package controllers.worldRenderer

import AppConstants
import controllers.worldRenderer.entities.*
import models.glTF.glTF
import models.scene.Scene
import models.scene.SceneTile
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class SceneExporter {
    var sceneId = (System.currentTimeMillis() / 1000L).toInt()

    fun exportSceneToFile(scene: Scene, renderer: Renderer) {
        // create output directory if it does not yet exist
        File(AppConstants.OUTPUT_DIRECTORY).mkdirs()

        // create timestamped output directory
        val timestamp = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH_mm_ss.SSSSSS")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())
        val outDir = "${AppConstants.OUTPUT_DIRECTORY}/${timestamp}"
        File(outDir).mkdirs()

        // init glTF builder
        val gltf = glTF()

        writeMaterials(gltf)

        ++sceneId

        for (rx in 0 until scene.radius) {
            for (ry in 0 until scene.radius) {
                val region = scene.getRegion(rx, ry) ?: continue
                for (z in 0 until 4) {
                    if (
                        (z != 0 || renderer.z0ChkBtnSelected) &&
                        (z != 1 || renderer.z1ChkBtnSelected) &&
                        (z != 2 || renderer.z2ChkBtnSelected) &&
                        (z != 3 || renderer.z3ChkBtnSelected)
                    ) {
                        for (x in 0 until 64) {
                            for (y in 0 until 64) {
                                val tile = region.tiles[z][x][y] ?: continue
                                this.upload(gltf, tile)
                            }
                        }
                    }
                }
            }
        }

        gltf.save(outDir)

        // copy textures
        copyTextures(outDir)
    }

    private fun writeMaterials(gltf: glTF) {
        for (rx in 0 until 98) {
            if (rx != 54) {
                gltf.addTextureMaterial("./${AppConstants.TEXTURES_DIRECTORY_NAME}/$rx.png")
            }
        }
    }

    private fun copyTextures(outDir: String) {
        val src = Path.of(AppConstants.TEXTURES_DIRECTORY)
        val dest = Path.of("${outDir}/${AppConstants.TEXTURES_DIRECTORY_NAME}")

        Files.walk(src).forEach {
            Files.copy(
                it,
                dest.resolve(src.relativize(it)),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    private fun upload(gltf: glTF, tile: SceneTile) {
        tile.tilePaint?.let { upload(gltf, it) }
        tile.tileModel?.let { upload(gltf, it) }

        uploadIfStatic(gltf, tile.wall?.entity)
        uploadIfStatic(gltf, tile.wall?.entity2)

        uploadIfStatic(gltf, tile.wallDecoration?.entity)
        uploadIfStatic(gltf, tile.floorDecoration?.entity)

        tile.gameObjects.map { it.entity }.forEach {
            uploadIfStatic(gltf, it)
        }
    }

    private fun uploadIfStatic(gltf: glTF, entity: Entity?) {
        if (entity is StaticObject) uploadModel(gltf, entity)
    }

    private fun upload(gltf: glTF, tile: TilePaint) {
        val swHeight = tile.swHeight
        val seHeight = tile.seHeight
        val neHeight = tile.neHeight
        val nwHeight = tile.nwHeight

        val neColor = tile.neColor
        val nwColor = tile.nwColor
        val seColor = tile.seColor
        val swColor = tile.swColor

        if (neColor != 12345678) {
            val vertexDx = 0
            val vertexDy = 0
            val vertexCx = 128
            val vertexCy = 0
            val vertexAx = 128
            val vertexAy = 128
            val vertexBx = 0
            val vertexBy = 128

            val materialBuffer = gltf.getOrCreateBuffersForMaterial(tile.texture)

            materialBuffer.addVertex(
                (vertexAx + tile.computeObj.x).toFloat() / scale,
                (neHeight + tile.computeObj.y).toFloat() / scale,
                (vertexAy + tile.computeObj.z).toFloat() / scale,
                1f, 1f, neColor
            )
            materialBuffer.addVertex(
                (vertexBx + tile.computeObj.x).toFloat() / scale,
                (nwHeight + tile.computeObj.y).toFloat() / scale,
                (vertexBy + tile.computeObj.z).toFloat() / scale,
                0f, 1f, nwColor
            )
            materialBuffer.addVertex(
                (vertexCx + tile.computeObj.x).toFloat() / scale,
                (seHeight + tile.computeObj.y).toFloat() / scale,
                (vertexCy + tile.computeObj.z).toFloat() / scale,
                1f, 0f, seColor
            )

            materialBuffer.addVertex(
                (vertexDx + tile.computeObj.x).toFloat() / scale,
                (swHeight + tile.computeObj.y).toFloat() / scale,
                (vertexDy + tile.computeObj.z).toFloat() / scale,
                0f, 0f, swColor
            )
            materialBuffer.addVertex(
                (vertexCx + tile.computeObj.x).toFloat() / scale,
                (seHeight + tile.computeObj.y).toFloat() / scale,
                (vertexCy + tile.computeObj.z).toFloat() / scale,
                1f, 0f, seColor
            )
            materialBuffer.addVertex(
                (vertexBx + tile.computeObj.x).toFloat() / scale,
                (nwHeight + tile.computeObj.y).toFloat() / scale,
                (vertexBy + tile.computeObj.z).toFloat() / scale,
                0f, 1f, nwColor
            )
        }
    }

    private fun upload(gltf: glTF, tileModel: TileModel) {
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

        for (i in 0 until faceCount) {
            val triangleA = faceX[i]
            val triangleB = faceY[i]
            val triangleC = faceZ[i]

            val colorA = triangleColorA[i]
            val colorB = triangleColorB[i]
            val colorC = triangleColorC[i]

            if (colorA != 12345678) {
                val vertexXA = vertexX[triangleA]
                val vertexZA = vertexZ[triangleA]
                val vertexXB = vertexX[triangleB]
                val vertexZB = vertexZ[triangleB]
                val vertexXC = vertexX[triangleC]
                val vertexZC = vertexZ[triangleC]

                val textureId = if (triangleTextures != null) triangleTextures[i] else -1
                val materialBuffer = gltf.getOrCreateBuffersForMaterial(textureId)

                materialBuffer.addVertex(
                    (vertexXA + tileModel.computeObj.x).toFloat() / scale,
                    (vertexY[triangleA] + tileModel.computeObj.y).toFloat() / scale,
                    (vertexZA + tileModel.computeObj.z).toFloat() / scale,
                    vertexXA.toFloat() / 128.0f,
                    vertexZA.toFloat() / 128.0f,
                    colorA
                )
                materialBuffer.addVertex(
                    (vertexXB + tileModel.computeObj.x).toFloat() / scale,
                    (vertexY[triangleB] + tileModel.computeObj.y).toFloat() / scale,
                    (vertexZB + tileModel.computeObj.z).toFloat() / scale,
                    vertexXB.toFloat() / 128.0f,
                    vertexZB.toFloat() / 128.0f,
                    colorB
                )
                materialBuffer.addVertex(
                    (vertexXC + tileModel.computeObj.x).toFloat() / scale,
                    (vertexY[triangleC] + tileModel.computeObj.y).toFloat() / scale,
                    (vertexZC + tileModel.computeObj.z).toFloat() / scale,
                    vertexXC.toFloat() / 128.0f,
                    vertexZC.toFloat() / 128.0f,
                    colorC
                )
            }
        }
    }

    private fun uploadModel(gltf: glTF, entity: Entity) {
        val model = entity.getModel()
        val triangleCount = model.modelDefinition.faceCount

        for (i in 0 until triangleCount) {
            pushFace(gltf, model, i)
        }
    }

    private fun pushFace(gltf: glTF, model: Model, face: Int) {
        val modelDefinition = model.modelDefinition

        val vertexX = model.vertexPositionsX
        val vertexY = model.vertexPositionsY
        val vertexZ = model.vertexPositionsZ

        val trianglesX = modelDefinition.faceVertexIndices1
        val trianglesY = modelDefinition.faceVertexIndices2
        val trianglesZ = modelDefinition.faceVertexIndices3

        val color1s = model.faceColors1
        val color2s = model.faceColors2
        val color3s = model.faceColors3

        val transparencies = modelDefinition.faceAlphas
        val faceTextures = modelDefinition.faceTextures
        val facePriorities = modelDefinition.faceRenderPriorities

        val triangleA = trianglesX!![face]
        val triangleB = trianglesY!![face]
        val triangleC = trianglesZ!![face]

        val color1 = color1s!![face]
        var color2 = color2s!![face]
        var color3 = color3s!![face]
        var alpha = 0

        if (transparencies != null && (faceTextures == null || faceTextures[face].toInt() == -1)) {
            alpha = transparencies[face].toInt() and 255 shl 24
        }

        var priority = 0
        if (facePriorities != null) {
            priority = facePriorities[face].toInt() and 255 shl 16
        }

        if (color3 == -1) {
            color3 = color1
            color2 = color1
        } else if (color3 == -2) {
            return
        }

        val u = modelDefinition.faceTextureUCoordinates
        val v = modelDefinition.faceTextureVCoordinates

        val textureId = if (faceTextures != null) faceTextures[face].toInt() else -1
        val materialBuffer = gltf.getOrCreateBuffersForMaterial(textureId)

        var uf = floatArrayOf(0f, 0f, 0f)
        var vf = floatArrayOf(0f, 0f, 0f)

        if (u != null) {
            uf = u[face] ?: uf
        }

        if (v != null) {
            vf = v[face] ?: vf
        }

        materialBuffer.addVertex(
            (vertexX[triangleA] + model.computeObj.x).toFloat() / scale,
            (vertexY[triangleA] + model.computeObj.y).toFloat() / scale,
            (vertexZ[triangleA] + model.computeObj.z).toFloat() / scale,
            uf[0], vf[0],
            alpha or priority or color1
        )

        materialBuffer.addVertex(
            (vertexX[triangleB] + model.computeObj.x).toFloat() / scale,
            (vertexY[triangleB] + model.computeObj.y).toFloat() / scale,
            (vertexZ[triangleB] + model.computeObj.z).toFloat() / scale,
            uf[1], vf[1],
            alpha or priority or color2
        )

        materialBuffer.addVertex(
            (vertexX[triangleC] + model.computeObj.x).toFloat() / scale,
            (vertexY[triangleC] + model.computeObj.y).toFloat() / scale,
            (vertexZ[triangleC] + model.computeObj.z).toFloat() / scale,
            uf[2], vf[2],
            alpha or priority or color3
        )
    }

    companion object {
        const val scale = 100f
    }
}
