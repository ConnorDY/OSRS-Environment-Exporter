package cache.definitions.converters

import cache.LocationType
import cache.definitions.ModelDefinition
import cache.definitions.ObjectDefinition
import cache.loaders.ModelLoader
import models.DebugOptionsModel
import org.slf4j.LoggerFactory

class ObjectToModelConverter(
    private val modelLoader: ModelLoader,
    private val debugOptionsModel: DebugOptionsModel,
) {
    private val logger = LoggerFactory.getLogger(ObjectToModelConverter::class.java)
    private val litModelCache = HashMap<Long, ModelDefinition>()

    init {
        val listener: (Any?) -> Unit = {
            litModelCache.clear()
        }
        debugOptionsModel.modelSubIndex.value.addEarlyListener(listener)
        debugOptionsModel.badModelIndexOverride.value.addEarlyListener(listener)
        debugOptionsModel.removeProperlyTypedModels.value.addEarlyListener(listener)
    }

    fun toModel(objectDefinition: ObjectDefinition, type: Int, orientation: Int): ModelDefinition? {
        val modelTag: Long = if (objectDefinition.modelTypes == null) {
            orientation + (10 shl 3) + (objectDefinition.id shl 10).toLong()
        } else {
            orientation + (type shl 3) + (objectDefinition.id shl 10).toLong()
        }
        var litModel: ModelDefinition? = litModelCache[modelTag]
        if (litModel == null) {
            litModel = objectDefinition.getModelDefinition(type, orientation)
            if (litModel == null) {
                return null
            }

            litModel.tag = modelTag
            litModelCache[modelTag] = litModel
        }
        return litModel
    }

    private fun ObjectDefinition.getModelDefinition(
        type: Int,
        orientation: Int
    ): ModelDefinition? {
        val modelIds = modelIds
        val modelTypes = modelTypes
        var modelDefinition: ModelDefinition? = null
        if (modelTypes == null) {
            if (modelIds == null) {
                return null
            }
            val modelLen = modelIds.size
            val isRotated = isRotated xor (type == LocationType.WALL_CORNER.id && orientation > 3)

            val debugSubIndex = debugOptionsModel.modelSubIndex.value.get()
            if (modelLen > 1 && debugSubIndex != -1) {
                if (debugSubIndex in 0 until modelLen) {
                    return getAndRotateModel(debugSubIndex, isRotated, type, orientation, modelIds)
                }
                return null
            } else if (debugOptionsModel.removeProperlyTypedModels.value.get()) {
                return null
            }

            for (i in 0 until modelLen) {
                val nextModel = getAndRotateModel(i, isRotated, type, orientation, modelIds) ?: continue
                modelDefinition =
                    if (modelDefinition == null) nextModel
                    else ModelDefinition.combine(modelDefinition, nextModel)
            }
        } else {
            var modelIdx = modelTypes.indexOf(type)
            if (modelIdx == -1 && modelTypes.size == 1) {
                // single model type only
                modelIdx = 0
            } else if (modelIdx == -1) {
                val indexOverride = debugOptionsModel.badModelIndexOverride.value.get()
                if (indexOverride !in modelTypes.indices) {
                    logger.debug("Bad model index, not replacing")
                    return null
                }
                logger.debug("Bad model index, replacing with {} (out of {})", indexOverride, modelTypes.size)
                modelIdx = indexOverride
            } else if (debugOptionsModel.removeProperlyTypedModels.value.get()) {
                return null
            }
            val isRotated = isRotated xor (orientation > 3)
            modelDefinition = getAndRotateModel(modelIdx, isRotated, type, orientation, modelIds!!)
        }
        if (modelDefinition == null) {
            return null
        }
        return modelDefinition
    }

    private fun ObjectDefinition.getAndRotateModel(modelIdx: Int, isRotated: Boolean, type: Int, orientation: Int, modelIds: IntArray): ModelDefinition? {
        var modelId = modelIds[modelIdx]
        if (isRotated) {
            modelId += 65536
        }
        val modelDefinition = modelLoader[modelId] ?: return null

        if (isRotated) {
            modelDefinition.flipZ()
        }

        if (type in LocationType.INSIDE_WALL_DECORATION.id..LocationType.DIAGONAL_WALL_DECORATION.id && orientation > 3) {
            modelDefinition.rotate(256)
            modelDefinition.translate(45, 0, -45)
        }

        when (orientation and 3) {
            1 -> modelDefinition.rotateY90Ccw()
            2 -> modelDefinition.rotateY180()
            3 -> modelDefinition.rotateY270Ccw()
        }
        if (recolorToFind != null) {
            for (i in recolorToFind!!.indices) {
                modelDefinition.recolor(recolorToFind!![i], recolorToReplace[i])
            }
        }
        if (retextureToFind != null) {
            for (i in retextureToFind!!.indices) {
                modelDefinition.retexture(
                    retextureToFind!![i],
                    textureToReplace!![i]
                )
            }
        }

        modelDefinition.scale(modelSizeX, modelSizeHeight, modelSizeY)
        modelDefinition.translate(offsetX, offsetHeight, offsetY)
        return modelDefinition
    }
}
