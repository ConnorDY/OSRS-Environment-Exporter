package controllers.worldRenderer.entities

import controllers.worldRenderer.Constants
import controllers.worldRenderer.components.*
import controllers.worldRenderer.helpers.GpuIntBuffer
import controllers.worldRenderer.helpers.ModelBuffers
import utils.Observable

class TileModel(
    overlayPath: Int,
    overlayRotation: Int,
    overlayTexture: Int,
    x: Int,
    y: Int,
    var6: Int,
    var7: Int,
    var8: Int,
    var9: Int,
    var10: Int,
    var11: Int,
    var12: Int,
    var13: Int,
    var14: Int,
    var15: Int,
    var16: Int,
    var17: Int,
    var18: Int,
    var19: Int,
    hoverComponent: HoverComponent = HoverComponent(),
    selectComponent: SelectComponent = SelectComponent(),
    clickableComponent: ClickableComponent = ClickableComponent()
) : Observable<TileModel>(),
    Renderable,
    Clickable by clickableComponent,
    Hoverable by hoverComponent,
    Selectable by selectComponent {

    var computeObj: ComputeObj = ComputeObj()

    var vertexX: IntArray
    var vertexY: IntArray
    var vertexZ: IntArray
    var triangleColorA: IntArray = intArrayOf()
        get() {
            if (isSelected) {
                return IntArray(faceCount) { Constants.SELECTED_HSL }
            }
            if (isHovered) {
                return IntArray(faceCount) { Constants.HOVER_HSL }
            }
            return field
        }

    var triangleColorB: IntArray = intArrayOf()
        get() {
            if (isSelected) {
                return IntArray(faceCount) { Constants.SELECTED_HSL }
            }
            if (isHovered) {
                return IntArray(faceCount) { Constants.HOVER_HSL }
            }
            return field
        }

    var triangleColorC: IntArray = intArrayOf()
        get() {
            if (isSelected) {
                return IntArray(faceCount) { Constants.SELECTED_HSL }
            }
            if (isHovered) {
                return IntArray(faceCount) { Constants.HOVER_HSL }
            }
            return field
        }

    var faceX: IntArray
    var faceY: IntArray
    var faceZ: IntArray
    var faceCount: Int = -1
    var triangleTextureId: IntArray? = null
    var isFlat = true
    var shape: Int
    var rotation: Int
    var underlayRgb: Int
    var overlayRgb: Int

    override fun draw(modelBuffers: ModelBuffers, sceneX: Int, sceneY: Int, height: Int, objType: Int) {
        val x: Int = sceneX * Constants.LOCAL_TILE_SIZE
        val z: Int = sceneY * Constants.LOCAL_TILE_SIZE

        val b: GpuIntBuffer = modelBuffers.modelBufferUnordered
        modelBuffers.incUnorderedModels()
        b.ensureCapacity(13)

        computeObj.idx = modelBuffers.targetBufferOffset
        computeObj.flags = ModelBuffers.FLAG_SCENE_BUFFER
        computeObj.x = x
        computeObj.z = z
        computeObj.pickerId = modelBuffers.calcPickerId(sceneX, sceneY, objType)
        b.buffer.put(computeObj.toArray())

        modelBuffers.addTargetBufferOffset(computeObj.size * 3)
    }

    override fun drawDynamic(modelBuffers: ModelBuffers, sceneX: Int, sceneY: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun clearDraw(modelBuffers: ModelBuffers) {
//        TODO("Not yet implemented")
    }

    init {
        hoverComponent.observable = this
        selectComponent.observable = this
        clickableComponent.onClickFunc = {
            isSelected = true
        }

        if (var7 != var6 || var8 != var6 || var9 != var6) {
            isFlat = false
        }
        shape = overlayPath
        rotation = overlayRotation
        underlayRgb = var18
        overlayRgb = var19
        val var20: Short = 128
        val var21 = var20 / 2
        val var22 = var20 / 4
        val var23 = var20 * 3 / 4
        val var24 = field1617[overlayPath]
        val var25 = var24.size
        vertexX = IntArray(var25)
        vertexY = IntArray(var25)
        vertexZ = IntArray(var25)
        val var26 = IntArray(var25)
        val var27 = IntArray(var25)
        val var28 = var20 * x
        val var29 = y * var20
        var var31: Int
        var var32: Int
        var var33: Int
        var var34: Int
        var var35: Int
        var var36: Int
        for (var30 in 0 until var25) {
            var31 = var24[var30]
            if (var31 and 1 == 0 && var31 <= 8) {
                var31 = (var31 - overlayRotation - overlayRotation - 1 and 7) + 1
            }
            if (var31 > 8 && var31 <= 12) {
                var31 = (var31 - 9 - overlayRotation and 3) + 9
            }
            if (var31 > 12 && var31 <= 16) {
                var31 = (var31 - 13 - overlayRotation and 3) + 13
            }

            if (var31 == 1) {
                var32 = var28
                var33 = var29
                var34 = var6
                var35 = var10
                var36 = var14
            } else if (var31 == 2) {
                var32 = var28 + var21
                var33 = var29
                var34 = var7 + var6 shr 1
                var35 = var11 + var10 shr 1
                var36 = var15 + var14 shr 1
            } else if (var31 == 3) {
                var32 = var28 + var20
                var33 = var29
                var34 = var7
                var35 = var11
                var36 = var15
            } else if (var31 == 4) {
                var32 = var28 + var20
                var33 = var29 + var21
                var34 = var8 + var7 shr 1
                var35 = var11 + var12 shr 1
                var36 = var15 + var16 shr 1
            } else if (var31 == 5) {
                var32 = var28 + var20
                var33 = var29 + var20
                var34 = var8
                var35 = var12
                var36 = var16
            } else if (var31 == 6) {
                var32 = var28 + var21
                var33 = var29 + var20
                var34 = var8 + var9 shr 1
                var35 = var13 + var12 shr 1
                var36 = var17 + var16 shr 1
            } else if (var31 == 7) {
                var32 = var28
                var33 = var29 + var20
                var34 = var9
                var35 = var13
                var36 = var17
            } else if (var31 == 8) {
                var32 = var28
                var33 = var29 + var21
                var34 = var9 + var6 shr 1
                var35 = var13 + var10 shr 1
                var36 = var17 + var14 shr 1
            } else if (var31 == 9) {
                var32 = var28 + var21
                var33 = var29 + var22
                var34 = var7 + var6 shr 1
                var35 = var11 + var10 shr 1
                var36 = var15 + var14 shr 1
            } else if (var31 == 10) {
                var32 = var28 + var23
                var33 = var29 + var21
                var34 = var8 + var7 shr 1
                var35 = var11 + var12 shr 1
                var36 = var15 + var16 shr 1
            } else if (var31 == 11) {
                var32 = var28 + var21
                var33 = var29 + var23
                var34 = var8 + var9 shr 1
                var35 = var13 + var12 shr 1
                var36 = var17 + var16 shr 1
            } else if (var31 == 12) {
                var32 = var28 + var22
                var33 = var29 + var21
                var34 = var9 + var6 shr 1
                var35 = var13 + var10 shr 1
                var36 = var17 + var14 shr 1
            } else if (var31 == 13) {
                var32 = var28 + var22
                var33 = var29 + var22
                var34 = var6
                var35 = var10
                var36 = var14
            } else if (var31 == 14) {
                var32 = var28 + var23
                var33 = var29 + var22
                var34 = var7
                var35 = var11
                var36 = var15
            } else if (var31 == 15) {
                var32 = var28 + var23
                var33 = var29 + var23
                var34 = var8
                var35 = var12
                var36 = var16
            } else {
                var32 = var28 + var22
                var33 = var29 + var23
                var34 = var9
                var35 = var13
                var36 = var17
            }
            vertexX[var30] = var32 - x * Constants.LOCAL_TILE_SIZE
            vertexY[var30] = var34
            vertexZ[var30] = var33 - y * Constants.LOCAL_TILE_SIZE
            var26[var30] = var35
            var27[var30] = var36
        }
        val var38 = field1626[overlayPath]
        var31 = var38.size / 4
        faceX = IntArray(var31)
        faceY = IntArray(var31)
        faceZ = IntArray(var31)
        triangleColorA = IntArray(var31)
        triangleColorB = IntArray(var31)
        triangleColorC = IntArray(var31)
        faceCount = var31
        if (overlayTexture != -1) {
            triangleTextureId = IntArray(var31)
        }
        var32 = 0
        var33 = 0
        while (var33 < var31) {
            var34 = var38[var32]
            var35 = var38[var32 + 1]
            var36 = var38[var32 + 2]
            var var37 = var38[var32 + 3]
            var32 += 4
            if (var35 < 4) {
                var35 = var35 - overlayRotation and 3
            }
            if (var36 < 4) {
                var36 = var36 - overlayRotation and 3
            }
            if (var37 < 4) {
                var37 = var37 - overlayRotation and 3
            }
            faceX[var33] = var35
            faceY[var33] = var36
            faceZ[var33] = var37
            if (var34 == 0) {
                triangleColorA[var33] = var26[var35]
                triangleColorB[var33] = var26[var36]
                triangleColorC[var33] = var26[var37]
                if (triangleTextureId != null) {
                    triangleTextureId!![var33] = -1
                }
            } else {
                triangleColorA[var33] = var27[var35]
                triangleColorB[var33] = var27[var36]
                triangleColorC[var33] = var27[var37]
                if (triangleTextureId != null) {
                    triangleTextureId!![var33] = overlayTexture
                }
            }
            ++var33
        }
        var33 = var6
        var34 = var7
        if (var7 < var6) {
            var33 = var7
        }
        if (var7 > var7) {
            var34 = var7
        }
        if (var8 < var33) {
            var33 = var8
        }
        if (var8 > var34) {
            var34 = var8
        }
        if (var9 < var33) {
            var33 = var9
        }
        if (var9 > var34) {
            var34 = var9
        }
        var33 /= 14
        var34 /= 14
    }

    companion object {
        var field1615: IntArray
        var field1605: IntArray
        var field1613: IntArray
        var field1623: IntArray
        var field1620: IntArray
        val field1617: Array<IntArray>
        val field1626: Array<IntArray>

        init {
            field1615 = IntArray(6)
            field1605 = IntArray(6)
            field1613 = IntArray(6)
            field1623 = IntArray(6)
            field1620 = IntArray(6)
            field1617 = arrayOf(
                intArrayOf(1, 3, 5, 7),
                intArrayOf(1, 3, 5, 7),
                intArrayOf(1, 3, 5, 7),
                intArrayOf(1, 3, 5, 7, 6),
                intArrayOf(1, 3, 5, 7, 6),
                intArrayOf(1, 3, 5, 7, 6),
                intArrayOf(1, 3, 5, 7, 6),
                intArrayOf(1, 3, 5, 7, 2, 6),
                intArrayOf(1, 3, 5, 7, 2, 8),
                intArrayOf(1, 3, 5, 7, 2, 8),
                intArrayOf(1, 3, 5, 7, 11, 12),
                intArrayOf(1, 3, 5, 7, 11, 12),
                intArrayOf(1, 3, 5, 7, 13, 14)
            )
            field1626 = arrayOf(
                intArrayOf(0, 1, 2, 3, 0, 0, 1, 3),
                intArrayOf(1, 1, 2, 3, 1, 0, 1, 3),
                intArrayOf(0, 1, 2, 3, 1, 0, 1, 3),
                intArrayOf(0, 0, 1, 2, 0, 0, 2, 4, 1, 0, 4, 3),
                intArrayOf(0, 0, 1, 4, 0, 0, 4, 3, 1, 1, 2, 4),
                intArrayOf(0, 0, 4, 3, 1, 0, 1, 2, 1, 0, 2, 4),
                intArrayOf(0, 1, 2, 4, 1, 0, 1, 4, 1, 0, 4, 3),
                intArrayOf(0, 4, 1, 2, 0, 4, 2, 5, 1, 0, 4, 5, 1, 0, 5, 3),
                intArrayOf(0, 4, 1, 2, 0, 4, 2, 3, 0, 4, 3, 5, 1, 0, 4, 5),
                intArrayOf(0, 0, 4, 5, 1, 4, 1, 2, 1, 4, 2, 3, 1, 4, 3, 5),
                intArrayOf(0, 0, 1, 5, 0, 1, 4, 5, 0, 1, 2, 4, 1, 0, 5, 3, 1, 5, 4, 3, 1, 4, 2, 3),
                intArrayOf(1, 0, 1, 5, 1, 1, 4, 5, 1, 1, 2, 4, 0, 0, 5, 3, 0, 5, 4, 3, 0, 4, 2, 3),
                intArrayOf(1, 0, 5, 4, 1, 0, 1, 5, 0, 0, 4, 3, 0, 4, 5, 3, 0, 5, 2, 3, 0, 1, 2, 5)
            )
        }
    }
}
