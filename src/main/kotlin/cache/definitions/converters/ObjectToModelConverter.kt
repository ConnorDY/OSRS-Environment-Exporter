package cache.definitions.converters

import cache.definitions.ModelDefinition
import cache.definitions.ObjectDefinition
import cache.loaders.ModelLoader
import models.DebugOptionsModel

class ObjectToModelConverter(
    private val modelLoader: ModelLoader,
    private val debugOptionsModel: DebugOptionsModel,
    private val litModelCache: HashMap<Long, ModelDefinition> = HashMap()
) {
    init {
        val listener: (Any) -> Unit = {
            litModelCache.clear()
        }
        debugOptionsModel.onlyType10Models.addListener(listener)
        debugOptionsModel.modelSubIndex.addListener(listener)
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
            for (i in 0 until modelLen) {
                modelDefinition = getAndRotateModel(i, isRotated, orientation, modelIds)
            }
            if (modelLen > 1) {
                // TODO: Combine models?
                val idx = debugOptionsModel.modelSubIndex.get()
                if (idx in 0 until modelLen) {
                    return getAndRotateModel(idx, isRotated, orientation, modelIds)
                }
                return null
            }
        } else {
            val modelIdx = modelTypes.indexOf(type)
            if (modelIdx == -1) {
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
