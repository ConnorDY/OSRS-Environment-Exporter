package cache.definitions.converters

import cache.definitions.ModelDefinition
import cache.definitions.ObjectDefinition
import cache.loaders.ModelLoader
import models.DebugOptionsModel
import org.slf4j.LoggerFactory

class ObjectToModelConverter(
    private val modelLoader: ModelLoader,
    private val debugOptionsModel: DebugOptionsModel,
    private val litModelCache: HashMap<Long, ModelDefinition> = HashMap()
) {
    private val logger = LoggerFactory.getLogger(ObjectToModelConverter::class.java)

    init {
        val listener: (Any) -> Unit = {
            litModelCache.clear()
        }
        debugOptionsModel.onlyType10Models.addEarlyListener(listener)
        debugOptionsModel.modelSubIndex.addEarlyListener(listener)
        debugOptionsModel.badModelIndexOverride.addEarlyListener(listener)
        debugOptionsModel.removeProperlyTypedModels.addEarlyListener(listener)
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
            if ((type != 10 && debugOptionsModel.onlyType10Models.get()) || modelIds == null) {
                return null
            }
            val modelLen = modelIds.size

            val debugSubIndex = debugOptionsModel.modelSubIndex.get()
            if (modelLen > 1 && debugSubIndex != -1) {
                if (debugSubIndex in 0 until modelLen) {
                    return getAndRotateModel(debugSubIndex, isRotated, orientation, modelIds)
                }
                return null
            } else if (debugOptionsModel.removeProperlyTypedModels.get()) {
                return null
            }

            for (i in 0 until modelLen) {
                val nextModel = getAndRotateModel(i, isRotated, orientation, modelIds) ?: continue
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
                val indexOverride = debugOptionsModel.badModelIndexOverride.get()
                if (indexOverride !in modelTypes.indices) {
                    logger.debug("Bad model index, not replacing")
                    return null
                }
                logger.debug("Bad model index, replacing with {} (out of {})", indexOverride, modelTypes.size)
                modelIdx = indexOverride
            } else if (debugOptionsModel.removeProperlyTypedModels.get()) {
                return null
            }
            val isRotated = isRotated xor (orientation > 3)
            modelDefinition = getAndRotateModel(modelIdx, isRotated, orientation, modelIds!!)
        }
        if (modelDefinition == null) {
            return null
        }
        return modelDefinition
    }

    private fun ObjectDefinition.getAndRotateModel(modelIdx: Int, isRotated: Boolean, orientation: Int, modelIds: IntArray): ModelDefinition? {
        var modelId = modelIds[modelIdx]
        if (isRotated) {
            modelId += 65536
        }
        val modelDefinition = modelLoader[modelId] ?: return null

        if (isRotated) {
            modelDefinition.rotateMulti()
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
        return modelDefinition
    }
}
