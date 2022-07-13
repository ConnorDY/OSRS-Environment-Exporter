package controllers.worldRenderer

import AppConstants
import cache.utils.ColorPalette.Companion.rs2hsbToColor
import controllers.worldRenderer.entities.*
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
    class FileWriters (
        private val tfile: PrintWriter,
        private val mfile: PrintWriter,
        private val colourFile: PrintWriter
    ) {
        fun writeToTFile(value: String) {
            tfile.write(value)
        }

        fun writeToMFile(value: String) {
            mfile.write(value)
        }

        fun writeToColourFile(value: String) {
            colourFile.write(value)
        }
    }

    var sceneId = (System.currentTimeMillis() / 1000L).toInt()
    var vertexcountc = 0
    var vertexcountt = 0
    var objcount = 0
    var objt: Int
    var objm: Int
    var scale: Float
    var materials: Boolean
    var textures: Boolean

    fun exportSceneToFile(scene: Scene, renderer: Renderer) {
        Intrinsics.checkParameterIsNotNull(scene, "scene")
        Intrinsics.checkParameterIsNotNull(renderer, "renderer")

        vertexcountc = 0
        vertexcountt = 0

        // create timestamped output directory
        val timestamp = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH_mm_ss.SSSSSS")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())
        val outDir = "${AppConstants.OUTPUT_DIRECTORY}/${timestamp}"
        File(outDir).mkdirs()

        File("${outDir}/temp.obj").printWriter().use {
            val tfile = it
            File("${outDir}/materials.obj").printWriter().use {
                val mfile = it
                File("${outDir}/col.mtl").printWriter().use {
                    val colourfile = it
                    val fileWriters = FileWriters(tfile, mfile, colourfile)

                    writeMaterials(fileWriters)
                    if (materials) {
                        fileWriters.writeToMFile("mtllib col.mtl\n")
                    }

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
                                            this.upload(fileWriters, tile)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    copyTextures(outDir)
                }
            }
        }

        // use python to convert obj to ply
        val process =
            ProcessBuilder(".\\Program\\convert.exe").start()
        process.waitFor()
    }

    private fun writeMaterials(fileWriters: FileWriters) {
        for (rx in 0 until 98) {
            if (rx != 54) {
                fileWriters.writeToColourFile(
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

    fun upload(fileWriters: FileWriters, tile: SceneTile) {
        tile.tilePaint?.let { upload(fileWriters, it) }
        tile.tileModel?.let { upload(fileWriters, it) }
        uploadIfStatic(fileWriters, tile.wall?.entity)
        uploadIfStatic(fileWriters, tile.wall?.entity2)
        uploadIfStatic(fileWriters, tile.wallDecoration?.entity)
        uploadIfStatic(fileWriters, tile.floorDecoration?.entity)
        tile.gameObjects.map { it.entity }.forEach {
            uploadIfStatic(fileWriters, it)
        }
    }

    private fun uploadIfStatic(fileWriters: FileWriters, entity: Entity?) {
        if (entity is StaticObject) uploadModel(fileWriters, entity)
    }

    fun writevertex(fileWriters: FileWriters, v1: Int, v2: Int, v3: Int, c: Int) {
        if (objm != objcount) {
            objm = objcount
            fileWriters.writeToMFile(
                "o obj${objcount}\n"
            )
        }
        ++vertexcountt
        fileWriters.writeToMFile(
            "v " + v1.toFloat() / scale + " "
                + (-v2).toFloat() / scale + " "
                + (-v3).toFloat() / scale + " " + "\n"
        )
    }

    fun writevertexcolor(fileWriters: FileWriters, v1: Int, v2: Int, v3: Int, c: Int) {
        if (objt != objcount) {
            objt = objcount
            fileWriters.writeToTFile(
                "o obj${objcount}\n"
            )
        }
        ++vertexcountc
        val color = rs2hsbToColor(c)
        fileWriters.writeToTFile(
            "v " + v1.toFloat() / scale + " " +
                + (-v2).toFloat() / scale + " "
                + (-v3).toFloat() / scale + " "
                + (color.red.toFloat() / 255.0f).toDouble() + " "
                + (color.green.toFloat() / 255.0f).toDouble() + " "
                + (color.blue.toFloat() / 255.0f).toDouble() + "\n"
        )
    }

    fun writetexture(fileWriters: FileWriters, v1: Float, v2: Float, v3: Float, c: Float) {
        fileWriters.writeToMFile(
            "vt $v2 ${1f - v3}\n"
        )
    }

    fun write3facem(fileWriters: FileWriters) {
        fileWriters.writeToMFile(
            "f ${vertexcountt - 2}/${vertexcountt - 2} ${vertexcountt - 1}/${vertexcountt - 1} ${vertexcountt}/${vertexcountt}\n"
        )
    }

    fun write3facet(fileWriters: FileWriters) {
        fileWriters.writeToTFile(
            "f ${vertexcountc - 2} ${vertexcountc - 1} ${vertexcountc}\n"
        )
    }

    fun upload(fileWriters: FileWriters, tile: TilePaint) {
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
                        fileWriters,
                        vertexAx + tile.computeObj.x,
                        neHeight + tile.computeObj.y,
                        vertexAy + tile.computeObj.z,
                        neColor
                    )
                    writevertex(
                        fileWriters,
                        vertexBx + tile.computeObj.x,
                        nwHeight + tile.computeObj.y,
                        vertexBy + tile.computeObj.z,
                        nwColor
                    )
                    writevertex(
                        fileWriters,
                        vertexCx + tile.computeObj.x,
                        seHeight + tile.computeObj.y,
                        vertexCy + tile.computeObj.z,
                        seColor
                    )
                    writetexture(fileWriters, tex, 1.0f, 1.0f, 0.0f)
                    writetexture(fileWriters, tex, 0.0f, 1.0f, 0.0f)
                    writetexture(fileWriters, tex, 1.0f, 0.0f, 0.0f)
                    fileWriters.writeToMFile(
                        "usemtl t${(tex - 1f).toInt()}\n"
                    )
                    write3facem(fileWriters)
                    writevertex(
                        fileWriters,
                        vertexDx + tile.computeObj.x,
                        swHeight + tile.computeObj.y,
                        vertexDy + tile.computeObj.z,
                        swColor
                    )
                    writevertex(
                        fileWriters,
                        vertexCx + tile.computeObj.x,
                        seHeight + tile.computeObj.y,
                        vertexCy + tile.computeObj.z,
                        seColor
                    )
                    writevertex(
                        fileWriters,
                        vertexBx + tile.computeObj.x,
                        nwHeight + tile.computeObj.y,
                        vertexBy + tile.computeObj.z,
                        nwColor
                    )
                    writetexture(fileWriters, tex, 0.0f, 0.0f, 0.0f)
                    writetexture(fileWriters, tex, 1.0f, 0.0f, 0.0f)
                    writetexture(fileWriters, tex, 0.0f, 1.0f, 0.0f)
                    fileWriters.writeToMFile(
                        "usemtl t${(tex - 1f).toInt()}\n"
                    )
                    write3facem(fileWriters)
                }
            } else if (textures) {
                writevertexcolor(
                    fileWriters,
                    vertexAx + tile.computeObj.x,
                    neHeight + tile.computeObj.y,
                    vertexAy + tile.computeObj.z,
                    neColor
                )
                writevertexcolor(
                    fileWriters,
                    vertexBx + tile.computeObj.x,
                    nwHeight + tile.computeObj.y,
                    vertexBy + tile.computeObj.z,
                    nwColor
                )
                writevertexcolor(
                    fileWriters,
                    vertexCx + tile.computeObj.x,
                    seHeight + tile.computeObj.y,
                    vertexCy + tile.computeObj.z,
                    seColor
                )
                write3facet(fileWriters)
                writevertexcolor(
                    fileWriters,
                    vertexDx + tile.computeObj.x,
                    swHeight + tile.computeObj.y,
                    vertexDy + tile.computeObj.z,
                    swColor
                )
                writevertexcolor(
                    fileWriters,
                    vertexCx + tile.computeObj.x,
                    seHeight + tile.computeObj.y,
                    vertexCy + tile.computeObj.z,
                    seColor
                )
                writevertexcolor(
                    fileWriters,
                    vertexBx + tile.computeObj.x,
                    nwHeight + tile.computeObj.y,
                    vertexBy + tile.computeObj.z,
                    nwColor
                )
                write3facet(fileWriters)
            }
        }
    }

    fun upload(fileWriters: FileWriters, tileModel: TileModel) {
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
                            fileWriters,
                            vertexXA + tileModel.computeObj.x,
                            vertexY[triangleA] + tileModel.computeObj.y,
                            vertexZA + tileModel.computeObj.z,
                            colorA
                        )
                        writevertex(
                            fileWriters,
                            vertexXB + tileModel.computeObj.x,
                            vertexY[triangleB] + tileModel.computeObj.y,
                            vertexZB + tileModel.computeObj.z,
                            colorB
                        )
                        writevertex(
                            fileWriters,
                            vertexXC + tileModel.computeObj.x,
                            vertexY[triangleC] + tileModel.computeObj.y,
                            vertexZC + tileModel.computeObj.z,
                            colorC
                        )
                        writetexture(
                            fileWriters,
                            tex,
                            vertexXA.toFloat() / 128.0f,
                            vertexZA.toFloat() / 128.0f,
                            0.0f
                        )
                        writetexture(
                            fileWriters,
                            tex,
                            vertexXB.toFloat() / 128.0f,
                            vertexZB.toFloat() / 128.0f,
                            0.0f
                        )
                        writetexture(
                            fileWriters,
                            tex,
                            vertexXC.toFloat() / 128.0f,
                            vertexZC.toFloat() / 128.0f,
                            0.0f
                        )
                        fileWriters.writeToMFile(
                            "usemtl t${(tex - 1f).toInt()}\n"
                        )
                        write3facem(fileWriters)
                    }
                } else if (textures) {
                    writevertexcolor(
                        fileWriters,
                        vertexXA + tileModel.computeObj.x,
                        vertexY[triangleA] + tileModel.computeObj.y,
                        vertexZA + tileModel.computeObj.z,
                        colorA
                    )
                    writevertexcolor(
                        fileWriters,
                        vertexXB + tileModel.computeObj.x,
                        vertexY[triangleB] + tileModel.computeObj.y,
                        vertexZB + tileModel.computeObj.z,
                        colorB
                    )
                    writevertexcolor(
                        fileWriters,
                        vertexXC + tileModel.computeObj.x,
                        vertexY[triangleC] + tileModel.computeObj.y,
                        vertexZC + tileModel.computeObj.z,
                        colorC
                    )
                    write3facet(fileWriters)
                }
            }
        }
    }

    private fun uploadModel(fileWriters: FileWriters, entity: Entity) {
        val model = entity.getModel()
        if (model.computeObj.offset >= 0) {
        }
        val triangleCount = model.modelDefinition.faceCount
        for (i in 0 until triangleCount) {
            pushFace(fileWriters, model, i)
        }
    }

    private fun pushFace(fileWriters: FileWriters, model: Model, face: Int) {
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
                                fileWriters,
                                a + model.computeObj.x,
                                b + model.computeObj.y,
                                c + model.computeObj.z,
                                alpha or priority or color1
                            )
                            a = vertexX[triangleB]
                            b = vertexY[triangleB]
                            c = vertexZ[triangleB]
                            writevertex(
                                fileWriters,
                                a + model.computeObj.x,
                                b + model.computeObj.y,
                                c + model.computeObj.z,
                                alpha or priority or color2
                            )
                            a = vertexX[triangleC]
                            b = vertexY[triangleC]
                            c = vertexZ[triangleC]
                            writevertex(
                                fileWriters,
                                a + model.computeObj.x,
                                b + model.computeObj.y,
                                c + model.computeObj.z,
                                alpha or priority or color3
                            )
                            var var10002 = uf!![0]
                            writetexture(fileWriters, texture, var10002, vf!![0], 0.0f)
                            var10002 = uf[1]
                            writetexture(fileWriters, texture, var10002, vf[1], 0.0f)
                            var10002 = uf[2]
                            writetexture(fileWriters, texture, var10002, vf[2], 0.0f)
                            fileWriters.writeToMFile(
                                "usemtl t${(texture - 1f).toInt()}\n"
                            )
                            write3facem(fileWriters)
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
                fileWriters,
                a + model.computeObj.x,
                b + model.computeObj.y,
                c + model.computeObj.z,
                alpha or priority or color1
            )
            a = vertexX[triangleB]
            b = vertexY[triangleB]
            c = vertexZ[triangleB]
            writevertexcolor(
                fileWriters,
                a + model.computeObj.x,
                b + model.computeObj.y,
                c + model.computeObj.z,
                alpha or priority or color2
            )
            a = vertexX[triangleC]
            b = vertexY[triangleC]
            c = vertexZ[triangleC]
            writevertexcolor(
                fileWriters,
                a + model.computeObj.x,
                b + model.computeObj.y,
                c + model.computeObj.z,
                alpha or priority or color3
            )
            write3facet(fileWriters)
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