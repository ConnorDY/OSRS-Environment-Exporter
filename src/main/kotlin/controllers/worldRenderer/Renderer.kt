package controllers.worldRenderer

import cache.LocationType
import cache.definitions.Location
import cache.definitions.ModelDefinition
import cache.definitions.ObjectDefinition
import cache.definitions.converters.ObjectToModelConverter
import com.google.inject.Inject
import com.jogamp.newt.NewtFactory
import com.jogamp.newt.event.WindowAdapter
import com.jogamp.newt.event.WindowEvent
import com.jogamp.newt.javafx.NewtCanvasJFX
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.*
import com.jogamp.opengl.util.Animator
import com.jogamp.opengl.util.GLBuffers
import controllers.worldRenderer.entities.Model
import controllers.worldRenderer.entities.StaticObject
import controllers.worldRenderer.entities.TilePaint
import controllers.worldRenderer.helpers.*
import controllers.worldRenderer.helpers.GLUtil.glDeleteBuffer
import controllers.worldRenderer.helpers.GLUtil.glDeleteBuffers
import controllers.worldRenderer.helpers.GLUtil.glDeleteFrameBuffer
import controllers.worldRenderer.helpers.GLUtil.glDeleteRenderbuffers
import controllers.worldRenderer.helpers.GLUtil.glDeleteTexture
import controllers.worldRenderer.helpers.GLUtil.glDeleteVertexArrays
import controllers.worldRenderer.helpers.GLUtil.glGenBuffers
import controllers.worldRenderer.helpers.GLUtil.glGenVertexArrays
import controllers.worldRenderer.helpers.GLUtil.glGetInteger
import controllers.worldRenderer.shaders.Shader
import controllers.worldRenderer.shaders.ShaderException
import controllers.worldRenderer.shaders.Template
import javafx.scene.Group
import models.HoverModel
import models.HoverObject
import models.ObjectsModel
import models.scene.*
import java.awt.event.ActionListener
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.math.min

class Renderer @Inject constructor(
    private val camera: Camera,
    private val scene: Scene,
    private val sceneUploader: SceneUploader,
    private val inputHandler: InputHandler,
    private val textureManager: TextureManager,
    private val debugModel: models.DebugModel,
    private val objectsModel: ObjectsModel,
    private val hoverModel: HoverModel,
    private val objectToModelConverter: ObjectToModelConverter
) : GLEventListener {
    private val MAX_TEMP_VERTICES: Int = 65535

    private lateinit var gl: GL4
    private var glProgram = 0
    private var glComputeProgram = 0
    private var glSmallComputeProgram = 0
    private var glUnorderedComputeProgram = 0

    private var fboMainRenderer = 0
    private var rboDepthMain = 0
    private var texColorMain = 0
    private var texPickerMain = 0
    private val pboIds = IntArray(3)
    private var pboIndex = 0

    private var vaoHandle = 0

    private var fboSceneHandle = 0
    private var colorTexSceneHandle = 0
    private var rboSceneHandle = 0
    private var depthTexSceneHandle = 0

    // scene vertex buffer id
    private var bufferId = 0

    // scene uv buffer id
    private var uvBufferId = 0

    private var tmpBufferId = 0// temporary scene vertex buffer
    private var tmpUvBufferId = 0// temporary scene uv buffer
    private var tmpModelBufferId = 0 // scene model buffer, large
    private var tmpModelBufferSmallId = 0// scene model buffer, small

    private var tmpModelBufferUnorderedId = 0
    private var tmpOutBufferId = 0 // target vertex buffer for compute shaders
    private var tmpOutUvBufferId = 0// target uv buffer for compute shaders
    private var colorPickerBufferId = 0// buffer for unique picker id
    private var animFrameBufferId = 0// which frame the model should display on
    private var selectedIdsBufferId = 0

    private var hoverId = 0

    private var textureArrayId = 0
    private var uniformBufferId = 0
    private val uniformBuffer: IntBuffer = GpuIntBuffer.allocateDirect(5 + 3 + 1)
    private val textureOffsets = FloatArray(128)

    private lateinit var modelBuffers: ModelBuffers

    // Uniforms
    private var uniDrawDistance = 0
    private var uniProjectionMatrix = 0
    private var uniBrightness = 0
    private var uniTextures = 0
    private var uniTextureOffsets = 0
    private var uniBlockSmall = 0
    private var uniBlockLarge = 0
    private var uniBlockMain = 0
    private var uniSmoothBanding = 0
    private var uniHoverId = 0
    private var uniMouseCoordsId = 0

    // FIXME: setting these here locks this in as the minimum
    // figure out how to make these small and resize programmatically after load
    var canvasWidth = 800
    var canvasHeight = (canvasWidth / 1.3).toInt()
    private var lastViewportWidth = 0
    private var lastViewportHeight = 0
    private var lastCanvasWidth = 0
    private var lastCanvasHeight = 0
    private var lastStretchedCanvasWidth = 0
    private var lastStretchedCanvasHeight = 0
    private var lastAntiAliasingMode: AntiAliasingMode? = null

    private val useComputeShaders: Boolean = false

    fun reposResize(x: Int, y: Int, width: Int, height: Int) {
        window.setPosition(x, y)
        window.setSize(width, height)
    }

    //-Dnewt.verbose=true
    //-Dnewt.debug=true
    lateinit var window: GLWindow
    lateinit var animator: Animator
    fun initCanvas(group: Group) {
        // center camera in viewport
        camera.centerX = canvasWidth / 2
        camera.centerY = canvasHeight / 2
        tmpOutUvBufferId = -1
        tmpOutBufferId = tmpOutUvBufferId
        tmpModelBufferUnorderedId = tmpOutBufferId
        tmpModelBufferSmallId = tmpModelBufferUnorderedId
        tmpModelBufferId = tmpModelBufferSmallId
        tmpUvBufferId = tmpModelBufferId
        tmpBufferId = tmpUvBufferId
        uniformBufferId = tmpBufferId
        uvBufferId = uniformBufferId
        bufferId = uvBufferId
        selectedIdsBufferId = -1
        colorPickerBufferId = selectedIdsBufferId
        modelBuffers = ModelBuffers()

        val jfxNewtDisplay = NewtFactory.createDisplay(null, false)
        val screen = NewtFactory.createScreen(jfxNewtDisplay, 0)
        val glProfile = GLProfile.get(GLProfile.GL4)
        val glCaps = GLCapabilities(glProfile)
        glCaps.alphaBits = 8

        window = GLWindow.create(screen, glCaps)
        window.addGLEventListener(this)
        window.addKeyListener(inputHandler)
        window.addMouseListener(inputHandler)
        inputHandler.renderer = this

        val glCanvas = NewtCanvasJFX(window)
        glCanvas.width = canvasWidth.toDouble()
        glCanvas.height = canvasHeight.toDouble()

        animator = Animator(window)
        animator.setUpdateFPSFrames(3, null)
        animator.start()

        // Begin bad hack to fix GLWindow not releasing focus properly //
        window.addWindowListener(object : WindowAdapter() {
            override fun windowGainedFocus(e: WindowEvent) {
                // when heavyweight window gains focus, also tell javafx to give focus to glCanvas
                glCanvas.requestFocus()
            }
        })
        glCanvas.focusedProperty().addListener { _, _, newValue ->
            if (!newValue) {
                window.isVisible = false
                window.isVisible = true
            }
        }
        // End bad hack //

        group.children.add(glCanvas)

        lastCanvasHeight = -1
        lastCanvasWidth = lastCanvasHeight
        lastViewportHeight = lastCanvasWidth
        lastViewportWidth = lastViewportHeight
        lastStretchedCanvasHeight = -1
        lastStretchedCanvasWidth = lastStretchedCanvasHeight
        lastAntiAliasingMode = null
        textureArrayId = -1

        scene.sceneChangeListeners.add(ActionListener {
            isSceneUploadRequired = true
            camera.cameraX = Constants.LOCAL_HALF_TILE_SIZE * scene.radius * REGION_SIZE
            camera.cameraY = Constants.LOCAL_HALF_TILE_SIZE * scene.radius * REGION_SIZE
            camera.cameraZ = -2500
        })
    }

    fun loadScene() {
        scene.load(10038, 1)
//        scene.load(9271, 1)
//        scene.load(12850, 1)
//        scene.load(9271, 1)
    }

    private var injectedObject: SceneObject? = null
    fun bindModels() {
        objectsModel.heldObject.addListener { _, _, newValue ->
            injectedObject = newValue
        }

//        scene.getRegion(0, 0)!!.tileChangeListeners.add(object : TileChangeListener {
//            override fun onTileChange(tile: SceneTile) {
//                println("tile changed".format(tile))
//            }
//        })
    }

//    private fun injectEntity(entity: Entity) {
//        sceneUploader.uploadModel(entity, modelBuffers.vertexBuffer, modelBuffers.uvBuffer)
//        injectedEntity = entity
//    }

    private var idx = 0
    private fun handleHover() {
        val mouseX: Int = inputHandler.mouseX
        val mouseY: Int = inputHandler.mouseY

        // Using 3 PBOs brings this function time to 0.05ms, with only 2 PBOs is it 10ms
        // This will write to pboIndex and read from nextIndex which should have finished drawing to
        // since it will 2 frames behind.
        pboIndex = (pboIndex + 1) % 3
        val nextIndex = (pboIndex + 1) % 3

        // Read from pixel buffer object async to get pixels without blocking render
        gl.glBindFramebuffer(GL.GL_READ_FRAMEBUFFER, fboMainRenderer)
        gl.glBindBuffer(GL2ES3.GL_PIXEL_PACK_BUFFER, pboIds[pboIndex])
        gl.glReadBuffer(GL2ES2.GL_COLOR_ATTACHMENT1)
        gl.glReadPixels(mouseX, canvasHeight - mouseY, 1, 1, GL2ES3.GL_RED_INTEGER, GL2ES2.GL_INT, 0)
        gl.glReadBuffer(GL.GL_COLOR_ATTACHMENT0)
        gl.glBindBuffer(GL2ES3.GL_PIXEL_PACK_BUFFER, 0)
        gl.glBindFramebuffer(GL.GL_READ_FRAMEBUFFER, 0)
        gl.glBindBuffer(GL2ES3.GL_PIXEL_PACK_BUFFER, pboIds[nextIndex])
        val srcBuf = gl.glMapBuffer(GL2ES3.GL_PIXEL_PACK_BUFFER, GL2ES3.GL_READ_ONLY)
        val pboBytes = ByteArray(4)
        srcBuf[pboBytes]
        val pickerId: Int =
            pboBytes[3].toInt() and 0xFF shl 24 or (pboBytes[2].toInt() and 0xFF shl 16) or (pboBytes[1].toInt() and 0xFF shl 8) or (pboBytes[0].toInt() and 0xFF)
        gl.glUnmapBuffer(GL2ES3.GL_PIXEL_PACK_BUFFER)
        if (pickerId == 1058050193) { // clear sky coor
            return
        }

        // -1 = null hover, ignore
        // -2 = we want to pass through this object
        if (pickerId == -1) {
            hoverId = -1
            return
        }
        if (pickerId != -2) {
            hoverId = pickerId
        }

        val x = hoverId shr 18 and 0x1FFF
        val y = hoverId shr 5 and 0x1FFF
        val type = hoverId and 0x1F
        val tile = scene.getTile(0, x, y) ?: return

        hoverModel.hovered.set(HoverObject(x, y, LocationType.fromId(type)!!, tile))

        if (injectedObject != null && tile.tilePaint != null) {
            val objectDefinition: ObjectDefinition = injectedObject!!.objectDefinition
            val modelDefinition: ModelDefinition? =
                objectToModelConverter.toModel(objectDefinition, injectedObject!!.type, injectedObject!!.orientation)
                    ?: return
            val model = Model(modelDefinition!!, objectDefinition.ambient, objectDefinition.contrast)
            model.xOff = objectDefinition.sizeX * REGION_SIZE
            model.yOff = objectDefinition.sizeY * REGION_SIZE
            sceneUploader.uploadModel(
                StaticObject(model, tile.tilePaint!!.nwHeight, injectedObject!!.type, injectedObject!!.orientation),
                modelBuffers.vertexBuffer,
                modelBuffers.uvBuffer
            )
            model.drawDynamic(modelBuffers, x, y, tile.tilePaint!!.nwHeight)
        }
    }

    fun redrawTile(tilePaint: TilePaint) {
        // modify the tiles vertex data
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId)
        val buf = GpuIntBuffer()
        sceneUploader.upload(tilePaint, buf, GpuFloatBuffer())
        buf.flip()

        gl.glBufferSubData(
            GL.GL_ARRAY_BUFFER,
            (tilePaint.computeObj.offset * 4 * GLBuffers.SIZEOF_INT).toLong(), buf.buffer.limit() * GLBuffers.SIZEOF_INT.toLong(), buf.buffer
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0)

        // add the tile to be recomputed
        tilePaint.recompute(modelBuffers)
    }

    private fun handleClick() {
        if (hoverId == -1) {
            return
        }

        // modifying tile height code
        if (inputHandler.mouseClicked) {
            inputHandler.mouseClicked = false
            val x = hoverId shr 18 and 0x1FFF
            val y = hoverId shr 5 and 0x1FFF
            val tile = scene.getTile(0, x, y) ?: return
            if (tile.tilePaint == null) return

            tile.tilePaint!!.swHeight += 10
            redrawTile(tile.tilePaint!!)

//            val north = scene.getTile(0, x, y-1)
//            if (north?.tilePaint != null) {
//                north.tilePaint!!.seHeight += 10
//                redrawTile(north.tilePaint!!)
//            }
//
//            val west = scene.getTile(0, x-1, y)
//            if (west?.tilePaint != null) {
//                west.tilePaint!!.neHeight += 10
//                redrawTile(west.tilePaint!!)
//            }
//
//            val northWest = scene.getTile(0, x-1, y-1)
//            if (northWest?.tilePaint != null) {
//                northWest.tilePaint!!.swHeight += 10
//                redrawTile(northWest.tilePaint!!)
//            }
        }

        // adding model to scene code
        if (inputHandler.mouseClicked) {
            inputHandler.mouseClicked = false
            val x = hoverId shr 18 and 0x1FFF
            val y = hoverId shr 5 and 0x1FFF
            val tile = scene.getTile(0, x, y)
            val region = scene.getRegionFromSceneCoord(x, y)
            if (injectedObject != null && region != null) {
                val objectDefinition: ObjectDefinition = injectedObject!!.objectDefinition
                val loc = Location(objectDefinition.id, injectedObject!!.type, injectedObject!!.orientation, x, y, 0)
                region.locationsDefinition.locations.add(loc)

                val modelDefinition: ModelDefinition? = objectToModelConverter.toModel(
                    objectDefinition,
                    injectedObject!!.type,
                    injectedObject!!.orientation
                )
                val model = Model(modelDefinition!!, objectDefinition.ambient, objectDefinition.contrast)
                model.xOff = objectDefinition.sizeX * REGION_SIZE
                model.yOff = objectDefinition.sizeY * REGION_SIZE
                sceneUploader.uploadModel(
                    StaticObject(model, tile!!.tilePaint!!.nwHeight, injectedObject!!.type, injectedObject!!.orientation),
                    modelBuffers.vertexBuffer,
                    modelBuffers.uvBuffer
                )
                model.drawPersistent(modelBuffers, x, y, tile.tilePaint!!.nwHeight, injectedObject!!.type)
            }
            if (injectedObject == null) {
                val type = hoverId and 0x1F
                val tile = scene.getTile(0, x, y)
                val region = scene.getRegionFromSceneCoord(x, y)
                if (tile != null && region != null) {
//                    if (type == 10) {
//                        tile.gameObjects[0].entity?.getModel()?.clearDraw(modelBuffers)
//                        val removed = region.locationsDefinition.locations.removeIf { it.type == 10 }
//                        println("removed? $removed")
//                    }
//                    tile.tilePaint?.clearDraw(modelBuffers)
//                    tile.gameObjects.forEach { it.entity?.getModel()?.clearDraw(modelBuffers) }
                }

//            val obj = StaticObject(injectedModel!!, tile?.tilePaint?.neHeight!!)
//            injectEntity(obj)
            }
        }
    }

    override fun init(drawable: GLAutoDrawable) {
        try {
            gl = drawable.gl.gL4
            gl.glEnable(GL.GL_DEPTH_TEST)
            gl.glDepthFunc(GL.GL_LEQUAL)
            gl.glDepthRangef(0f, 1f)
            //            gl.glGetIntegerv(gl.GL_DEPTH_BITS, intBuf1);
//            System.out.printf("depth bits %s \n", intBuf1.get(0));
            initProgram()
            initUniformBuffer()
            initBuffers()
            initPickerBuffer()
            initVao()

            // disable vsync
            gl.swapInterval = 0;
        } catch (e: ShaderException) {
            e.printStackTrace()
        }
    }

    override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {
        gl = drawable.gl.gL4
        canvasWidth = width
        canvasHeight = height
        initPickerBuffer()
        camera.centerX = canvasWidth / 2
        camera.centerY = canvasHeight / 2
        gl.glViewport(x, y, width, height)
    }

    private var isSceneUploadRequired = true
    private val clientStart = System.currentTimeMillis()
    override fun display(drawable: GLAutoDrawable?) {
        if (isSceneUploadRequired) {
            uploadScene()
        }

        sceneUploader.resetOffsets() // to reuse uploadModel function

        debugModel.fps.set(animator.lastFPS)
        handleClick()
        handleHover()

        if (canvasWidth > 0 && canvasHeight > 0 && (canvasWidth != lastViewportWidth || canvasHeight != lastViewportHeight)) {
            createProjectionMatrix(
                0f,
                canvasWidth.toFloat(),
                canvasHeight.toFloat(),
                0f,
                1f,
                (Constants.MAX_DISTANCE * Constants.LOCAL_TILE_SIZE).toFloat()
            )
            lastViewportWidth = canvasWidth
            lastViewportHeight = canvasHeight
        }

        // base FBO to enable picking
        gl.glBindFramebuffer(GL.GL_DRAW_FRAMEBUFFER, fboMainRenderer)

        // Setup anti-aliasing
        val antiAliasingMode: AntiAliasingMode = AntiAliasingMode.MSAA_4
        val aaEnabled = antiAliasingMode !== AntiAliasingMode.DISABLED
        if (aaEnabled) {
            gl.glEnable(GL.GL_MULTISAMPLE)
            val stretchedCanvasWidth = canvasWidth
            val stretchedCanvasHeight = canvasHeight

            // Re-create fbo
            if (lastStretchedCanvasWidth != stretchedCanvasWidth || lastStretchedCanvasHeight != stretchedCanvasHeight || lastAntiAliasingMode !== antiAliasingMode
            ) {
                val maxSamples: Int = glGetInteger(gl, GL.GL_MAX_SAMPLES)
                val samples = min(antiAliasingMode.ordinal, maxSamples)
                initAAFbo(stretchedCanvasWidth, stretchedCanvasHeight, samples)
                lastStretchedCanvasWidth = stretchedCanvasWidth
                lastStretchedCanvasHeight = stretchedCanvasHeight
            }
            gl.glBindFramebuffer(GL.GL_DRAW_FRAMEBUFFER, fboSceneHandle)
        }
        lastAntiAliasingMode = antiAliasingMode
        val i =
            GLBuffers.newDirectIntBuffer(intArrayOf(GL.GL_COLOR_ATTACHMENT0, GL2ES2.GL_COLOR_ATTACHMENT1))
        gl.glDrawBuffers(2, i)

        // Clear scene
        val sky = 9493480
        gl.glClearColor((sky shr 16 and 0xFF) / 255f, (sky shr 8 and 0xFF) / 255f, (sky and 0xFF) / 255f, 1f)
        gl.glClear(GL.GL_COLOR_BUFFER_BIT or GL.GL_DEPTH_BUFFER_BIT)

        modelBuffers.flip()
        modelBuffers.flipVertUv()

        val vertexBuffer: IntBuffer = modelBuffers.vertexBuffer.buffer
        val uvBuffer: FloatBuffer = modelBuffers.uvBuffer.buffer
        val modelBuffer: IntBuffer = modelBuffers.modelBuffer.buffer
        val modelBufferSmall: IntBuffer = modelBuffers.modelBufferSmall.buffer
        val modelBufferUnordered: IntBuffer = modelBuffers.modelBufferUnordered.buffer
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, tmpBufferId)
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            vertexBuffer.limit() * Integer.BYTES.toLong(),
            vertexBuffer,
            GL.GL_DYNAMIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, tmpUvBufferId)
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            uvBuffer.limit() * java.lang.Float.BYTES.toLong(),
            uvBuffer,
            GL.GL_DYNAMIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, tmpModelBufferId)
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            modelBuffer.limit() * Integer.BYTES.toLong(),
            modelBuffer,
            GL.GL_DYNAMIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, tmpModelBufferSmallId)
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            modelBufferSmall.limit() * Integer.BYTES.toLong(),
            modelBufferSmall,
            GL.GL_DYNAMIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, tmpModelBufferUnorderedId)
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            modelBufferUnordered.limit() * Integer.BYTES.toLong(),
            modelBufferUnordered,
            GL.GL_DYNAMIC_DRAW
        )
        val clientCycle = ((System.currentTimeMillis() - clientStart) / 20).toInt() // 50 fps

        // UBO
        gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, uniformBufferId)
        uniformBuffer.clear()
        uniformBuffer
            .put(camera.yaw)
            .put(camera.pitch)
            .put(camera.centerX)
            .put(camera.centerY)
            .put(camera.scale)
            .put(camera.cameraX) //x
            .put(camera.cameraZ) // z
            .put(camera.cameraY) // y
            .put(clientCycle) // currFrame
        uniformBuffer.flip()
        gl.glBufferSubData(
            GL2ES3.GL_UNIFORM_BUFFER,
            0,
            uniformBuffer.limit() * Integer.BYTES.toLong(),
            uniformBuffer
        )
        gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, 0)

        val textureProvider = ""
        // Draw 3d scene
        if (textureProvider != null && bufferId != -1) {
            gl.glUniformBlockBinding(glSmallComputeProgram, uniBlockSmall, 0)
            gl.glUniformBlockBinding(glComputeProgram, uniBlockLarge, 0)
            gl.glBindBufferBase(GL2ES3.GL_UNIFORM_BUFFER, 0, uniformBufferId)

            /*
         * Compute is split into two separate programs 'small' and 'large' to
         * save on GPU resources. Small will sort <= 512 faces, large will do <= 4096.
         */

            // unordered
            gl.glUseProgram(glUnorderedComputeProgram)
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 0, tmpModelBufferUnorderedId)
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 1, bufferId)
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 2, tmpBufferId)
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 3, tmpOutBufferId)
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 4, tmpOutUvBufferId)
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 5, uvBufferId)
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 6, tmpUvBufferId)
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 7, colorPickerBufferId)
            //            gl.glBindBufferBase(gl.GL_SHADER_STORAGE_BUFFER, 8, animFrameBufferId);
            gl.glDispatchCompute(modelBuffers.unorderedModelsCount, 1, 1)

            // small
            gl.glUseProgram(glSmallComputeProgram)
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 0, tmpModelBufferSmallId)
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 1, bufferId)
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 2, tmpBufferId)
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 3, tmpOutBufferId) // vout[]
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 4, tmpOutUvBufferId) //uvout[]
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 5, uvBufferId)
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 6, tmpUvBufferId)
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 7, colorPickerBufferId)
            //            gl.glBindBufferBase(gl.GL_SHADER_STORAGE_BUFFER, 8, animFrameBufferId);
            gl.glDispatchCompute(modelBuffers.smallModelsCount, 1, 1)

            // large
            gl.glUseProgram(glComputeProgram)
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 0, tmpModelBufferId)
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 1, bufferId)
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 2, tmpBufferId)
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 3, tmpOutBufferId)
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 4, tmpOutUvBufferId)
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 5, uvBufferId)
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 6, tmpUvBufferId)
            gl.glBindBufferBase(GL3ES3.GL_SHADER_STORAGE_BUFFER, 7, colorPickerBufferId)
            //            gl.glBindBufferBase(gl.GL_SHADER_STORAGE_BUFFER, 8, animFrameBufferId);
            gl.glDispatchCompute(modelBuffers.largeModelsCount, 1, 1)
            gl.glMemoryBarrier(GL3ES3.GL_SHADER_STORAGE_BARRIER_BIT)

            if (textureArrayId == -1) {
                // lazy init textures as they may not be loaded at plugin start.
                // this will return -1 and retry if not all textures are loaded yet, too.
                textureArrayId = textureManager.initTextureArray(gl)
            }
//            val textures: Array<TextureDefinition> = textureProvider.getTextureDefinitions()
            gl.glUseProgram(glProgram)
            // Brightness happens to also be stored in the texture provider, so we use that
            gl.glUniform1f(uniBrightness, 0.7f) //(float) textureProvider.getBrightness());
            gl.glUniform1i(uniDrawDistance, Constants.MAX_DISTANCE * Constants.LOCAL_TILE_SIZE)
            gl.glUniform1f(uniSmoothBanding, 1f)

            // This is just for animating!
//            for (int id = 0; id < textures.length; ++id) {
//                TextureDefinition texture = textures[id];
//                if (texture == null) {
//                    continue;
//                }
//
//                textureProvider.load(id); // trips the texture load flag which lets textures animate
//
//                textureOffsets[id * 2] = texture.field1782;
//                textureOffsets[id * 2 + 1] = texture.field1783;
//            }
            gl.glUniform1i(uniHoverId, hoverId)
            gl.glUniform2i(uniMouseCoordsId, inputHandler.mouseX, canvasHeight - inputHandler.mouseY)

            // Bind uniforms
            gl.glUniformBlockBinding(glProgram, uniBlockMain, 0)
            gl.glUniform1i(uniTextures, 1) // texture sampler array is bound to texture1
            gl.glUniform2fv(uniTextureOffsets, 128, textureOffsets, 0)

            // We just allow the GL to do face culling. Note this requires the priority renderer
            // to have logic to disregard culled faces in the priority depth testing.
            gl.glEnable(GL.GL_CULL_FACE)

            // Draw output of compute shaders
            gl.glBindVertexArray(vaoHandle)
            gl.glDrawArrays(GL.GL_TRIANGLES, 0, modelBuffers.targetBufferOffset + modelBuffers.tempOffset)
            gl.glDisable(GL.GL_CULL_FACE)
            gl.glUseProgram(0)
        }

        if (aaEnabled) {
            gl.glBindFramebuffer(GL.GL_READ_FRAMEBUFFER, fboSceneHandle)
            gl.glBindFramebuffer(GL.GL_DRAW_FRAMEBUFFER, fboMainRenderer)
            gl.glBlitFramebuffer(
                0, 0, lastStretchedCanvasWidth, lastStretchedCanvasHeight,
                0, 0, lastStretchedCanvasWidth, lastStretchedCanvasHeight,
                GL.GL_COLOR_BUFFER_BIT or GL.GL_DEPTH_BUFFER_BIT, GL.GL_NEAREST
            )
            gl.glReadBuffer(GL2ES2.GL_COLOR_ATTACHMENT1)
            gl.glDrawBuffer(GL2ES2.GL_COLOR_ATTACHMENT1)
            gl.glBlitFramebuffer(
                0, 0, lastStretchedCanvasWidth, lastStretchedCanvasHeight,
                0, 0, lastStretchedCanvasWidth, lastStretchedCanvasHeight,
                GL.GL_COLOR_BUFFER_BIT or GL.GL_DEPTH_BUFFER_BIT, GL.GL_NEAREST
            )
            gl.glDisable(GL.GL_BLEND)

            // Reset
            gl.glReadBuffer(GL.GL_COLOR_ATTACHMENT0)
            gl.glDrawBuffer(GL.GL_COLOR_ATTACHMENT0)
        }

        gl.glBindFramebuffer(GL.GL_READ_FRAMEBUFFER, fboMainRenderer)
        gl.glBindFramebuffer(GL.GL_DRAW_FRAMEBUFFER, 0)
        gl.glBlitFramebuffer(
            0, 0, canvasWidth, canvasHeight,
            0, 0, canvasWidth, canvasHeight,
            GL.GL_COLOR_BUFFER_BIT or GL.GL_DEPTH_BUFFER_BIT, GL.GL_NEAREST
        )
        gl.glBindFramebuffer(GL.GL_READ_FRAMEBUFFER, 0)

        modelBuffers.clearVertUv()
        modelBuffers.clear()
    }

    private fun drawTiles() {
        modelBuffers.clear()
        modelBuffers.targetBufferOffset = 0
        for (x in 0 until scene.radius * REGION_SIZE) {
            for (y in 0 until scene.radius * REGION_SIZE) {
                val tile: SceneTile? = scene.getTile(0, x, y)
                if (tile != null) {
                    drawTile(tile)
                }
            }
        }

        // allocate enough size in the outputBuffer for the static verts + the dynamic verts -- each vertex is an ivec4, 4 ints
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, tmpOutBufferId)
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            ((modelBuffers.targetBufferOffset + MAX_TEMP_VERTICES) * GLBuffers.SIZEOF_INT * 4).toLong(),
            null,
            GL.GL_DYNAMIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, tmpOutUvBufferId)
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            ((modelBuffers.targetBufferOffset + MAX_TEMP_VERTICES) * GLBuffers.SIZEOF_FLOAT * 4).toLong(),
            null,
            GL.GL_DYNAMIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, colorPickerBufferId)
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            ((modelBuffers.targetBufferOffset + MAX_TEMP_VERTICES) * GLBuffers.SIZEOF_INT).toLong(),
            null,
            GL.GL_DYNAMIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, animFrameBufferId)
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            ((modelBuffers.targetBufferOffset + MAX_TEMP_VERTICES) * GLBuffers.SIZEOF_INT * 4).toLong(),
            null,
            GL.GL_DYNAMIC_DRAW
        )
    }

    private fun drawTile(tile: SceneTile) {
        val x: Int = tile.x
        val y: Int = tile.y
        if (tile.tilePaint != null) {
            tile.tilePaint!!.draw(modelBuffers, x, y, 0, 30)
        }

        if (tile.tileModel != null) {
            tile.tileModel!!.draw(modelBuffers, x, y, 0, 31)
        }

        if (tile.floorDecoration != null) {
            tile.floorDecoration!!.entity!!.getModel()
                .draw(modelBuffers, x, y, tile.floorDecoration!!.entity!!.height, LocationType.FLOOR_DECORATION.id)
        }

        if (tile.wall != null) {
            tile.wall!!.entity!!.getModel()
                .draw(modelBuffers, x, y, tile.wall!!.entity!!.height, tile.wall!!.type.id)
            tile.wall!!.entity2?.getModel()
                ?.draw(modelBuffers, x, y, tile.wall!!.entity2!!.height, tile.wall!!.type.id)
        }

        if (tile.wallDecoration != null) {
            tile.wallDecoration!!.entity!!.getModel().draw(
                modelBuffers,
                x,
                y,
                tile.wallDecoration!!.entity!!.height,
                LocationType.INTERACTABLE_WALL_DECORATION.id
            )
        }

        for (gameObject in tile.gameObjects) {
            gameObject.entity!!.getModel()
                .draw(modelBuffers, x, y, gameObject.entity.height, LocationType.INTERACTABLE.id)
        }
    }

    private fun uploadScene() {
        modelBuffers.clearVertUv()
        sceneUploader.upload(scene, modelBuffers.vertexBuffer, modelBuffers.uvBuffer)
        modelBuffers.flipVertUv()
        val vertexBuffer: IntBuffer = modelBuffers.vertexBuffer.buffer
        val uvBuffer: FloatBuffer = modelBuffers.uvBuffer.buffer

        println("vertexBuffer size %d".format(vertexBuffer.limit()))
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId)
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            vertexBuffer.limit() * GLBuffers.SIZEOF_INT.toLong(),
            vertexBuffer,
            GL2ES3.GL_STATIC_COPY
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, uvBufferId)
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            uvBuffer.limit() * GLBuffers.SIZEOF_FLOAT.toLong(),
            uvBuffer,
            GL2ES3.GL_STATIC_COPY
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0)
        modelBuffers.clearVertUv()
        drawTiles()
        isSceneUploadRequired = false
    }

    override fun dispose(drawable: GLAutoDrawable?) {
        shutdownBuffers()
        shutdownProgram()
        shutdownVao()
        shutdownPickerFbo()
        shutdownAAFbo()
    }

    @Throws(ShaderException::class)
    private fun initProgram() {
        val template = Template()
        template.addInclude(Shader::class.java)

        glProgram = Shader.PROGRAM.compile(gl, template)
        glComputeProgram = Shader.COMPUTE_PROGRAM.compile(gl, template)
        glSmallComputeProgram = Shader.SMALL_COMPUTE_PROGRAM.compile(gl, template)
        glUnorderedComputeProgram = Shader.UNORDERED_COMPUTE_PROGRAM.compile(gl, template)

        initUniforms()
    }

    private fun initUniforms() {
        uniProjectionMatrix = gl.glGetUniformLocation(glProgram, "projectionMatrix")
        uniBrightness = gl.glGetUniformLocation(glProgram, "brightness")
        uniSmoothBanding = gl.glGetUniformLocation(glProgram, "smoothBanding")
        uniDrawDistance = gl.glGetUniformLocation(glProgram, "drawDistance")
        uniTextures = gl.glGetUniformLocation(glProgram, "textures")
        uniTextureOffsets = gl.glGetUniformLocation(glProgram, "textureOffsets")
        uniBlockSmall = gl.glGetUniformBlockIndex(glSmallComputeProgram, "uniforms")
        uniBlockLarge = gl.glGetUniformBlockIndex(glComputeProgram, "uniforms")
        uniBlockMain = gl.glGetUniformBlockIndex(glProgram, "uniforms")
        uniHoverId = gl.glGetUniformLocation(glProgram, "hoverId")
        uniMouseCoordsId = gl.glGetUniformLocation(glProgram, "mouseCoords")
    }

    private fun initUniformBuffer() {
        uniformBufferId = glGenBuffers(gl)
        gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, uniformBufferId)
        uniformBuffer.clear()
        uniformBuffer.put(IntArray(9))
        uniformBuffer.flip()
        gl.glBufferData(
            GL2ES3.GL_UNIFORM_BUFFER,
            uniformBuffer.limit() * Integer.BYTES.toLong(),
            uniformBuffer,
            GL.GL_DYNAMIC_DRAW
        )
        gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, 0)
    }

    private fun initPickerBuffer() {
        fboMainRenderer = GLUtil.glGenFrameBuffer(gl)
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fboMainRenderer)

        // create depth buffer
        rboDepthMain = GLUtil.glGenRenderbuffer(gl)
        gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, rboDepthMain)
        gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, GL2ES2.GL_DEPTH_COMPONENT, canvasWidth, canvasHeight)
        gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_RENDERBUFFER, rboDepthMain)

        // Create color texture
        texColorMain = GLUtil.glGenTexture(gl)
        gl.glBindTexture(GL.GL_TEXTURE_2D, texColorMain)
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, canvasWidth, canvasHeight, 0, GL.GL_RGBA, GL.GL_FLOAT, null)
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST)
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST)
        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, GL.GL_TEXTURE_2D, texColorMain, 0)
        texPickerMain = GLUtil.glGenTexture(gl)
        gl.glBindTexture(GL.GL_TEXTURE_2D, texPickerMain)
        gl.glTexImage2D(
            GL.GL_TEXTURE_2D,
            0,
            GL2ES3.GL_R32I,
            canvasWidth,
            canvasHeight,
            0,
            GL2GL3.GL_BGRA_INTEGER,
            GL2ES2.GL_INT,
            null
        )
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST)
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST)
        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL2ES2.GL_COLOR_ATTACHMENT1, GL.GL_TEXTURE_2D, texPickerMain, 0)

        // init pbo
        gl.glGenBuffers(3, pboIds, 0)
        gl.glBindBuffer(GL2ES3.GL_PIXEL_PACK_BUFFER, pboIds[0])
        gl.glBufferData(GL2ES3.GL_PIXEL_PACK_BUFFER, GLBuffers.SIZEOF_INT.toLong(), null, GL2ES3.GL_STREAM_READ)
        gl.glBindBuffer(GL2ES3.GL_PIXEL_PACK_BUFFER, pboIds[1])
        gl.glBufferData(GL2ES3.GL_PIXEL_PACK_BUFFER, GLBuffers.SIZEOF_INT.toLong(), null, GL2ES3.GL_STREAM_READ)
        gl.glBindBuffer(GL2ES3.GL_PIXEL_PACK_BUFFER, pboIds[2])
        gl.glBufferData(GL2ES3.GL_PIXEL_PACK_BUFFER, GLBuffers.SIZEOF_INT.toLong(), null, GL2ES3.GL_STREAM_READ)
        gl.glBindBuffer(GL2ES3.GL_PIXEL_PACK_BUFFER, 0)
        val status = gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER)
        if (status != GL.GL_FRAMEBUFFER_COMPLETE) {
            println("bad picker fbo")
        }
        gl.glBindTexture(GL.GL_TEXTURE_2D, 0)
        gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, 0)
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0)
    }

    private fun initAAFbo(width: Int, height: Int, aaSamples: Int) {
        // Create and bind the FBO
        fboSceneHandle = GLUtil.glGenFrameBuffer(gl)
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fboSceneHandle)

        // Create color render buffer
        rboSceneHandle = GLUtil.glGenRenderbuffer(gl)
        gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, rboSceneHandle)
        gl.glRenderbufferStorageMultisample(GL.GL_RENDERBUFFER, aaSamples, GL.GL_RGBA, width, height)
        gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, GL.GL_RENDERBUFFER, rboSceneHandle)

        // Create color texture
        colorTexSceneHandle = GLUtil.glGenTexture(gl)
        gl.glBindTexture(GL2ES2.GL_TEXTURE_2D_MULTISAMPLE, colorTexSceneHandle)
        gl.glTexImage2DMultisample(GL2ES2.GL_TEXTURE_2D_MULTISAMPLE, aaSamples, GL.GL_RGBA, width, height, true)
        // Bind color tex
        gl.glFramebufferTexture2D(
            GL.GL_FRAMEBUFFER,
            GL.GL_COLOR_ATTACHMENT0,
            GL2ES2.GL_TEXTURE_2D_MULTISAMPLE,
            colorTexSceneHandle,
            0
        )

        // Create depth texture
        depthTexSceneHandle = GLUtil.glGenTexture(gl)
        gl.glBindTexture(GL2ES2.GL_TEXTURE_2D_MULTISAMPLE, depthTexSceneHandle)
        gl.glTexImage2DMultisample(
            GL2ES2.GL_TEXTURE_2D_MULTISAMPLE,
            aaSamples,
            GL2ES2.GL_DEPTH_COMPONENT,
            width,
            height,
            true
        )
        // bind depth tex
        gl.glFramebufferTexture2D(
            GL.GL_FRAMEBUFFER,
            GL.GL_DEPTH_ATTACHMENT,
            GL2ES2.GL_TEXTURE_2D_MULTISAMPLE,
            depthTexSceneHandle,
            0
        )

        // Create picker texture
        val texPickerHandle = GLUtil.glGenTexture(gl)
        gl.glBindTexture(GL2ES2.GL_TEXTURE_2D_MULTISAMPLE, texPickerHandle)
        gl.glTexImage2DMultisample(GL2ES2.GL_TEXTURE_2D_MULTISAMPLE, aaSamples, GL2ES3.GL_R32I, width, height, true)
        // Bind color tex
        gl.glFramebufferTexture2D(
            GL.GL_FRAMEBUFFER,
            GL2ES2.GL_COLOR_ATTACHMENT1,
            GL2ES2.GL_TEXTURE_2D_MULTISAMPLE,
            texPickerHandle,
            0
        )
        val status = gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER)
        if (status != GL.GL_FRAMEBUFFER_COMPLETE) {
            println("bad aaPicker fbo")
        }

        // Reset
        gl.glBindTexture(GL2ES2.GL_TEXTURE_2D_MULTISAMPLE, 0)
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0)
        gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, 0)
    }

    private fun initBuffers() {
        bufferId = glGenBuffers(gl)
        uvBufferId = glGenBuffers(gl)
        tmpBufferId = glGenBuffers(gl)
        tmpUvBufferId = glGenBuffers(gl)
        tmpModelBufferId = glGenBuffers(gl)
        tmpModelBufferSmallId = glGenBuffers(gl)
        tmpModelBufferUnorderedId = glGenBuffers(gl)
        tmpOutBufferId = glGenBuffers(gl)
        tmpOutUvBufferId = glGenBuffers(gl)
        colorPickerBufferId = glGenBuffers(gl)
        animFrameBufferId = glGenBuffers(gl)
        selectedIdsBufferId = glGenBuffers(gl)
    }

    private fun initVao() {
        // Create VAO
        vaoHandle = glGenVertexArrays(gl)
        gl.glBindVertexArray(vaoHandle)
        gl.glEnableVertexAttribArray(0)
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, tmpOutBufferId)
        gl.glVertexAttribIPointer(0, 4, GL2ES2.GL_INT, 0, 0)
        gl.glEnableVertexAttribArray(1)
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, tmpOutUvBufferId)
        gl.glVertexAttribPointer(1, 4, GL.GL_FLOAT, false, 0, 0)
        gl.glEnableVertexAttribArray(2)
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, colorPickerBufferId)
        gl.glVertexAttribIPointer(2, 1, GL2ES2.GL_INT, 0, 0)

//        gl.glEnableVertexAttribArray(3);
//        gl.glBindBuffer(gl.GL_ARRAY_BUFFER, animFrameBufferId);
//        gl.glVertexAttribIPointer(3, 4, gl.GL_INT, 0, 0);

        // unbind VBO
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0)
        gl.glBindVertexArray(0)
    }

    private fun createProjectionMatrix(
        left: Float,
        right: Float,
        bottom: Float,
        top: Float,
        near: Float,
        far: Float
    ) {
        // create a standard orthographic projection
        val tx = -((right + left) / (right - left))
        val ty = -((top + bottom) / (top - bottom))
        val tz = -((far + near) / (far - near))

        gl.glUseProgram(glProgram)
        val matrix = floatArrayOf(
            2 / (right - left), 0f, 0f, 0f, 0f, 2 / (top - bottom), 0f, 0f, 0f, 0f, -2 / (far - near), 0f,
            tx, ty, tz, 1f
        )
        gl.glUniformMatrix4fv(uniProjectionMatrix, 1, false, matrix, 0)
        gl.glUseProgram(0)
    }

    private fun shutdownBuffers() {
        if (bufferId != -1) {
            glDeleteBuffer(gl, bufferId)
            bufferId = -1
        }
        if (uvBufferId != -1) {
            glDeleteBuffer(gl, uvBufferId)
            uvBufferId = -1
        }
        if (tmpBufferId != -1) {
            glDeleteBuffer(gl, tmpBufferId)
            tmpBufferId = -1
        }
        if (tmpUvBufferId != -1) {
            glDeleteBuffer(gl, tmpUvBufferId)
            tmpUvBufferId = -1
        }
        if (tmpModelBufferId != -1) {
            glDeleteBuffer(gl, tmpModelBufferId)
            tmpModelBufferId = -1
        }
        if (tmpModelBufferSmallId != -1) {
            glDeleteBuffer(gl, tmpModelBufferSmallId)
            tmpModelBufferSmallId = -1
        }
        if (tmpModelBufferUnorderedId != -1) {
            glDeleteBuffer(gl, tmpModelBufferUnorderedId)
            tmpModelBufferUnorderedId = -1
        }
        if (tmpOutBufferId != -1) {
            glDeleteBuffer(gl, tmpOutBufferId)
            tmpOutBufferId = -1
        }
        if (tmpOutUvBufferId != -1) {
            glDeleteBuffer(gl, tmpOutUvBufferId)
            tmpOutUvBufferId = -1
        }
        if (colorPickerBufferId != -1) {
            glDeleteBuffer(gl, colorPickerBufferId)
            colorPickerBufferId = -1
        }
        if (animFrameBufferId != -1) {
            glDeleteBuffer(gl, animFrameBufferId)
            animFrameBufferId = -1
        }
        if (selectedIdsBufferId != -1) {
            glDeleteBuffer(gl, selectedIdsBufferId)
            selectedIdsBufferId = -1
        }
    }

    private fun shutdownProgram() {
        gl.glDeleteProgram(glProgram)
        glProgram = -1
        gl.glDeleteProgram(glComputeProgram)
        glComputeProgram = -1
        gl.glDeleteProgram(glSmallComputeProgram)
        glSmallComputeProgram = -1
        gl.glDeleteProgram(glUnorderedComputeProgram)
        glUnorderedComputeProgram = -1
    }

    private fun shutdownVao() {
        glDeleteVertexArrays(gl, vaoHandle)
        vaoHandle = -1
    }

    private fun shutdownAAFbo() {
        if (colorTexSceneHandle != -1) {
            glDeleteTexture(gl, colorTexSceneHandle)
            colorTexSceneHandle = -1
        }
        if (depthTexSceneHandle != -1) {
            glDeleteTexture(gl, depthTexSceneHandle)
            depthTexSceneHandle = -1
        }
        if (fboSceneHandle != -1) {
            glDeleteFrameBuffer(gl, fboSceneHandle)
            fboSceneHandle = -1
        }
        if (rboSceneHandle != -1) {
            glDeleteRenderbuffers(gl, rboSceneHandle)
            rboSceneHandle = -1
        }
    }

    private fun shutdownPickerFbo() {
        if (texPickerMain != -1) {
            glDeleteTexture(gl, texPickerMain)
            texPickerMain = -1
        }
        if (texColorMain != -1) {
            glDeleteTexture(gl, texColorMain)
            texColorMain = -1
        }
        if (fboMainRenderer != -1) {
            glDeleteFrameBuffer(gl, fboMainRenderer)
            fboMainRenderer = -1
        }
        if (rboDepthMain != -1) {
            glDeleteFrameBuffer(gl, rboDepthMain)
            rboDepthMain = -1
        }
        glDeleteBuffers(gl, pboIds)
    }
}