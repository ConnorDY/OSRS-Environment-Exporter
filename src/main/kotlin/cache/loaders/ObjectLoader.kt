package cache.loaders

import cache.ConfigType
import cache.IndexType
import cache.definitions.ObjectDefinition
import cache.utils.read24BitInt
import cache.utils.readString
import cache.utils.readUnsignedByte
import cache.utils.readUnsignedShort
import com.displee.cache.CacheLibrary
import com.google.inject.Inject
import java.nio.ByteBuffer

class ObjectLoader @Inject constructor(
    cacheLibrary: CacheLibrary,
    private val objectDefinitionCache: HashMap<Int, ObjectDefinition> = HashMap()
) {
    fun getAll(): HashMap<Int, ObjectDefinition> {
        return objectDefinitionCache
    }

    fun get(id: Int): ObjectDefinition? {
        return objectDefinitionCache[id]
    }

    fun load(id: Int, b: ByteArray?): ObjectDefinition {
        val def = ObjectDefinition()
        val inputStream = ByteBuffer.wrap(b)
        def.id = id
        while (true) {
            val opcode = inputStream.readUnsignedByte()
            if (opcode == 0) {
                break
            }
            processOp(opcode, def, inputStream)
        }
        post(def)
        return def
    }

    private fun processOp(opcode: Int, def: ObjectDefinition, inputStream: ByteBuffer) {
        println("Processing opcode " + opcode)
        if (opcode == 1) {
            val length: Int = inputStream.readUnsignedByte()
            if (length > 0) {
                val modelTypes = IntArray(length)
                val modelIds = IntArray(length)
                for (index in 0 until length) {
                    modelIds[index] = inputStream.readUnsignedShort()
                    modelTypes[index] = inputStream.readUnsignedByte()
                }
                def.modelTypes = modelTypes
                def.modelIds = modelIds
            }
        } else if (opcode == 2) {
            def.name = inputStream.readString().toString()
        } else if (opcode == 5) {
            val length: Int = inputStream.readUnsignedByte()
            if (length > 0) {
                def.modelTypes = null
                val objectModels = IntArray(length)
                for (index in 0 until length) {
                    objectModels[index] = inputStream.readUnsignedShort()
                }
                def.modelIds = objectModels
            }
        } else if (opcode == 14) {
            def.sizeX = inputStream.readUnsignedByte()
        } else if (opcode == 15) {
            def.sizeY = inputStream.readUnsignedByte()
        } else if (opcode == 17) {
            def.interactType = 0
            def.blocksProjectile = false
        } else if (opcode == 18) {
            def.blocksProjectile = false
        } else if (opcode == 19) {
            def.wallOrDoor = inputStream.readUnsignedByte()
        } else if (opcode == 21) {
            def.contouredGround = 0
        } else if (opcode == 22) {
            def.mergeNormals = true
        } else if (opcode == 23) {
            def.aBool2111 = true
        } else if (opcode == 24) {
            def.animationID = inputStream.readUnsignedShort()
            if (def.animationID == 0xFFFF) {
                def.animationID = -1
            }
        } else if (opcode == 27) {
            def.interactType = 1
        } else if (opcode == 28) {
            def.decorDisplacement = inputStream.readUnsignedByte()
        } else if (opcode == 29) {
            def.ambient = inputStream.get().toInt()
        } else if (opcode == 39) {
            def.contrast = inputStream.get() * 25
        } else if (opcode in 30..34) {
            val actions: Array<String?> = def.actions
            actions[opcode - 30] = inputStream.readString()
            if (actions[opcode - 30].equals("Hidden", ignoreCase = true)) {
                actions[opcode - 30] = null
            }
        } else if (opcode == 40) {
            val length: Int = inputStream.readUnsignedByte()
            val recolorToFind = ShortArray(length)
            val recolorToReplace = ShortArray(length)
            for (index in 0 until length) {
                recolorToFind[index] = inputStream.short
                recolorToReplace[index] = inputStream.short
            }
            def.recolorToFind = recolorToFind
            def.recolorToReplace = recolorToReplace
        } else if (opcode == 41) {
            val length: Int = inputStream.readUnsignedByte()
            val textureToFind = ShortArray(length)
            val textureToReplace = ShortArray(length)
            for (index in 0 until length) {
                textureToFind[index] = inputStream.short
                textureToReplace[index] = inputStream.short
            }
            def.retextureToFind = textureToFind
            def.textureToReplace = textureToReplace
        } else if (opcode == 61) {
            val ignore = inputStream.readUnsignedShort()
        } else if (opcode == 62) {
            def.isRotated = true
        } else if (opcode == 64) {
            def.shadow = false
        } else if (opcode == 65) {
            def.modelSizeX = inputStream.readUnsignedShort()
        } else if (opcode == 66) {
            def.modelSizeHeight = inputStream.readUnsignedShort()
        } else if (opcode == 67) {
            def.modelSizeY = inputStream.readUnsignedShort()
        } else if (opcode == 68) {
            def.mapSceneID = inputStream.readUnsignedShort()
        } else if (opcode == 69) {
            def.blockingMask = inputStream.get().toInt()
        } else if (opcode == 70) {
            def.offsetX = inputStream.readUnsignedShort()
        } else if (opcode == 71) {
            def.offsetHeight = inputStream.readUnsignedShort()
        } else if (opcode == 72) {
            def.offsetY = inputStream.readUnsignedShort()
        } else if (opcode == 73) {
            def.obstructsGround = true
        } else if (opcode == 74) {
            def.isHollow = true
        } else if (opcode == 75) {
            def.supportsItems = inputStream.readUnsignedByte()
        } else if (opcode == 77) {
            var varpID: Int = inputStream.readUnsignedShort()
            if (varpID == 0xFFFF) {
                varpID = -1
            }
            def.transformVarbit = varpID
            var configId: Int = inputStream.readUnsignedShort()
            if (configId == 0xFFFF) {
                configId = -1
            }
            def.transformVarp = configId
            val length: Int = inputStream.readUnsignedByte()
            val configChangeDest = IntArray(length + 2)
            for (index in 0..length) {
                configChangeDest[index] = inputStream.readUnsignedShort()
                if (0xFFFF == configChangeDest[index]) {
                    configChangeDest[index] = -1
                }
            }
            configChangeDest[length + 1] = -1
            def.transforms = configChangeDest
        } else if (opcode == 78) {
            def.ambientSoundId = inputStream.readUnsignedShort()
            def.anInt2083 = inputStream.readUnsignedByte()
        } else if (opcode == 79) {
            def.anInt2112 = inputStream.readUnsignedShort()
            def.anInt2113 = inputStream.readUnsignedShort()
            def.anInt2083 = inputStream.readUnsignedByte()
            val length: Int = inputStream.readUnsignedByte()
            val anIntArray2084 = IntArray(length)
            for (index in 0 until length) {
                anIntArray2084[index] = inputStream.readUnsignedShort()
            }
            def.anIntArray2084 = anIntArray2084
        } else if (opcode == 81) {
            def.contouredGround = inputStream.readUnsignedByte() * 256
        } else if (opcode == 82) {
            def.mapAreaId = inputStream.readUnsignedShort()
        } else if (opcode == 92) {
            var transformVarbit: Int = inputStream.readUnsignedShort()
            if (transformVarbit == 0xFFFF) {
                transformVarbit = -1
            }
            def.transformVarbit = transformVarbit
            var transformVarp: Int = inputStream.readUnsignedShort()
            if (transformVarp == 0xFFFF) {
                transformVarp = -1
            }
            def.transformVarp = transformVarp
            var transform: Int = inputStream.readUnsignedShort()
            if (transform == 0xFFFF) {
                transform = -1
            }
            val length: Int = inputStream.readUnsignedByte()
            val transforms = IntArray(length + 2)
            for (index in 0..length) {
                transforms[index] = inputStream.readUnsignedShort()
                if (0xFFFF == transforms[index]) {
                    transforms[index] = -1
                }
            }
            transforms[length + 1] = transform
            def.transforms = transforms
        } else if (opcode == 249) {
            val length: Int = inputStream.readUnsignedByte()
            val params: MutableMap<Int, Any> = java.util.HashMap(length)
            for (i in 0 until length) {
                val isString = inputStream.readUnsignedByte() == 1
                val key: Int = inputStream.read24BitInt()
                val value = if (isString) {
                    inputStream.readString()
                } else {
                    inputStream.int
                }
                params[key] = value as Any
            }
            def.params = params
        } else {
            println("Unrecognized opcode: $opcode");
        }
    }

    private fun post(def: ObjectDefinition) {
        if (def.wallOrDoor == -1) {
            def.wallOrDoor = 0
            if (def.modelIds != null && (def.modelTypes == null || def.modelTypes!![0] == 10)) {
                def.wallOrDoor = 1
            }
            for (var1 in 0..4) {
                if (def.actions[var1] != null) {
                    def.wallOrDoor = 1
                }
            }
        }
        if (def.supportsItems == -1) {
            def.supportsItems = if (def.interactType != 0) 1 else 0
        }
    }

    init {
        val index = cacheLibrary.index(IndexType.CONFIGS.id)
        val archive = index.archive(ConfigType.OBJECT.id)
        for (file in archive!!.fileIds()) {
            val data = cacheLibrary.data(IndexType.CONFIGS.id, ConfigType.OBJECT.id, file)
            val def = load(file, data)
            objectDefinitionCache[def.id] = def
        }
        index.unCache()
    }
}