package controllers.worldRenderer

import cache.utils.ColorPalette.Companion.rs2hsbToColor
import controllers.worldRenderer.entities.*
import models.scene.Scene
import models.scene.SceneTile
import java.io.*
import kotlin.jvm.internal.Intrinsics

class SceneExporter {
    var sceneId = (System.currentTimeMillis() / 1000L).toInt()
    var tfile: PrintWriter
    var mfile: PrintWriter
    var colourfile: PrintWriter
    var vertexcountc = 0
    var vertexcountt = 0
    var objcount = 0
    var objt: Int
    var objm: Int
    var scale: Float
    var materials: Int
    var textures: Int

    fun exportSceneToFile(scene: Scene, renderer: Renderer) {
        Intrinsics.checkParameterIsNotNull(scene, "scene")
        Intrinsics.checkParameterIsNotNull(renderer, "renderer")
        vertexcountc = 0
        vertexcountt = 0
        tfile.close()
        mfile.close()
        colourfile.close()
        if (textures == 1) {
            tfile = File("./OBJ/temp.obj").printWriter()
        }
        if (materials == 1) {
            mfile = File("./OBJ/materials.obj").printWriter()
        }
        this.colourfile = File("./OBJ/col.mtl").printWriter()
        for (rx in 0 until 98) {
            if (rx != 54) {
                colourfile.write("newmtl t$rx\n")
                colourfile.write(" Kd 1.000 1.000 1.000\n")
                colourfile.write(" d 1.0\n")
                colourfile.write(" illum 2\n")
                colourfile.write(" map_Kd ./Textures/$rx.png\n")
            }
        }
        if (materials == 1) {
            mfile.write("mtllib col.mtl\n")
        }
        ++sceneId
        for (rx in 0 until scene.radius) {
            for (ry in 0 until scene.radius) {
                val region = scene.getRegion(rx, ry) ?: continue
                for (z in 0 until 4) {
                    if ((z != 0 || renderer.z0ChkBtnSelected) && (z != 1 || renderer.z1ChkBtnSelected) && (z != 2 || renderer.z2ChkBtnSelected) && (z != 3 || renderer.z3ChkBtnSelected)) {
                        for (x in 0 until 64) {
                            for (y in 0 until 64) {
                                val tile = region.tiles[z][x][y] ?: continue
                                this.upload(tile)
                            }
                        }
                    }
                }
            }
        }
        tfile.close()
        mfile.close()
        colourfile.close()
        val process =
            ProcessBuilder(".\\Program\\convert.exe").start()
        process.waitFor()
    }

    fun upload(tile: SceneTile) {
        val sceneTilePaint = tile.tilePaint
        if (sceneTilePaint != null) {
            this.upload(sceneTilePaint)
        }
        val sceneTileModel = tile.tileModel
        if (sceneTileModel != null) {
            this.upload(sceneTileModel)
        }
        val wall = tile.wall
        if (wall != null) {
            var entity = wall.entity
            if (entity is StaticObject) {
                uploadModel(entity)
            }
            entity = wall.entity2
            if (entity is StaticObject) {
                uploadModel(entity)
            }
        }
        val wallDecoration = tile.wallDecoration
        if (wallDecoration != null) {
            val entity = wallDecoration.entity
            if (entity is StaticObject) {
                uploadModel(entity)
            }
        }
        val floorDecoration = tile.floorDecoration
        if (floorDecoration != null) {
            val entity = floorDecoration.entity
            if (entity is StaticObject) {
                uploadModel(entity)
            }
        }
        for (gameObject in tile.gameObjects) {
            val entity = gameObject.entity
            if (entity is StaticObject) {
                uploadModel(entity)
            }
        }
    }

    fun writevertex(v1: Int, v2: Int, v3: Int, c: Int) {
        if (objm != objcount) {
            objm = objcount
            mfile.write(
                """
    o obj${objcount}
    
    """.trimIndent()
            )
        }
        ++vertexcountt
        mfile.write("v " + v1.toFloat() / scale + " " + (-v2).toFloat() / scale + " " + (-v3).toFloat() / scale + " " + "\n")
    }

    fun writevertexcolor(v1: Int, v2: Int, v3: Int, c: Int) {
        if (objt != objcount) {
            objt = objcount
            tfile.write(
                """
    o obj${objcount}
    
    """.trimIndent()
            )
        }
        ++vertexcountc
        val color = rs2hsbToColor(c)
        tfile.write("v " + v1.toFloat() / scale + " " + (-v2).toFloat() / scale + " " + (-v3).toFloat() / scale + " " + (color.red.toFloat() / 255.0f).toDouble() + " " + (color.green.toFloat() / 255.0f).toDouble() + " " + (color.blue.toFloat() / 255.0f).toDouble() + "\n")
    }

    fun writetexture(v1: Float, v2: Float, v3: Float, c: Float) {
        mfile.write(
            """vt $v2 ${1f - v3}
"""
        )
    }

    fun writecolour(col: Int) {
        val color = rs2hsbToColor(col)
        colourfile.write(
            """  Kd ${(color.red.toFloat() / 255.0f).toDouble()} ${(color.green.toFloat() / 255.0f).toDouble()} ${(color.blue.toFloat() / 255.0f).toDouble()}
"""
        )
    }

    fun write3facem() {
        mfile.write(
            """f ${vertexcountt - 2}/${vertexcountt - 2} ${vertexcountt - 1}/${vertexcountt - 1} ${vertexcountt}/${vertexcountt}
"""
        )
    }

    fun write3facet() {
        tfile.write(
            """f ${vertexcountc - 2} ${vertexcountc - 1} ${vertexcountc}
"""
        )
    }

    fun upload(tile: TilePaint) {
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
                if (materials == 1) {
                    writevertex(
                        vertexAx + tile.computeObj.x,
                        neHeight + tile.computeObj.y,
                        vertexAy + tile.computeObj.z,
                        neColor
                    )
                    writevertex(
                        vertexBx + tile.computeObj.x,
                        nwHeight + tile.computeObj.y,
                        vertexBy + tile.computeObj.z,
                        nwColor
                    )
                    writevertex(
                        vertexCx + tile.computeObj.x,
                        seHeight + tile.computeObj.y,
                        vertexCy + tile.computeObj.z,
                        seColor
                    )
                    writetexture(tex, 1.0f, 1.0f, 0.0f)
                    writetexture(tex, 0.0f, 1.0f, 0.0f)
                    writetexture(tex, 1.0f, 0.0f, 0.0f)
                    mfile.write(
                        """
    usemtl t${(tex - 1f).toInt()}
    
    """.trimIndent()
                    )
                    write3facem()
                    writevertex(
                        vertexDx + tile.computeObj.x,
                        swHeight + tile.computeObj.y,
                        vertexDy + tile.computeObj.z,
                        swColor
                    )
                    writevertex(
                        vertexCx + tile.computeObj.x,
                        seHeight + tile.computeObj.y,
                        vertexCy + tile.computeObj.z,
                        seColor
                    )
                    writevertex(
                        vertexBx + tile.computeObj.x,
                        nwHeight + tile.computeObj.y,
                        vertexBy + tile.computeObj.z,
                        nwColor
                    )
                    writetexture(tex, 0.0f, 0.0f, 0.0f)
                    writetexture(tex, 1.0f, 0.0f, 0.0f)
                    writetexture(tex, 0.0f, 1.0f, 0.0f)
                    mfile.write(
                        """
    usemtl t${(tex - 1f).toInt()}
    
    """.trimIndent()
                    )
                    write3facem()
                }
            } else if (textures == 1) {
                writevertexcolor(
                    vertexAx + tile.computeObj.x,
                    neHeight + tile.computeObj.y,
                    vertexAy + tile.computeObj.z,
                    neColor
                )
                writevertexcolor(
                    vertexBx + tile.computeObj.x,
                    nwHeight + tile.computeObj.y,
                    vertexBy + tile.computeObj.z,
                    nwColor
                )
                writevertexcolor(
                    vertexCx + tile.computeObj.x,
                    seHeight + tile.computeObj.y,
                    vertexCy + tile.computeObj.z,
                    seColor
                )
                write3facet()
                writevertexcolor(
                    vertexDx + tile.computeObj.x,
                    swHeight + tile.computeObj.y,
                    vertexDy + tile.computeObj.z,
                    swColor
                )
                writevertexcolor(
                    vertexCx + tile.computeObj.x,
                    seHeight + tile.computeObj.y,
                    vertexCy + tile.computeObj.z,
                    seColor
                )
                writevertexcolor(
                    vertexBx + tile.computeObj.x,
                    nwHeight + tile.computeObj.y,
                    vertexBy + tile.computeObj.z,
                    nwColor
                )
                write3facet()
            }
        }
    }

    fun upload(tileModel: TileModel) {
        Intrinsics.checkParameterIsNotNull(tileModel, "tileModel")
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
                if (triangleTextures != null) {
                    if (triangleTextures[i] != -1) {
                        val tex = triangleTextures[i].toFloat() + 1.0f
                        if (materials == 1) {
                            writevertex(
                                vertexXA + tileModel.computeObj.x,
                                vertexY[triangleA] + tileModel.computeObj.y,
                                vertexZA + tileModel.computeObj.z,
                                colorA
                            )
                            writevertex(
                                vertexXB + tileModel.computeObj.x,
                                vertexY[triangleB] + tileModel.computeObj.y,
                                vertexZB + tileModel.computeObj.z,
                                colorB
                            )
                            writevertex(
                                vertexXC + tileModel.computeObj.x,
                                vertexY[triangleC] + tileModel.computeObj.y,
                                vertexZC + tileModel.computeObj.z,
                                colorC
                            )
                            writetexture(
                                tex,
                                vertexXA.toFloat() / 128.0f,
                                vertexZA.toFloat() / 128.0f,
                                0.0f
                            )
                            writetexture(
                                tex,
                                vertexXB.toFloat() / 128.0f,
                                vertexZB.toFloat() / 128.0f,
                                0.0f
                            )
                            writetexture(
                                tex,
                                vertexXC.toFloat() / 128.0f,
                                vertexZC.toFloat() / 128.0f,
                                0.0f
                            )
                            mfile.write(
                                """
    usemtl t${(tex - 1f).toInt()}
    
    """.trimIndent()
                            )
                            write3facem()
                        }
                    } else if (textures == 1) {
                        writevertexcolor(
                            vertexXA + tileModel.computeObj.x,
                            vertexY[triangleA] + tileModel.computeObj.y,
                            vertexZA + tileModel.computeObj.z,
                            colorA
                        )
                        writevertexcolor(
                            vertexXB + tileModel.computeObj.x,
                            vertexY[triangleB] + tileModel.computeObj.y,
                            vertexZB + tileModel.computeObj.z,
                            colorB
                        )
                        writevertexcolor(
                            vertexXC + tileModel.computeObj.x,
                            vertexY[triangleC] + tileModel.computeObj.y,
                            vertexZC + tileModel.computeObj.z,
                            colorC
                        )
                        write3facet()
                    }
                } else if (textures == 1) {
                    writevertexcolor(
                        vertexXA + tileModel.computeObj.x,
                        vertexY[triangleA] + tileModel.computeObj.y,
                        vertexZA + tileModel.computeObj.z,
                        colorA
                    )
                    writevertexcolor(
                        vertexXB + tileModel.computeObj.x,
                        vertexY[triangleB] + tileModel.computeObj.y,
                        vertexZB + tileModel.computeObj.z,
                        colorB
                    )
                    writevertexcolor(
                        vertexXC + tileModel.computeObj.x,
                        vertexY[triangleC] + tileModel.computeObj.y,
                        vertexZC + tileModel.computeObj.z,
                        colorC
                    )
                    write3facet()
                }
            }
        }
    }

    private fun uploadModel(entity: Entity) {
        val model = entity.getModel()
        if (model.computeObj.offset >= 0) {
        }
        val triangleCount = model.modelDefinition.faceCount
        for (i in 0 until triangleCount) {
            pushFace(model, i)
        }
    }

    private fun pushFace(model: Model, face: Int) {
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
                        if (materials == 1) {
                            var a = vertexX[triangleA]
                            var b = vertexY[triangleA]
                            var c = vertexZ[triangleA]
                            writevertex(
                                a + model.computeObj.x,
                                b + model.computeObj.y,
                                c + model.computeObj.z,
                                alpha or priority or color1
                            )
                            a = vertexX[triangleB]
                            b = vertexY[triangleB]
                            c = vertexZ[triangleB]
                            writevertex(
                                a + model.computeObj.x,
                                b + model.computeObj.y,
                                c + model.computeObj.z,
                                alpha or priority or color2
                            )
                            a = vertexX[triangleC]
                            b = vertexY[triangleC]
                            c = vertexZ[triangleC]
                            writevertex(
                                a + model.computeObj.x,
                                b + model.computeObj.y,
                                c + model.computeObj.z,
                                alpha or priority or color3
                            )
                            var var10002 = uf!![0]
                            writetexture(texture, var10002, vf!![0], 0.0f)
                            var10002 = uf[1]
                            writetexture(texture, var10002, vf[1], 0.0f)
                            var10002 = uf[2]
                            writetexture(texture, var10002, vf[2], 0.0f)
                            mfile.write(
                                """
    usemtl t${(texture - 1f).toInt()}
    
    """.trimIndent()
                            )
                            write3facem()
                        }
                        return
                    }
                }
            }
            if (textures == 1) {
                var a = vertexX[triangleA]
                var b = vertexY[triangleA]
                var c = vertexZ[triangleA]
                writevertexcolor(
                    a + model.computeObj.x,
                    b + model.computeObj.y,
                    c + model.computeObj.z,
                    alpha or priority or color1
                )
                a = vertexX[triangleB]
                b = vertexY[triangleB]
                c = vertexZ[triangleB]
                writevertexcolor(
                    a + model.computeObj.x,
                    b + model.computeObj.y,
                    c + model.computeObj.z,
                    alpha or priority or color2
                )
                a = vertexX[triangleC]
                b = vertexY[triangleC]
                c = vertexZ[triangleC]
                writevertexcolor(
                    a + model.computeObj.x,
                    b + model.computeObj.y,
                    c + model.computeObj.z,
                    alpha or priority or color3
                )
                write3facet()
            }
        } else if (textures == 1) {
            var a = vertexX[triangleA]
            var b = vertexY[triangleA]
            var c = vertexZ[triangleA]
            writevertexcolor(
                a + model.computeObj.x,
                b + model.computeObj.y,
                c + model.computeObj.z,
                alpha or priority or color1
            )
            a = vertexX[triangleB]
            b = vertexY[triangleB]
            c = vertexZ[triangleB]
            writevertexcolor(
                a + model.computeObj.x,
                b + model.computeObj.y,
                c + model.computeObj.z,
                alpha or priority or color2
            )
            a = vertexX[triangleC]
            b = vertexY[triangleC]
            c = vertexZ[triangleC]
            writevertexcolor(
                a + model.computeObj.x,
                b + model.computeObj.y,
                c + model.computeObj.z,
                alpha or priority or color3
            )
            write3facet()
        }
    }

    init {
        tfile = File("./OBJ/temp.obj").printWriter()
        mfile = File("./OBJ/materials.obj").printWriter()
        colourfile = File("./OBJ/col.mtl").printWriter()
        objt = -1
        objm = -1
        scale = 100.0f
        materials = 1
        textures = 1
    }
}