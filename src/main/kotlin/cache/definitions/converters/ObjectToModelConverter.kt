package cache.definitions.converters

import cache.definitions.ModelDefinition
import cache.definitions.ObjectDefinition
import cache.loaders.ModelLoader
import com.google.inject.Inject

class ObjectToModelConverter @Inject constructor(
    private val modelLoader: ModelLoader,
    private val litModelCache: HashMap<Long, ModelDefinition> = HashMap()
) {

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

    private fun ObjectDefinition.getModelDefinition(type: Int, orientation: Int): ModelDefinition? {
        var orientation = orientation
        var modelDefinition: ModelDefinition? = null
        if (modelTypes == null) {
            if (type != 10 || modelIds == null) {
                return null
            }
            val modelLen = modelIds!!.size
            for (i in 0 until modelLen) {
                var modelId = modelIds!![i]
                if (isRotated) {
                    modelId += 65536
                }
                modelDefinition = modelLoader.get(modelId)
                if (modelDefinition == null) {
                    return null
                }

                if (isRotated) {
                    modelDefinition.rotateMulti()
                }
            }
            if (modelLen > 1) {
                modelDefinition = ModelDefinition()
            }
        } else {
            var modelIdx = -1
            for (i in modelTypes!!.indices) {
                if (modelTypes!![i] == type) {
                    modelIdx = i
                    break
                }
            }
            if (modelIdx == -1) {
                return null
            }
            var modelId = modelIds!![modelIdx]
            val isRotated = isRotated xor (orientation > 3)
            if (isRotated) {
                modelId += 65536
            }
            modelDefinition = modelLoader.get(modelId)

            if (modelDefinition == null) {
                return null
            }

            if (isRotated) {
                modelDefinition.rotateMulti()
            }
        }
        if (modelDefinition == null) {
            return null
        }

        orientation = orientation and 0x3
        when (orientation) {
            1 -> {
                modelDefinition.rotateY90Ccw()
            }
            2 -> {
                modelDefinition.rotateY180()
            }
            3 -> {
                modelDefinition.rotateY270Ccw()
            }
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
