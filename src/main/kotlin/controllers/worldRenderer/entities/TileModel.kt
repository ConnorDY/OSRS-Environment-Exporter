package controllers.worldRenderer.entities

import controllers.worldRenderer.Constants
import controllers.worldRenderer.components.*
import controllers.worldRenderer.helpers.GpuIntBuffer
import controllers.worldRenderer.helpers.ModelBuffers
import utils.EventType
import utils.Observable

class TileModel(
    public var overlayPath: Int = 0,
    public var overlayRotation: Int = 0,
    public var overlayTexture: Int,
    public val x: Int,
    public val y: Int,
    public var swHeight: Int,
    public var seHeight: Int,
    public var neHeight: Int,
    public var nwHeight: Int,
    public var swColor: Int,
    public var seColor: Int,
    public var neColor: Int,
    public var nwColor: Int,
    public var swColorB: Int,
    public var seColorB: Int,
    public var neColorB: Int,
    public var nwColorB: Int,
    public val underlayRgb: Int = 0,
    public val overlayRgb: Int = 0,
    hoverComponent: HoverComponent = HoverComponent(),
    selectComponent: SelectComponent = SelectComponent(),
    clickableComponent: ClickableComponent = ClickableComponent()
) : Observable<TileModel>(),
    Renderable,
    Clickable by clickableComponent,
    Hoverable by hoverComponent,
    Selectable by selectComponent {

    var computeObj: ComputeObj = ComputeObj()

    lateinit var vertexX: IntArray
    lateinit var vertexY: IntArray
    lateinit var vertexZ: IntArray
    lateinit var faceX: IntArray
    lateinit var faceY: IntArray
    lateinit var faceZ: IntArray

    fun setHeight(swHeight: Int, seHeight: Int, neHeight: Int, nwHeight: Int) {
        this.swHeight = swHeight
        this.seHeight = seHeight
        this.neHeight = neHeight
        this.nwHeight = nwHeight
        calculateVertexs()
        notifyObservers(EventType.SELECT)
    }

    fun setColor(swColor: Int, seColor: Int, neColor: Int, nwColor: Int) {
        this.swColor = swColor
        this.seColor = seColor
        this.neColor = neColor
        this.nwColor = nwColor
        calculateVertexs()
        notifyObservers(EventType.SELECT)
    }

    fun setBrightnessMaybe(swColor: Int, seColor: Int, neColor: Int, nwColor: Int) {
        this.swColorB = swColor
        this.seColorB = seColor
        this.neColorB = neColor
        this.nwColorB = nwColor
        calculateVertexs()
        notifyObservers(EventType.SELECT)
    }

    var triangleColorA: IntArray = intArrayOf()
        private set
        get() {
            if (isSelected) {
                return IntArray(faceCount) { Constants.SELECTED_HSL }
            }
            if (isHovered) {
                return field.map { it + Constants.HOVER_HSL_ALPHA }.toIntArray()
            }
            return field
        }

    var triangleColorB: IntArray = intArrayOf()
        private set
        get() {
            if (isSelected) {
                return IntArray(faceCount) { Constants.SELECTED_HSL }
            }
            if (isHovered) {
                return field.map { it + Constants.HOVER_HSL_ALPHA }.toIntArray()
            }
            return field
        }

    var triangleColorC: IntArray = intArrayOf()
        private set
        get() {
            if (isSelected) {
                return IntArray(faceCount) { Constants.SELECTED_HSL }
            }
            if (isHovered) {
                return field.map { it + Constants.HOVER_HSL_ALPHA }.toIntArray()
            }
            return field
        }


    var faceCount: Int = -1
    var triangleTextureId: IntArray? = null
    var isFlat = true

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

    fun recompute(modelBuffers: ModelBuffers) {
        val b: GpuIntBuffer = modelBuffers.modelBufferUnordered
        modelBuffers.incUnorderedModels()
        b.ensureCapacity(13)
        computeObj.flags = 0

        b.buffer.put(computeObj.toArray())
    }

    init {
        hoverComponent.observable = this
        selectComponent.observable = this
        clickableComponent.onClickFunc = {
            isSelected = true
        }

        this.calculateVertexs()
    }

    fun calculateVertexs() {
        if (seHeight != swHeight || neHeight != swHeight || nwHeight != swHeight) {
            isFlat = false
        }
        val size: Short = 128
        val sizeHalf = size / 2
        val sizeFourth = size / 4
        val sizeThreeFourths = size / 4 * 3
        val rotationShape = rotationShapes[overlayPath]
        val rotationShapeSize = rotationShape.size
        val pathShape = pathShapes[overlayPath]
        faceCount = pathShape.size / 4
        faceX = IntArray(faceCount)
        faceY = IntArray(faceCount)
        faceZ = IntArray(faceCount)
        vertexX = IntArray(rotationShapeSize)
        vertexY = IntArray(rotationShapeSize)
        vertexZ = IntArray(rotationShapeSize)
        val vertexColors = IntArray(rotationShapeSize)
        val vertexColorsB = IntArray(rotationShapeSize)
        val sizeX = size * x
        val sizeY = size * y
        var vertX: Int
        var vertY: Int
        var vertZ: Int
        var vertexColor: Int
        var vertexColorB: Int
        for (rotationIndex in 0 until rotationShapeSize) {
            var rotationShapeValue: Int = rotationShape[rotationIndex]
            if (rotationShapeValue and 1 == 0 && rotationShapeValue <= 8) {
                rotationShapeValue = (rotationShapeValue - overlayRotation - overlayRotation - 1 and 7) + 1
            }
            if (rotationShapeValue in 9..12) {
                rotationShapeValue = (rotationShapeValue - 9 - overlayRotation and 3) + 9
            }
            if (rotationShapeValue in 13..16) {
                rotationShapeValue = (rotationShapeValue - 13 - overlayRotation and 3) + 13
            }

            if (rotationShapeValue == 1) {
                vertX = sizeX
                vertY = sizeY
                vertZ = swHeight
                vertexColor = swColor
                vertexColorB = swColorB
            } else if (rotationShapeValue == 2) {
                vertX = sizeX + sizeHalf
                vertY = sizeY
                vertZ = seHeight + swHeight shr 1
                vertexColor = seColor + swColor shr 1
                vertexColorB = seColorB + swColorB shr 1
            } else if (rotationShapeValue == 3) {
                vertX = sizeX + size
                vertY = sizeY
                vertZ = seHeight
                vertexColor = seColor
                vertexColorB = seColorB
            } else if (rotationShapeValue == 4) {
                vertX = sizeX + size
                vertY = sizeY + sizeHalf
                vertZ = neHeight + seHeight shr 1
                vertexColor = seColor + neColor shr 1
                vertexColorB = seColorB + neColorB shr 1
            } else if (rotationShapeValue == 5) {
                vertX = sizeX + size
                vertY = sizeY + size
                vertZ = neHeight
                vertexColor = neColor
                vertexColorB = neColorB
            } else if (rotationShapeValue == 6) {
                vertX = sizeX + sizeHalf
                vertY = sizeY + size
                vertZ = neHeight + nwHeight shr 1
                vertexColor = nwColor + neColor shr 1
                vertexColorB = nwColorB + neColorB shr 1
            } else if (rotationShapeValue == 7) {
                vertX = sizeX
                vertY = sizeY + size
                vertZ = nwHeight
                vertexColor = nwColor
                vertexColorB = nwColorB
            } else if (rotationShapeValue == 8) {
                vertX = sizeX
                vertY = sizeY + sizeHalf
                vertZ = nwHeight + swHeight shr 1
                vertexColor = nwColor + swColor shr 1
                vertexColorB = nwColorB + swColorB shr 1
            } else if (rotationShapeValue == 9) {
                vertX = sizeX + sizeHalf
                vertY = sizeY + sizeFourth
                vertZ = seHeight + swHeight shr 1
                vertexColor = seColor + swColor shr 1
                vertexColorB = seColorB + swColorB shr 1
            } else if (rotationShapeValue == 10) {
                vertX = sizeX + sizeThreeFourths
                vertY = sizeY + sizeHalf
                vertZ = neHeight + seHeight shr 1
                vertexColor = seColor + neColor shr 1
                vertexColorB = seColorB + neColorB shr 1
            } else if (rotationShapeValue == 11) {
                vertX = sizeX + sizeHalf
                vertY = sizeY + sizeThreeFourths
                vertZ = neHeight + nwHeight shr 1
                vertexColor = nwColor + neColor shr 1
                vertexColorB = nwColorB + neColorB shr 1
            } else if (rotationShapeValue == 12) {
                vertX = sizeX + sizeFourth
                vertY = sizeY + sizeHalf
                vertZ = nwHeight + swHeight shr 1
                vertexColor = nwColor + swColor shr 1
                vertexColorB = nwColorB + swColorB shr 1
            } else if (rotationShapeValue == 13) {
                vertX = sizeX + sizeFourth
                vertY = sizeY + sizeFourth
                vertZ = swHeight
                vertexColor = swColor
                vertexColorB = swColorB
            } else if (rotationShapeValue == 14) {
                vertX = sizeX + sizeThreeFourths
                vertY = sizeY + sizeFourth
                vertZ = seHeight
                vertexColor = seColor
                vertexColorB = seColorB
            } else if (rotationShapeValue == 15) {
                vertX = sizeX + sizeThreeFourths
                vertY = sizeY + sizeThreeFourths
                vertZ = neHeight
                vertexColor = neColor
                vertexColorB = neColorB
            } else {
                vertX = sizeX + sizeFourth
                vertY = sizeY + sizeThreeFourths
                vertZ = nwHeight
                vertexColor = nwColor
                vertexColorB = nwColorB
            }
            // X, Y, Z => X, Z, Y - because RuneScape has different axis :/
            vertexX[rotationIndex] = vertX - x * Constants.LOCAL_TILE_SIZE
            vertexY[rotationIndex] = vertZ
            vertexZ[rotationIndex] = vertY - y * Constants.LOCAL_TILE_SIZE
            vertexColors[rotationIndex] = vertexColor
            vertexColorsB[rotationIndex] = vertexColorB
        }
        if (triangleColorA.size != faceCount) {
            triangleColorA = IntArray(faceCount)
            triangleColorB = IntArray(faceCount)
            triangleColorC = IntArray(faceCount)
        }
        if (overlayTexture != -1) {
            triangleTextureId = IntArray(faceCount)
        }
        // var currentFace = 0 // can be rewritten to single var, by changing loop incrementer?
        var faceIndex = 0 // can be rewritten to single var, by changing loop incrementer?
        while (faceIndex < faceCount) {
            val currentFace = faceIndex * 4;
            val vc = pathShape[currentFace]
            var v1 = pathShape[currentFace + 1]
            var v2 = pathShape[currentFace + 2]
            var v3 = pathShape[currentFace + 3]
            if (v1 < 4) {
                v1 = v1 - overlayRotation and 3
            }
            if (v2 < 4) {
                v2 = v2 - overlayRotation and 3
            }
            if (v3 < 4) {
                v3 = v3 - overlayRotation and 3
            }
            faceX[faceIndex] = v1
            faceY[faceIndex] = v2
            faceZ[faceIndex] = v3
            if (vc == 0) { // Black color shape (mask?)
                triangleColorA[faceIndex] = vertexColors[v1]
                triangleColorB[faceIndex] = vertexColors[v2]
                triangleColorC[faceIndex] = vertexColors[v3]
                if (triangleTextureId != null) {
                    triangleTextureId!![faceIndex] = -1
                }
            } else { // White color shape (mask?)
                triangleColorA[faceIndex] = vertexColorsB[v1]
                triangleColorB[faceIndex] = vertexColorsB[v2]
                triangleColorC[faceIndex] = vertexColorsB[v3]
                if (triangleTextureId != null) {
                    triangleTextureId!![faceIndex] = overlayTexture
                }
            }
            // currentFace += 4 // can be rewritten to single var, by changing loop incrementer?
            ++faceIndex // can be rewritten to single var, by changing loop incrementer?
        }

        // Did next lines try to find smallest and biggest height or what?
        // var height1 = swHeight
        // var height2 = seHeight
        // if (seHeight < swHeight) {
        //     height1 = seHeight
        // }
        // if (seHeight > seHeight) {
        //     height2 = seHeight
        // }
        // if (neHeight < height1) {
        //     height1 = neHeight
        // }
        // if (neHeight > height2) {
        //     height2 = neHeight
        // }
        // if (nwHeight < height1) {
        //     height1 = nwHeight
        // }
        // if (nwHeight > height2) {
        //     height2 = nwHeight
        // }
        // height1 /= 14
        // height2 /= 14
    }

    companion object {
        val rotationShapes: Array<IntArray> = arrayOf(
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
        val pathShapes: Array<IntArray> = arrayOf(
            // 1. black (0) or white (1) shape, 2.,3.,4., - vertexes (https://i.imgur.com/FR83Q42.png)
            intArrayOf(0, 1, 2, 3,  0, 0, 1, 3),
            intArrayOf(1, 1, 2, 3,  1, 0, 1, 3),
            intArrayOf(0, 1, 2, 3,  1, 0, 1, 3),
            intArrayOf(0, 0, 1, 2,  0, 0, 2, 4,  1, 0, 4, 3),
            intArrayOf(0, 0, 1, 4,  0, 0, 4, 3,  1, 1, 2, 4),
            intArrayOf(0, 0, 4, 3,  1, 0, 1, 2,  1, 0, 2, 4),
            intArrayOf(0, 1, 2, 4,  1, 0, 1, 4,  1, 0, 4, 3),
            intArrayOf(0, 4, 1, 2,  0, 4, 2, 5,  1, 0, 4, 5,  1, 0, 5, 3),
            intArrayOf(0, 4, 1, 2,  0, 4, 2, 3,  0, 4, 3, 5,  1, 0, 4, 5),
            intArrayOf(0, 0, 4, 5,  1, 4, 1, 2,  1, 4, 2, 3,  1, 4, 3, 5),
            intArrayOf(0, 0, 1, 5,  0, 1, 4, 5,  0, 1, 2, 4,  1, 0, 5, 3,  1, 5, 4, 3,  1, 4, 2, 3),
            intArrayOf(1, 0, 1, 5,  1, 1, 4, 5,  1, 1, 2, 4,  0, 0, 5, 3,  0, 5, 4, 3,  0, 4, 2, 3),
            intArrayOf(1, 0, 5, 4,  1, 0, 1, 5,  0, 0, 4, 3,  0, 4, 5, 3,  0, 5, 2, 3,  0, 1, 2, 5)
        )

//        var field1615: IntArray
//        var field1605: IntArray
//        var field1613: IntArray
//        var field1623: IntArray
//        var field1620: IntArray
//        init {
//            field1615 = IntArray(6)
//            field1605 = IntArray(6)
//            field1613 = IntArray(6)
//            field1623 = IntArray(6)
//            field1620 = IntArray(6)
//        }
    }

    override fun toString(): String {
        return "overlayPath: $overlayPath, overlayRotation: $overlayRotation, underlayRgb: $underlayRgb, overlayRgb: $overlayRgb"
    }
}
