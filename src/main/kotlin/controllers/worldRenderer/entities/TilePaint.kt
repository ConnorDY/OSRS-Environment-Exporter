package controllers.worldRenderer.entities

import controllers.worldRenderer.Constants
import controllers.worldRenderer.components.*
import controllers.worldRenderer.helpers.GpuIntBuffer
import controllers.worldRenderer.helpers.ModelBuffers
import controllers.worldRenderer.helpers.ModelBuffers.Companion.FLAG_SCENE_BUFFER
import utils.EventType
import utils.Observable

class TilePaint(
    swHeight: Int = 0,
    seHeight: Int = 0,
    neHeight: Int = 0,
    nwHeight: Int = 0,
    swColor: Int,
    seColor: Int,
    neColor: Int,
    nwColor: Int,
    var texture: Int,
    var rgb: Int,
    hoverComponent: HoverComponent = HoverComponent(),
    selectComponent: SelectComponent = SelectComponent(),
    clickableComponent: ClickableComponent = ClickableComponent()
): Observable<TilePaint>(),
    Renderable,
    Hoverable by hoverComponent,
    Selectable by selectComponent,
    Clickable by clickableComponent {

    init {
        hoverComponent.observable = this
        selectComponent.observable = this
        clickableComponent.onClickFunc = {
            isSelected = true
        }
    }

    var swHeight: Int = swHeight
        set(value) {
            if (value == field) return
            field = value
            notifyObservers(EventType.SELECT)
        }

    var seHeight: Int = seHeight
        set(value) {
            if (value == field) return
            field = value
            notifyObservers(EventType.SELECT)
        }
    var neHeight: Int = neHeight
        set(value) {
            if (value == field) return
            field = value
            notifyObservers(EventType.SELECT)
        }
    var nwHeight: Int = nwHeight
        set(value) {
            if (value == field) return
            field = value
            notifyObservers(EventType.SELECT)
        }

    var swColor: Int = swColor
    get() {
        if (isSelected) {
            return Constants.SELECTED_HSL
        }
        if (isHovered) {
            return Constants.HOVER_HSL
        }
        return field
    }
    var seColor: Int = seColor
        get() {
            if (isSelected) {
                return Constants.SELECTED_HSL
            }
            if (isHovered) {
                return Constants.HOVER_HSL
            }
            return field
        }
    var neColor: Int = neColor
        get() {
            if (isSelected) {
                return Constants.SELECTED_HSL
            }
            if (isHovered) {
                return Constants.HOVER_HSL
            }
            return field
        }
    var nwColor: Int = nwColor
        get() {
            if (isSelected) {
                return Constants.SELECTED_HSL
            }
            if (isHovered) {
                return Constants.HOVER_HSL
            }
            return field
        }

    var computeObj: ComputeObj = ComputeObj()

    override fun draw(modelBuffers: ModelBuffers, sceneX: Int, sceneY: Int, height: Int, objType: Int) {
        val x: Int = sceneX * Constants.LOCAL_TILE_SIZE
        val z: Int = sceneY * Constants.LOCAL_TILE_SIZE
        val b: GpuIntBuffer = modelBuffers.modelBufferUnordered
        modelBuffers.incUnorderedModels()
        b.ensureCapacity(13)

        computeObj.idx = modelBuffers.targetBufferOffset
        computeObj.flags = FLAG_SCENE_BUFFER
        computeObj.x = x
        computeObj.z = z
        computeObj.pickerId = modelBuffers.calcPickerId(sceneX, sceneY, 30)
        b.buffer.put(computeObj.toArray())

        modelBuffers.addTargetBufferOffset(computeObj.size * 3)
    }

    override fun drawDynamic(modelBuffers: ModelBuffers, sceneX: Int, sceneY: Int, height: Int) {
        TODO("Not yet implemented")
    }

    fun recompute(modelBuffers: ModelBuffers) {
        val b: GpuIntBuffer = modelBuffers.modelBufferUnordered
        modelBuffers.incUnorderedModels()
        b.ensureCapacity(13)
        computeObj.flags = 0

        b.buffer.put(computeObj.toArray())
    }

    override fun clearDraw(modelBuffers: ModelBuffers) {
        val b: GpuIntBuffer = modelBuffers.modelBufferUnordered
        modelBuffers.incUnorderedModels()
        b.ensureCapacity(13)

        computeObj.x = Int.MAX_VALUE
        computeObj.y = Int.MAX_VALUE
        computeObj.z = Int.MAX_VALUE
        b.buffer.put(computeObj.toArray())
    }
}