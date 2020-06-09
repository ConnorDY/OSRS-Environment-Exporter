package utils

import cache.definitions.ModelDefinition
import cache.definitions.TextureDefinition
import cache.loaders.SpriteLoader
import cache.loaders.TextureLoader
import cache.utils.ColorPalette
import javafx.scene.image.WritableImage
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.MeshView
import javafx.scene.shape.TriangleMesh
import java.awt.Color

object JavaFxHelpers {
    fun modelToMeshViews(
        md: ModelDefinition,
        textureLoader: TextureLoader,
        spriteLoader: SpriteLoader
    ): Array<MeshView?> {
        val meshViews =
            arrayOfNulls<MeshView>(md.faceCount)
        for (i in 0 until md.faceCount) {
            val mesh = TriangleMesh()
            val faceA: Int = md.faceVertexIndices1!![i]
            val faceB: Int = md.faceVertexIndices2!![i]
            val faceC: Int = md.faceVertexIndices3!![i]
            mesh.points
                .addAll(
                    md.vertexPositionsX[faceA].toFloat(),
                    md.vertexPositionsY[faceA].toFloat(),
                    md.vertexPositionsZ[faceA].toFloat()
                )
            mesh.points
                .addAll(
                    md.vertexPositionsX[faceB].toFloat(),
                    md.vertexPositionsY[faceB].toFloat(),
                    md.vertexPositionsZ[faceB].toFloat()
                )
            mesh.points
                .addAll(
                    md.vertexPositionsX[faceC].toFloat(),
                    md.vertexPositionsY[faceC].toFloat(),
                    md.vertexPositionsZ[faceC].toFloat()
                )

//            mesh.getNormals().addAll(md.vertexNormals[faceA].x, md.vertexNormals[faceA].y, md.vertexNormals[faceA].z);
//            mesh.getNormals().addAll(md.vertexNormals[faceB].x, md.vertexNormals[faceB].y, md.vertexNormals[faceB].z);
//            mesh.getNormals().addAll(md.vertexNormals[faceC].x, md.vertexNormals[faceC].y, md.vertexNormals[faceC].z);
            if (md.faceTextureVCoordinates != null && md.faceTextureUCoordinates != null && md.faceTextureUCoordinates!![i] != null && md.faceTextureVCoordinates!![i] != null) {
                mesh.texCoords
                    .addAll(md.faceTextureUCoordinates!![i]!![0], md.faceTextureVCoordinates!![i]!!.get(0))
                mesh.texCoords
                    .addAll(md.faceTextureUCoordinates!![i]!![1], md.faceTextureVCoordinates!![i]!![1])
                mesh.texCoords
                    .addAll(md.faceTextureUCoordinates!![i]!![2], md.faceTextureVCoordinates!![i]!![2])
            } else {
                mesh.texCoords.addAll(0f, 0f, 1f, 0f, 0f, 1f)
            }
            mesh.faces.addAll(0, 0, 1, 1, 2, 2)
            val mat = PhongMaterial()
            val textureId = if (md.faceTextures != null) md.faceTextures!![i] else -1
            if (textureId.toInt() == -1) {
                val color: Color = ColorPalette.rs2hsbToColor(md.faceColors!![i].toInt())
                val alpha = if (md.faceAlphas != null) 1 - (md.faceAlphas!![i].toInt() and 0xFF) / 255f else 1f
                val c = javafx.scene.paint.Color(
                    (color.red / 255f).toDouble(),
                    (color.green / 255f).toDouble(),
                    (color.blue / 255f).toDouble(),
                    alpha.toDouble()
                )
                mat.diffuseColor = c
            } else {
                val texture: TextureDefinition? = textureLoader.get(textureId.toInt())
                if (texture != null) {
                    texture.loadPixels(0.8, 128, spriteLoader)
                    val writableImage = WritableImage(128, 128)
                    for (x in 0..127) {
                        for (y in 0..127) {
                            var argb = 0xFF shl 24 or texture.pixels[x + y * 128] //argb, full alpha first bits
                            if (texture.pixels[x + y * 128] == 0) {
                                argb = 0
                            }
                            writableImage.pixelWriter.setArgb(x, y, argb)
                        }
                    }
                    mat.diffuseMap = writableImage
                }
            }
            val mv = MeshView()
            mv.mesh = mesh
            mv.material = mat
            meshViews[i] = mv
        }
        return meshViews
    }

    // TODO: Works with osrs model format of using a material for every face
    // does not work with real objs
//    fun objToModelDefinition(obj: Obj, mtlList: List<Mtl>): ModelDefinition {
//        val m = ModelDefinition()
//        m.vertexCount = obj.getNumVertices()
//        m.faceCount = obj.getNumFaces()
//        m.vertexPositionsX = IntArray(m.vertexCount)
//        m.vertexPositionsY = IntArray(m.vertexCount)
//        m.vertexPositionsZ = IntArray(m.vertexCount)
//        for (i in 0 until m.vertexCount) {
//            m.vertexPositionsX.get(i) = obj.getVertex(i).getX()
//            m.vertexPositionsY.get(i) = -obj.getVertex(i).getY()
//            m.vertexPositionsZ.get(i) = -obj.getVertex(i).getZ()
//        }
//        m.faceVertexIndices1 = IntArray(m.faceCount)
//        m.faceVertexIndices2 = IntArray(m.faceCount)
//        m.faceVertexIndices3 = IntArray(m.faceCount)
//        m.faceColors = ShortArray(m.faceCount)
//        for (i in 0 until m.faceCount) {
//            m.faceVertexIndices1.get(i) = obj.getFace(i).getVertexIndex(0)
//            m.faceVertexIndices2.get(i) = obj.getFace(i).getVertexIndex(1)
//            m.faceVertexIndices3.get(i) = obj.getFace(i).getVertexIndex(2)
//            val mtl: Mtl = mtlList[i]
//            val hsbVals = Color.RGBtoHSB(
//                (mtl.getKd().getX() * 255f) as Int,
//                (mtl.getKd().getY() * 255f) as Int,
//                (mtl.getKd().getZ() * 255f) as Int,
//                null
//            )
//            val encodeHue = (hsbVals[0] * 63).toInt() and 0x3f shl 10
//            val encodeSat = (hsbVals[1] * 7).toInt() and 0x07 shl 7
//            val encodeBri = (hsbVals[2] * 127).toInt() and 0x7f
//            val hsbCol = (encodeHue or encodeSat or encodeBri + 1).toShort()
//            m.faceColors.get(i) = hsbCol
//        }
//        return m
//    }
}