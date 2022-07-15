package controllers.worldRenderer

import AppConstants
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
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
import kotlin.jvm.internal.Intrinsics

class SceneExporter {
    var sceneId = (System.currentTimeMillis() / 1000L).toInt()
    var vertexcountc = 0
    var vertexcountt = 0
    var objcount = 0
    var objt: Int
    var objm: Int
    var scale: Float
    var materials: Boolean
    var textures: Boolean
    var verticesT = ArrayList<FloatArray>()
    var verticesM = ArrayList<FloatArray>()

    fun exportSceneToFile(scene: Scene, renderer: Renderer) {
        Intrinsics.checkParameterIsNotNull(scene, "scene")
        Intrinsics.checkParameterIsNotNull(renderer, "renderer")

        vertexcountc = 0
        vertexcountt = 0

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
        val gltf = glTF(outDir)

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

        writeVerticesT(gltf)
        writeVerticesM(gltf)

        // convert to JSON
        val mapper = ObjectMapper()
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
        val json = mapper.writeValueAsString(gltf)

        // write to file
        File("${outDir}/scene.gltf").printWriter().use {
            it.write(json)
        }

        // copy textures
        copyTextures(outDir)
    }

    private fun writeMaterials(file: PrintWriter) {
        for (rx in 0 until 98) {
            if (rx != 54) {
                file.write(
                    """newmtl t$rx
 Kd 1.000 1.000 1.000
 Ks 0 0 0
 d 1.0
 illum 2
 map_Kd ./${AppConstants.TEXTURES_DIRECTORY_NAME}/$rx.png
"""
                )
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

    fun writeVerticesT(gltf: glTF) {
        if (verticesT.size > 0) {
            gltf.addMesh(verticesT, "t")
        }
    }

    fun writeVerticesM(gltf: glTF) {
        if (verticesM.size > 0) {
            gltf.addMesh(verticesM, "m")
        }
    }

    private fun writevertex(gltf: glTF, v1: Int, v2: Int, v3: Int, c: Int) {
        if (objm != objcount) {
            objm = objcount
            writeVerticesM(gltf)
            verticesM = ArrayList<FloatArray>()
        }
        verticesM.add(floatArrayOf(
            v1.toFloat() / scale,
            v2.toFloat() / scale,
            v3.toFloat() / scale
        ))
        ++vertexcountt
    }

    private fun writevertexcolor(gltf: glTF, v1: Int, v2: Int, v3: Int, c: Int) {
        if (objt != objcount) {
            objt = objcount
            writeVerticesT(gltf)
            verticesT = ArrayList<FloatArray>()
        }
        verticesT.add(floatArrayOf(
            v1.toFloat() / scale,
            v2.toFloat() / scale,
            v3.toFloat() / scale
        ))
        ++vertexcountc

        // TODO: re-implement this
        // previous implementation:
        // val color = rs2hsbToColor(c)
        // fileWriters.writeToTFile(
        //     "v " + v1.toFloat() / scale + " " +
        //         + (-v2).toFloat() / scale + " "
        //         + (-v3).toFloat() / scale + " "
        //         + (color.red.toFloat() / 255.0f).toDouble() + " "
        //         + (color.green.toFloat() / 255.0f).toDouble() + " "
        //         + (color.blue.toFloat() / 255.0f).toDouble() + "\n"
        // )
    }

    private fun writetexture(gltf: glTF, v1: Float, v2: Float, v3: Float, c: Float) {
        // TODO: implement this
        // previous implementation:
        // fileWriters.writeToMFile(
        //     "vt $v2 ${1f - v3}\n"
        // )
    }

    private fun write3facem(gltf: glTF) {
        // TODO: implement this
        // previous implementation:
        // fileWriters.writeToMFile(
        //     "f ${vertexcountt - 2}/${vertexcountt - 2} ${vertexcountt - 1}/${vertexcountt - 1} ${vertexcountt}/${vertexcountt}\n"
        // )
    }

    private fun write3facet(gltf: glTF) {
        // TODO: implement this
        // previous implementation:
        // fileWriters.writeToTFile(
        //     "f ${vertexcountc - 2} ${vertexcountc - 1} ${vertexcountc}\n"
        // )
    }

    private fun upload(gltf: glTF, tile: TilePaint) {
        Intrinsics.checkParameterIsNotNull(tile, "tile")
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
            if (tile.texture != -1) {
                val tex = tile.texture.toFloat() + 1.0f
                if (materials) {
                    writevertex(
                        gltf,
                        vertexAx + tile.computeObj.x,
                        neHeight + tile.computeObj.y,
                        vertexAy + tile.computeObj.z,
                        neColor
                    )
                    writevertex(
                        gltf,
                        vertexBx + tile.computeObj.x,
                        nwHeight + tile.computeObj.y,
                        vertexBy + tile.computeObj.z,
                        nwColor
                    )
                    writevertex(
                        gltf,
                        vertexCx + tile.computeObj.x,
                        seHeight + tile.computeObj.y,
                        vertexCy + tile.computeObj.z,
                        seColor
                    )
                    writetexture(gltf, tex, 1.0f, 1.0f, 0.0f)
                    writetexture(gltf, tex, 0.0f, 1.0f, 0.0f)
                    writetexture(gltf, tex, 1.0f, 0.0f, 0.0f)
                    // TODO: implement this
                    // previous implementation:
                    // fileWriters.writeToMFile(
                    //     "usemtl t${(tex - 1f).toInt()}\n"
                    // )
                    write3facem(gltf)
                    writevertex(
                        gltf,
                        vertexDx + tile.computeObj.x,
                        swHeight + tile.computeObj.y,
                        vertexDy + tile.computeObj.z,
                        swColor
                    )
                    writevertex(
                        gltf,
                        vertexCx + tile.computeObj.x,
                        seHeight + tile.computeObj.y,
                        vertexCy + tile.computeObj.z,
                        seColor
                    )
                    writevertex(
                        gltf,
                        vertexBx + tile.computeObj.x,
                        nwHeight + tile.computeObj.y,
                        vertexBy + tile.computeObj.z,
                        nwColor
                    )
                    writetexture(gltf, tex, 0.0f, 0.0f, 0.0f)
                    writetexture(gltf, tex, 1.0f, 0.0f, 0.0f)
                    writetexture(gltf, tex, 0.0f, 1.0f, 0.0f)
                    // TODO: implement this
                    // previous implementation:
                    // fileWriters.writeToMFile(
                    //     "usemtl t${(tex - 1f).toInt()}\n"
                    // )
                    write3facem(gltf)
                }
            } else if (textures) {
                writevertexcolor(
                    gltf,
                    vertexAx + tile.computeObj.x,
                    neHeight + tile.computeObj.y,
                    vertexAy + tile.computeObj.z,
                    neColor
                )
                writevertexcolor(
                    gltf,
                    vertexBx + tile.computeObj.x,
                    nwHeight + tile.computeObj.y,
                    vertexBy + tile.computeObj.z,
                    nwColor
                )
                writevertexcolor(
                    gltf,
                    vertexCx + tile.computeObj.x,
                    seHeight + tile.computeObj.y,
                    vertexCy + tile.computeObj.z,
                    seColor
                )
                write3facet(gltf)
                writevertexcolor(
                    gltf,
                    vertexDx + tile.computeObj.x,
                    swHeight + tile.computeObj.y,
                    vertexDy + tile.computeObj.z,
                    swColor
                )
                writevertexcolor(
                    gltf,
                    vertexCx + tile.computeObj.x,
                    seHeight + tile.computeObj.y,
                    vertexCy + tile.computeObj.z,
                    seColor
                )
                writevertexcolor(
                    gltf,
                    vertexBx + tile.computeObj.x,
                    nwHeight + tile.computeObj.y,
                    vertexBy + tile.computeObj.z,
                    nwColor
                )
                write3facet(gltf)
            }
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
                if (triangleTextures != null && triangleTextures[i] != -1) {
                    val tex = triangleTextures[i].toFloat() + 1.0f
                    if (materials) {
                        writevertex(
                            gltf,
                            vertexXA + tileModel.computeObj.x,
                            vertexY[triangleA] + tileModel.computeObj.y,
                            vertexZA + tileModel.computeObj.z,
                            colorA
                        )
                        writevertex(
                            gltf,
                            vertexXB + tileModel.computeObj.x,
                            vertexY[triangleB] + tileModel.computeObj.y,
                            vertexZB + tileModel.computeObj.z,
                            colorB
                        )
                        writevertex(
                            gltf,
                            vertexXC + tileModel.computeObj.x,
                            vertexY[triangleC] + tileModel.computeObj.y,
                            vertexZC + tileModel.computeObj.z,
                            colorC
                        )
                        writetexture(
                            gltf,
                            tex,
                            vertexXA.toFloat() / 128.0f,
                            vertexZA.toFloat() / 128.0f,
                            0.0f
                        )
                        writetexture(
                            gltf,
                            tex,
                            vertexXB.toFloat() / 128.0f,
                            vertexZB.toFloat() / 128.0f,
                            0.0f
                        )
                        writetexture(
                            gltf,
                            tex,
                            vertexXC.toFloat() / 128.0f,
                            vertexZC.toFloat() / 128.0f,
                            0.0f
                        )
                        // TODO: implement this
                        // previous implementation:
                        // fileWriters.writeToMFile(
                        //     "usemtl t${(tex - 1f).toInt()}\n"
                        // )
                        write3facem(gltf)
                    }
                } else if (textures) {
                    writevertexcolor(
                        gltf,
                        vertexXA + tileModel.computeObj.x,
                        vertexY[triangleA] + tileModel.computeObj.y,
                        vertexZA + tileModel.computeObj.z,
                        colorA
                    )
                    writevertexcolor(
                        gltf,
                        vertexXB + tileModel.computeObj.x,
                        vertexY[triangleB] + tileModel.computeObj.y,
                        vertexZB + tileModel.computeObj.z,
                        colorB
                    )
                    writevertexcolor(
                        gltf,
                        vertexXC + tileModel.computeObj.x,
                        vertexY[triangleC] + tileModel.computeObj.y,
                        vertexZC + tileModel.computeObj.z,
                        colorC
                    )
                    write3facet(gltf)
                }
            }
        }
    }

    private fun uploadModel(gltf: glTF, entity: Entity) {
        val model = entity.getModel()
        if (model.computeObj.offset >= 0) {
        }
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
        if (faceTextures != null) {
            if (u != null && v != null) {
                var var31 = u[face]
                val uf = var31
                if (var31 != null) {
                    var31 = v[face]
                    val vf = var31
                    if (var31 != null && faceTextures[face].toInt() != -1) {
                        val texture = faceTextures[face].toFloat() + 1.0f
                        if (materials) {
                            var a = vertexX[triangleA]
                            var b = vertexY[triangleA]
                            var c = vertexZ[triangleA]
                            writevertex(
                                gltf,
                                a + model.computeObj.x,
                                b + model.computeObj.y,
                                c + model.computeObj.z,
                                alpha or priority or color1
                            )
                            a = vertexX[triangleB]
                            b = vertexY[triangleB]
                            c = vertexZ[triangleB]
                            writevertex(
                                gltf,
                                a + model.computeObj.x,
                                b + model.computeObj.y,
                                c + model.computeObj.z,
                                alpha or priority or color2
                            )
                            a = vertexX[triangleC]
                            b = vertexY[triangleC]
                            c = vertexZ[triangleC]
                            writevertex(
                                gltf,
                                a + model.computeObj.x,
                                b + model.computeObj.y,
                                c + model.computeObj.z,
                                alpha or priority or color3
                            )
                            var var10002 = uf!![0]
                            writetexture(gltf, texture, var10002, vf!![0], 0.0f)
                            var10002 = uf[1]
                            writetexture(gltf, texture, var10002, vf[1], 0.0f)
                            var10002 = uf[2]
                            writetexture(gltf, texture, var10002, vf[2], 0.0f)
                            // TODO: implement this
                            // previous implementation:
                            // fileWriters.writeToMFile(
                            //     "usemtl t${(texture - 1f).toInt()}\n"
                            // )
                            write3facem(gltf)
                        }
                        return
                    }
                }
            }
        }
        if (textures) {
            var a = vertexX[triangleA]
            var b = vertexY[triangleA]
            var c = vertexZ[triangleA]
            writevertexcolor(
                gltf,
                a + model.computeObj.x,
                b + model.computeObj.y,
                c + model.computeObj.z,
                alpha or priority or color1
            )
            a = vertexX[triangleB]
            b = vertexY[triangleB]
            c = vertexZ[triangleB]
            writevertexcolor(
                gltf,
                a + model.computeObj.x,
                b + model.computeObj.y,
                c + model.computeObj.z,
                alpha or priority or color2
            )
            a = vertexX[triangleC]
            b = vertexY[triangleC]
            c = vertexZ[triangleC]
            writevertexcolor(
                gltf,
                a + model.computeObj.x,
                b + model.computeObj.y,
                c + model.computeObj.z,
                alpha or priority or color3
            )
            write3facet(gltf)
        }
    }

    init {
        objt = -1
        objm = -1
        scale = 100.0f
        materials = true
        textures = true
    }
}
