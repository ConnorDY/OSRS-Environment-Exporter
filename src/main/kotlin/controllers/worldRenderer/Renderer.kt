package controllers.worldRenderer

import cache.LocationType
import controllers.worldRenderer.helpers.AlphaMode
import controllers.worldRenderer.helpers.Animator
import controllers.worldRenderer.shaders.Shader
import controllers.worldRenderer.shaders.ShaderException
import models.DebugOptionsModel
import models.FrameRateModel
import models.config.ConfigOptions
import models.scene.REGION_SIZE
import models.scene.Scene
import models.scene.SceneTile
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL.createCapabilities
import org.lwjgl.opengl.GL11C.GL_BLEND
import org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL11C.GL_DEPTH_TEST
import org.lwjgl.opengl.GL11C.GL_LEQUAL
import org.lwjgl.opengl.GL11C.GL_NEAREST
import org.lwjgl.opengl.GL11C.GL_ONE
import org.lwjgl.opengl.GL11C.GL_ONE_MINUS_SRC_ALPHA
import org.lwjgl.opengl.GL11C.GL_SRC_ALPHA
import org.lwjgl.opengl.GL11C.glDepthFunc
import org.lwjgl.opengl.GL11C.glDepthRange
import org.lwjgl.opengl.GL11C.glDisable
import org.lwjgl.opengl.GL11C.glEnable
import org.lwjgl.opengl.GL11C.glViewport
import org.lwjgl.opengl.GL14C.glBlendFuncSeparate
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL20C.glDeleteProgram
import org.lwjgl.opengl.GL20C.glDrawBuffers
import org.lwjgl.opengl.GL20C.glGetUniformLocation
import org.lwjgl.opengl.GL20C.glUniform1i
import org.lwjgl.opengl.GL20C.glUseProgram
import org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT1
import org.lwjgl.opengl.GL30C.GL_DRAW_FRAMEBUFFER
import org.lwjgl.opengl.GL30C.GL_READ_FRAMEBUFFER
import org.lwjgl.opengl.GL30C.glBindFramebuffer
import org.lwjgl.opengl.GL30C.glBlitFramebuffer
import org.lwjgl.opengl.GL40C.GL_SAMPLE_SHADING
import org.lwjgl.opengl.GL40C.glMinSampleShading
import org.lwjgl.opengl.awt.AWTGLCanvas
import org.lwjgl.opengl.awt.GLData
import org.slf4j.LoggerFactory
import ui.CancelledException
import utils.Utils.doAllActions
import utils.Utils.isMacOS
import java.awt.event.ActionListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.tan

class Renderer(
    private val camera: Camera,
    private val scene: Scene,
    private val sceneUploader: SceneUploader,
    private val textureManager: TextureManager,
    private val configOptions: ConfigOptions,
    private val frameRateModel: FrameRateModel,
    private val debugOptionsModel: DebugOptionsModel,
) : RuneliteRenderer(
    antiAliasingMode = configOptions.antiAliasing.value.get()
) {
    enum class PreferredPriorityRenderer(val humanReadableName: String, val factory: () -> PriorityRenderer) {
        GLSL("GLSL Compute-based priority renderer", { GLSLPriorityRenderer() }),
        CPU_NAIVE("No priority renderer", { CPUNonPriorityRenderer() }),
    }

    private val logger = LoggerFactory.getLogger(Renderer::class.java)
    var priorityRendererPref: PreferredPriorityRenderer = configOptions.priorityRenderer.value.get()
        set(value) {
            if (field != value) {
                field = value
                changePriorityRenderer()
            }
        }

    private var textureArrayId = 0

    private lateinit var priorityRenderer: PriorityRenderer

    // Uniforms
    private var uniMouseCoordsId = -1
    private var uniAlphaMode = -1

    var canvasWidth = 100
    var canvasHeight = (canvasWidth / 1.3).toInt()

    private var animator: Animator? = null
    private var glCanvas: AWTGLCanvas? = null
    private lateinit var inputHandler: InputHandler

    private val pendingGlThreadActions = ConcurrentLinkedQueue<Runnable>()
    val sceneDrawListeners = ArrayList<SceneDrawListener>() // TODO concurrent?

    fun initCanvas(): AWTGLCanvas {
        // center camera in viewport
        camera.centerX = canvasWidth / 2
        camera.centerY = canvasHeight / 2

        val glData = GLData()
        glData.majorVersion = 3
        glData.minorVersion = 1
        glData.forwardCompatible = true

        val glCanvas = object : AWTGLCanvas(glData) {
            override fun initGL() {
                this@Renderer.init()
            }

            override fun paintGL() {
                if (isMacOS()) {
                    // MacOS highDPI stuff returns the wrong value for framebuffer width/height
                    this@Renderer.reshape(width, height)
                } else {
                    this@Renderer.reshape(framebufferWidth, framebufferHeight)
                }
                if (width > 0 && height > 0)
                    this@Renderer.display(this)
            }

            override fun disposeCanvas() {
                super.disposeCanvas()
                stop()
            }
        }

        inputHandler = InputHandler(glCanvas, camera, scene, configOptions, frameRateModel)
        glCanvas.addKeyListener(inputHandler)
        glCanvas.addMouseListener(inputHandler)
        glCanvas.addMouseMotionListener(inputHandler)
        glCanvas.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                frameRateModel.notifyNeedFrames()
            }
        })

        frameRateModel.powerSavingMode.addListener {
            frameRateModel.notifyNeedFrames()
        }

        textureArrayId = -1

        scene.sceneChangeListeners.add(
            ActionListener {
                animator?.doOnceBeforeGlRender {
                    queueSceneUpload()
                    shouldResetCamera = true
                }
                frameRateModel.notifyNeedFrames()
            }
        )

        animator = Animator(glCanvas, frameRateModel)
        this.glCanvas = glCanvas

        configOptions.fpsCap.value.addListener {
            setFpsTarget(it ?: 0)
        }
        setFpsTarget(configOptions.fpsCap.value.get() ?: 0)

        configOptions.priorityRenderer.value.addListener(::priorityRendererPref::set)
        configOptions.antiAliasing.value.addListener(::antiAliasingMode::set)

        val redrawSceneListener: (Any?) -> Unit = {
            animator?.doOnceBeforeGlRender {
                queueSceneUpload()
            }
            frameRateModel.notifyNeedFrames()
        }

        debugOptionsModel.zLevelsSelected.forEach { level ->
            level.addListener(redrawSceneListener)
        }

        debugOptionsModel.showTilePaint.value.addListener(redrawSceneListener)
        debugOptionsModel.showTileModels.value.addListener(redrawSceneListener)
        debugOptionsModel.showOnlyModelType.value.addListener(redrawSceneListener)

        doInGlThread {
            // Hack: can't call preDisplay() before any GL stuff is initialised
            // because PriorityRenderer needs to initialise GL buffers
            // So instead do it in the render function first, and before it thereafter.
            // Things are split like this because the render function locks AWT.
            preDisplay()
            animator!!.doBeforeGlRender(::preDisplay)
        }

        return glCanvas
    }

    fun start() {
        animator?.start()
    }

    override fun stop() {
        animator!!.stopWith {
            super.stop()
            shutdownProgram()
            priorityRenderer.destroy()
        }
    }

    fun init() {
        createCapabilities()

        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LEQUAL)
        glDepthRange(0.0, 1.0)

        priorityRenderer = try {
            priorityRendererPref.factory()
        } catch (e: ShaderException) {
            logger.warn("Tried to spawn priority renderer but got exception", e)
            CPUNonPriorityRenderer()
        }
        try {
            priorityRenderer.bindVao()
            initProgram()
        } catch (e: ShaderException) {
            e.printStackTrace()
        }
        initUniformBuffer()
    }

    private fun changePriorityRenderer() {
        val animator = animator ?: return
        animator.doOnceBeforeGlRender {
            forceLockedRender = true
        }
        doInGlThread {
            priorityRenderer.destroy()
            priorityRenderer = priorityRendererPref.factory()
            queueSceneUpload()
        }
        frameRateModel.notifyNeedFrames()
    }

    private fun doInGlThread(thing: Runnable) {
        pendingGlThreadActions.add(thing)
    }

    fun reshape(width: Int, height: Int) {
        canvasWidth = width
        canvasHeight = height
        camera.centerX = canvasWidth / 2
        camera.centerY = canvasHeight / 2
        glViewport(0, 0, width, height)
    }

    private var isSceneUploadRequired = true
    private var forceLockedRender = false
    private var shouldResetCamera = true
    private val clientStart = System.currentTimeMillis()
    private var lastUpdate = System.nanoTime()

    private fun queueSceneUpload() {
        animator!!.checkGlThread()
        isSceneUploadRequired = true
    }

    private fun preDisplay() {
        if (isSceneUploadRequired && !forceLockedRender) {
            uploadSceneCPUHalf()
            // do not unset isSceneUploadRequired here, it will be unset in display()
            // when the scene is uploaded to the GPU
        }
    }

    fun display(glCanvas: AWTGLCanvas) {
        pendingGlThreadActions.doAllActions()

        if (isSceneUploadRequired) {
            if (forceLockedRender) {
                uploadSceneCPUHalf()
                forceLockedRender = false
            }
            uploadSceneGPUHalf()
            if (shouldResetCamera) {
                if (debugOptionsModel.resetCameraOnSceneChange.value.get()) {
                    camera.cameraX = (Constants.LOCAL_HALF_TILE_SIZE * scene.cols * REGION_SIZE).toDouble()
                    camera.cameraY = (Constants.LOCAL_HALF_TILE_SIZE * scene.rows * REGION_SIZE).toDouble()
                    camera.cameraZ = -2500.0
                }
                shouldResetCamera = false
            }
            isSceneUploadRequired = false
        }

        val fov = configOptions.fov.value.get()
        camera.scale = (canvasWidth / (2 * tan(Math.toRadians(fov / 2)))).toInt()

        val thisUpdate = System.nanoTime()
        val deltaTime = (thisUpdate - lastUpdate).toDouble() / 1_000_000
        lastUpdate = thisUpdate

        inputHandler.tick(deltaTime)

        if (isAntiAliasingEnabled()) {
            if (GL.getCapabilities().OpenGL40) {
                if (configOptions.sampleShading.value.get()) {
                    glEnable(GL_SAMPLE_SHADING)
                    glMinSampleShading(1f)
                } else {
                    glDisable(GL_SAMPLE_SHADING)
                }
            } else if (configOptions.sampleShading.value.get()) {
                logger.warn("Cannot use sample shading on OpenGL < 4.0")
                configOptions.sampleShading.value.set(false)
            }
        }
        setupAntiAliasing(canvasWidth, canvasHeight)

        // TODO: was this necessary?
        val i = BufferUtils.createIntBuffer(2).put(GL_COLOR_ATTACHMENT0).put(GL_COLOR_ATTACHMENT1).flip()
        glDrawBuffers(i)

        clearScene()

        val clientCycle = ((System.currentTimeMillis() - clientStart) / 20).toInt() // 50 fps

        priorityRenderer.produceVertices(camera, clientCycle)

        if (textureArrayId == -1) {
            // lazy init textures as they may not be loaded at plugin start.
            // this will return -1 and retry if not all textures are loaded yet, too.
            textureArrayId = textureManager.initTextureArray()
        }
//        val textures: Array<TextureDefinition> = textureProvider.getTextureDefinitions()
        prepareDrawProgram(camera, canvasWidth, canvasHeight, clientCycle)
        GL20C.glUniform2i(uniMouseCoordsId, inputHandler.mouseX, canvasHeight - inputHandler.mouseY)

        val useBlend = configOptions.alphaMode.value.get() == AlphaMode.BLEND
        if (useBlend) {
            glEnable(GL_BLEND)
            glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE)
        }

        priorityRenderer.draw()

        if (useBlend) glDisable(GL_BLEND)
        glUseProgram(0)

        blitAntiAliasing()

        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0)
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0)
        glBlitFramebuffer(
            0, 0, canvasWidth, canvasHeight,
            0, 0, canvasWidth, canvasHeight,
            GL_COLOR_BUFFER_BIT, GL_NEAREST
        )
        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0)

        glCanvas.swapBuffers()
    }

    /** Sets the FPS target for this renderer.
     *  It may vary above and below the actual value.
     *  @param target The FPS target, or 0 for unlimited.
     */
    fun setFpsTarget(target: Int) {
        animator?.setFpsTarget(target)
    }

    private fun drawTiles() {
        debugOptionsModel.zLevelsSelected.forEachIndexed { z, visible ->
            if (visible.get()) {
                for (x in 0 until scene.cols * REGION_SIZE) {
                    for (y in 0 until scene.rows * REGION_SIZE) {
                        scene.getTile(z, x, y)?.let { drawTile(it, x, y) }
                    }
                }
            }
        }
    }

    private fun drawTile(tile: SceneTile, x: Int, y: Int) {
        val tilePaint = tile.tilePaint
        if (tilePaint != null) {
            priorityRenderer.positionRenderable(
                tilePaint,
                x,
                y,
                0,
                LocationType.TILE_PAINT.id
            )
        }

        val tileModel = tile.tileModel
        if (tileModel != null) {
            priorityRenderer.positionRenderable(
                tileModel,
                x,
                y,
                0,
                LocationType.TILE_MODEL.id
            )
        }

        val floorDecorationEntity = tile.floorDecoration?.entity
        if (floorDecorationEntity != null) {
            priorityRenderer.positionRenderable(
                floorDecorationEntity.model,
                x,
                y,
                floorDecorationEntity.height,
                LocationType.FLOOR_DECORATION.id
            )
        }

        val wall = tile.wall
        if (wall != null) {
            priorityRenderer.positionRenderable(
                wall.entity.model,
                x,
                y,
                wall.entity.height,
                wall.type.id
            )
            if (wall.entity2 != null) {
                priorityRenderer.positionRenderable(
                    wall.entity2.model,
                    x,
                    y,
                    wall.entity2.height,
                    wall.type.id
                )
            }
        }

        val wallDecorationEntity = tile.wallDecoration?.entity
        if (wallDecorationEntity != null) {
            priorityRenderer.positionRenderable(
                wallDecorationEntity.model,
                x,
                y,
                wallDecorationEntity.height,
                LocationType.INSIDE_WALL_DECORATION.id
            )
        }

        val wallDecorationEntity2 = tile.wallDecoration?.entity2
        if (wallDecorationEntity2 != null) {
            priorityRenderer.positionRenderable(
                wallDecorationEntity2.model,
                x,
                y,
                wallDecorationEntity2.height,
                LocationType.INSIDE_WALL_DECORATION.id
            )
        }

        for (gameObject in tile.gameObjects) {
            priorityRenderer.positionRenderable(
                gameObject.entity.model,
                x,
                y,
                gameObject.entity.height,
                LocationType.INTERACTABLE.id
            )
        }
    }

    private fun uploadSceneCPUHalf() {
        priorityRenderer.beginUploading()

        try {
            try {
                sceneUploader.upload(scene, priorityRenderer)
            } catch (e: CancelledException) {
                throw e // rethrow to cancel the whole upload
            } catch (e: Exception) {
                logger.warn("Error happened while rendering with $priorityRenderer", e)
                sceneDrawListeners.forEach { it.onError(e) }
            }

            sceneDrawListeners.forEach(SceneDrawListener::onStartDraw)
            drawTiles()
            sceneDrawListeners.forEach(SceneDrawListener::onEndDraw)
        } catch (e: CancelledException) {
            // Do nothing
        } catch (e: Exception) {
            logger.error("Error happened while positioning scene", e)
            sceneDrawListeners.forEach { it.onError(e) }
        }
    }

    private fun uploadSceneGPUHalf() {
        priorityRenderer.finishUploading()
        priorityRenderer.finishPositioning()
    }

    @Throws(ShaderException::class)
    private fun initProgram() {
        val template = Shader.createTemplate(-1, -1)
        template.addInclude(Shader::class.java)

        try {
            glProgram = Shader.PROGRAM.value.compile(template)
        } catch (e: ShaderException) {
            // This will likely destroy the renderer, but the rest of the program should
            // still be usable.
            e.printStackTrace()
        }

        initUniforms()
        updateAlphaMode(configOptions.alphaMode.value.get())
        configOptions.alphaMode.value.addListener {
            doInGlThread {
                updateAlphaMode(it)
            }
        }
    }

    private fun updateAlphaMode(mode: AlphaMode) {
        if (glProgram == -1 || uniAlphaMode == -1) return

        glUseProgram(glProgram)
        glUniform1i(uniAlphaMode, mode.internalId)
        glUseProgram(0)
    }

    override fun initUniforms() {
        super.initUniforms()
        uniMouseCoordsId = GL20C.glGetUniformLocation(glProgram, "mouseCoords")
        uniAlphaMode = glGetUniformLocation(glProgram, "alphaMode")
    }

    private fun shutdownProgram() {
        glDeleteProgram(glProgram)
        glProgram = -1
    }
}
