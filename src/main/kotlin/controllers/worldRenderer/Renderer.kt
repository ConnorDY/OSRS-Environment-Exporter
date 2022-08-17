package controllers.worldRenderer

import cache.LocationType
import cache.utils.ColorPalette
import controllers.worldRenderer.helpers.AlphaMode
import controllers.worldRenderer.helpers.Animator
import controllers.worldRenderer.helpers.AntiAliasingMode
import controllers.worldRenderer.helpers.GpuFloatBuffer
import controllers.worldRenderer.helpers.GpuIntBuffer
import controllers.worldRenderer.shaders.Shader
import controllers.worldRenderer.shaders.ShaderException
import models.DebugModel
import models.DebugOptionsModel
import models.config.ConfigOptions
import models.scene.REGION_HEIGHT
import models.scene.REGION_SIZE
import models.scene.Scene
import models.scene.SceneTile
import org.joml.Matrix4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL.createCapabilities
import org.lwjgl.opengl.GL11C.GL_BLEND
import org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT
import org.lwjgl.opengl.GL11C.GL_DEPTH_TEST
import org.lwjgl.opengl.GL11C.GL_LEQUAL
import org.lwjgl.opengl.GL11C.GL_NEAREST
import org.lwjgl.opengl.GL11C.GL_ONE
import org.lwjgl.opengl.GL11C.GL_ONE_MINUS_SRC_ALPHA
import org.lwjgl.opengl.GL11C.GL_RGBA
import org.lwjgl.opengl.GL11C.GL_SRC_ALPHA
import org.lwjgl.opengl.GL11C.glClear
import org.lwjgl.opengl.GL11C.glClearColor
import org.lwjgl.opengl.GL11C.glDepthFunc
import org.lwjgl.opengl.GL11C.glDepthRange
import org.lwjgl.opengl.GL11C.glDisable
import org.lwjgl.opengl.GL11C.glEnable
import org.lwjgl.opengl.GL11C.glGetInteger
import org.lwjgl.opengl.GL11C.glViewport
import org.lwjgl.opengl.GL13C.GL_MULTISAMPLE
import org.lwjgl.opengl.GL14C.GL_DEPTH_COMPONENT16
import org.lwjgl.opengl.GL14C.glBlendFuncSeparate
import org.lwjgl.opengl.GL15C.GL_DYNAMIC_DRAW
import org.lwjgl.opengl.GL15C.glBindBuffer
import org.lwjgl.opengl.GL15C.glBufferData
import org.lwjgl.opengl.GL15C.glBufferSubData
import org.lwjgl.opengl.GL15C.glGenBuffers
import org.lwjgl.opengl.GL20C.glDeleteProgram
import org.lwjgl.opengl.GL20C.glDrawBuffers
import org.lwjgl.opengl.GL20C.glGetUniformLocation
import org.lwjgl.opengl.GL20C.glUniform1f
import org.lwjgl.opengl.GL20C.glUniform1i
import org.lwjgl.opengl.GL20C.glUniform2fv
import org.lwjgl.opengl.GL20C.glUniform2i
import org.lwjgl.opengl.GL20C.glUniformMatrix4fv
import org.lwjgl.opengl.GL20C.glUseProgram
import org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT1
import org.lwjgl.opengl.GL30C.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.GL30C.GL_DRAW_FRAMEBUFFER
import org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL30C.GL_MAX_SAMPLES
import org.lwjgl.opengl.GL30C.GL_READ_FRAMEBUFFER
import org.lwjgl.opengl.GL30C.GL_RENDERBUFFER
import org.lwjgl.opengl.GL30C.glBindBufferBase
import org.lwjgl.opengl.GL30C.glBindFramebuffer
import org.lwjgl.opengl.GL30C.glBindRenderbuffer
import org.lwjgl.opengl.GL30C.glBlitFramebuffer
import org.lwjgl.opengl.GL30C.glDeleteFramebuffers
import org.lwjgl.opengl.GL30C.glDeleteRenderbuffers
import org.lwjgl.opengl.GL30C.glFramebufferRenderbuffer
import org.lwjgl.opengl.GL30C.glGenFramebuffers
import org.lwjgl.opengl.GL30C.glGenRenderbuffers
import org.lwjgl.opengl.GL30C.glRenderbufferStorageMultisample
import org.lwjgl.opengl.GL30C.glUniform1ui
import org.lwjgl.opengl.GL31C.GL_UNIFORM_BUFFER
import org.lwjgl.opengl.GL31C.glGetUniformBlockIndex
import org.lwjgl.opengl.GL31C.glUniformBlockBinding
import org.lwjgl.opengl.GL40C.GL_SAMPLE_SHADING
import org.lwjgl.opengl.GL40C.glMinSampleShading
import org.lwjgl.opengl.awt.AWTGLCanvas
import org.lwjgl.opengl.awt.GLData
import org.slf4j.LoggerFactory
import java.awt.event.ActionListener
import java.nio.IntBuffer
import kotlin.math.min

class Renderer(
    private val camera: Camera,
    private val scene: Scene,
    private val sceneUploader: SceneUploader,
    private val textureManager: TextureManager,
    private val configOptions: ConfigOptions,
    private val debugModel: DebugModel,
    private val debugOptionsModel: DebugOptionsModel,
) {
    enum class PreferredPriorityRenderer(val humanReadableName: String, val factory: () -> PriorityRenderer) {
        GLSL("GLSL Compute-based priority renderer", { GLSLPriorityRenderer() }),
        CPU_NAIVE("No priority renderer", { CPUNonPriorityRenderer() }),
    }

    private val logger = LoggerFactory.getLogger(Renderer::class.java)

    var antiAliasingMode: AntiAliasingMode = configOptions.antiAliasing.value.get()
    var priorityRendererPref: PreferredPriorityRenderer = configOptions.priorityRenderer.value.get()
        set(value) {
            if (field != value) {
                field = value
                changePriorityRenderer()
            }
        }

    private var glProgram = 0

    private var fboSceneHandle = -1
    private var rboSceneHandle = -1
    private var rboSceneDepthBuffer = -1

    private var textureArrayId = 0
    private var uniformBufferId = 0
    private val uniformBuffer: IntBuffer = GpuIntBuffer.allocateDirect(5 + 3 + 1)
    private val textureOffsets = FloatArray(256)

    private lateinit var priorityRenderer: PriorityRenderer

    var zLevelsSelected = Array(REGION_HEIGHT) { true }

    // Uniforms
    private var uniDrawDistance = 0
    private var uniViewProjectionMatrix = 0
    private var uniBrightness = 0
    private var uniTextures = 0
    private var uniTextureOffsets = 0
    private var uniBlockMain = 0
    private var uniSmoothBanding = 0
    private var uniMouseCoordsId = 0
    private var uniAlphaMode = -1
    private var uniHashSeed = -1

    var canvasWidth = 100
    var canvasHeight = (canvasWidth / 1.3).toInt()
    private var lastStretchedCanvasWidth = 0
    private var lastStretchedCanvasHeight = 0
    private var lastAntiAliasingMode: AntiAliasingMode? = null

    private var animator: Animator? = null
    private var glCanvas: AWTGLCanvas? = null
    private lateinit var inputHandler: InputHandler

    private val pendingGlThreadActions = ArrayList<Runnable>()

    fun initCanvas(): AWTGLCanvas {
        // center camera in viewport
        camera.centerX = canvasWidth / 2
        camera.centerY = canvasHeight / 2

        fboSceneHandle = -1
        rboSceneHandle = -1
        rboSceneDepthBuffer = -1

        val glData = GLData()
        glData.majorVersion = 3
        glData.minorVersion = 1
        glData.profile = GLData.Profile.CORE

        val glCanvas = object : AWTGLCanvas(glData) {
            override fun initGL() {
                this@Renderer.init()
            }

            override fun paintGL() {
                this@Renderer.reshape(0, 0, width, height)
                if (width > 0 && height > 0)
                    this@Renderer.display(this)
            }

            override fun disposeCanvas() {
                super.disposeCanvas()
                this@Renderer.dispose()
            }
        }

        inputHandler = InputHandler(glCanvas, camera, scene, configOptions)
        glCanvas.addKeyListener(inputHandler)
        glCanvas.addMouseListener(inputHandler)
        glCanvas.addMouseMotionListener(inputHandler)

        lastStretchedCanvasHeight = -1
        lastStretchedCanvasWidth = -1
        lastAntiAliasingMode = null
        textureArrayId = -1

        scene.sceneChangeListeners.add(
            ActionListener {
                isSceneUploadRequired = true

                if (debugOptionsModel.resetCameraOnSceneChange.value.get()) {
                    camera.cameraX = Constants.LOCAL_HALF_TILE_SIZE * scene.cols * REGION_SIZE
                    camera.cameraY = Constants.LOCAL_HALF_TILE_SIZE * scene.rows * REGION_SIZE
                    camera.cameraZ = -2500
                }
            }
        )

        animator = Animator(glCanvas)
        this.glCanvas = glCanvas

        configOptions.fpsCap.value.addListener {
            setFpsTarget(it ?: 0)
        }
        setFpsTarget(configOptions.fpsCap.value.get() ?: 0)

        configOptions.priorityRenderer.value.addListener(::priorityRendererPref::set)
        configOptions.antiAliasing.value.addListener(::antiAliasingMode::set)

        val redrawSceneListener: (Any?) -> Unit = {
            isSceneUploadRequired = true
        }
        debugOptionsModel.showTilePaint.value.addListener(redrawSceneListener)
        debugOptionsModel.showTileModels.value.addListener(redrawSceneListener)
        debugOptionsModel.showOnlyModelType.value.addListener(redrawSceneListener)

        return glCanvas
    }

    fun start() {
        animator?.start()
    }

    fun stop() {
        animator?.stop()
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
        doInGlThread {
            priorityRenderer.destroy()
            priorityRenderer = priorityRendererPref.factory()
            isSceneUploadRequired = true
        }
    }

    private fun doInGlThread(thing: Runnable) {
        pendingGlThreadActions.add(thing)
    }

    fun reshape(x: Int, y: Int, width: Int, height: Int) {
        canvasWidth = width
        canvasHeight = height
        camera.centerX = canvasWidth / 2
        camera.centerY = canvasHeight / 2
        glViewport(x, y, width, height)
    }

    var isSceneUploadRequired = true
    private val clientStart = System.currentTimeMillis()
    private var lastUpdate = System.nanoTime()

    fun display(glCanvas: AWTGLCanvas) {
        val animator = animator
        if (animator != null) {
            debugModel.fps.set("%.0f".format(animator.lastFPS))
        }

        pendingGlThreadActions.forEach(Runnable::run)
        pendingGlThreadActions.clear()

        if (isSceneUploadRequired) {
            uploadScene()
        }

        val thisUpdate = System.nanoTime()
        val deltaTime = (thisUpdate - lastUpdate).toDouble() / 1_000_000
        lastUpdate = thisUpdate

        inputHandler.tick(deltaTime)

        // Setup anti-aliasing
        val aaEnabled = antiAliasingMode !== AntiAliasingMode.DISABLED
        if (aaEnabled) {
            glEnable(GL_MULTISAMPLE)
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
            val stretchedCanvasWidth = canvasWidth
            val stretchedCanvasHeight = canvasHeight

            // Re-create fbo
            if (lastStretchedCanvasWidth != stretchedCanvasWidth || lastStretchedCanvasHeight != stretchedCanvasHeight || lastAntiAliasingMode !== antiAliasingMode
            ) {
                val maxSamples: Int = glGetInteger(GL_MAX_SAMPLES)
                val samples = min(antiAliasingMode.ordinal, maxSamples)
                initAAFbo(stretchedCanvasWidth, stretchedCanvasHeight, samples)
                lastStretchedCanvasWidth = stretchedCanvasWidth
                lastStretchedCanvasHeight = stretchedCanvasHeight
            }
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fboSceneHandle)
        } else {
            glDisable(GL_MULTISAMPLE)
            shutdownAAFbo()
        }
        lastAntiAliasingMode = antiAliasingMode
        // TODO: was this necessary?
        val i = BufferUtils.createIntBuffer(2).put(GL_COLOR_ATTACHMENT0).put(GL_COLOR_ATTACHMENT1).flip()
        glDrawBuffers(i)

        // Clear scene
        val sky = 9493480
        glClearColor((sky shr 16 and 0xFF) / 255f, (sky shr 8 and 0xFF) / 255f, (sky and 0xFF) / 255f, 1f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        val clientCycle = ((System.currentTimeMillis() - clientStart) / 20).toInt() // 50 fps

        priorityRenderer.produceVertices(camera, clientCycle)

        if (textureArrayId == -1) {
            // lazy init textures as they may not be loaded at plugin start.
            // this will return -1 and retry if not all textures are loaded yet, too.
            textureArrayId = textureManager.initTextureArray()
        }
//        val textures: Array<TextureDefinition> = textureProvider.getTextureDefinitions()
        glUseProgram(glProgram)
        // Brightness happens to also be stored in the texture provider, so we use that
        glUniform1f(uniBrightness, ColorPalette.BRIGHTNESS_HIGH.toFloat()) // (float) textureProvider.getBrightness());
        glUniform1i(uniDrawDistance, Constants.MAX_DISTANCE * Constants.LOCAL_TILE_SIZE)
        glUniform1f(uniSmoothBanding, 1f)
        glUniform1ui(uniHashSeed, camera.motionTicks)
        glUniformMatrix4fv(uniViewProjectionMatrix, false, calculateViewProjectionMatrix())

        // This is just for animating!
//        for (int id = 0; id < textures.length; ++id) {
//            TextureDefinition texture = textures[id];
//            if (texture == null) {
//                continue;
//            }
//
//            textureProvider.load(id); // trips the texture load flag which lets textures animate
//
//            textureOffsets[id * 2] = texture.field1782;
//            textureOffsets[id * 2 + 1] = texture.field1783;
//        }
        glUniform2i(uniMouseCoordsId, inputHandler.mouseX, canvasHeight - inputHandler.mouseY)

        // UBO
        glBindBuffer(GL_UNIFORM_BUFFER, uniformBufferId)
        uniformBuffer.clear()
        uniformBuffer
            .put(camera.yaw)
            .put(camera.pitch)
            .put(camera.centerX)
            .put(camera.centerY)
            .put(camera.scale)
            .put(camera.cameraX) // x
            .put(camera.cameraZ) // z
            .put(camera.cameraY) // y
            .put(clientCycle) // currFrame
        uniformBuffer.flip()
        glBufferSubData(
            GL_UNIFORM_BUFFER,
            0,
            uniformBuffer
        )
        glBindBuffer(GL_UNIFORM_BUFFER, 0)

        // Bind uniforms
        glBindBufferBase(GL_UNIFORM_BUFFER, 0, uniformBufferId)
        glUniformBlockBinding(glProgram, uniBlockMain, 0)
        glUniform1i(uniTextures, 1) // texture sampler array is bound to texture1
        glUniform2fv(uniTextureOffsets, textureOffsets)

        val useBlend = configOptions.alphaMode.value.get() == AlphaMode.BLEND
        if (useBlend) {
            glEnable(GL_BLEND)
            glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE)
        }

        priorityRenderer.draw()

        if (useBlend) glDisable(GL_BLEND)
        glUseProgram(0)

        if (aaEnabled) {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, fboSceneHandle)
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0)
            glBlitFramebuffer(
                0, 0, lastStretchedCanvasWidth, lastStretchedCanvasHeight,
                0, 0, lastStretchedCanvasWidth, lastStretchedCanvasHeight,
                GL_COLOR_BUFFER_BIT, GL_NEAREST
            )
        }

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
        zLevelsSelected.forEachIndexed { z, visible ->
            if (visible) {
                for (x in 0 until scene.cols * REGION_SIZE) {
                    for (y in 0 until scene.rows * REGION_SIZE) {
                        scene.getTile(z, x, y)?.let { drawTile(it) }
                    }
                }
            }
        }
    }

    private fun drawTile(tile: SceneTile) {
        val x: Int = tile.x
        val y: Int = tile.y
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
                LocationType.INTERACTABLE_WALL_DECORATION.id
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

    fun exportScene() {
        SceneExporter(textureManager, debugOptionsModel).exportSceneToFile(scene, this)
    }

    private fun uploadScene() {
        priorityRenderer.beginUploading()

        try {
            sceneUploader.upload(scene, priorityRenderer)
        } catch (e: Exception) {
            e.printStackTrace()
            logger.warn("Error happened while rendering with {}", priorityRenderer)
        }

        priorityRenderer.finishUploading()

        drawTiles()
        priorityRenderer.finishPositioning()

        isSceneUploadRequired = false
    }

    fun dispose() {
        shutdownProgram()
        shutdownAAFbo()
        priorityRenderer.destroy()
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

    private fun initUniforms() {
        uniViewProjectionMatrix = glGetUniformLocation(glProgram, "viewProjectionMatrix")
        uniBrightness = glGetUniformLocation(glProgram, "brightness")
        uniSmoothBanding = glGetUniformLocation(glProgram, "smoothBanding")
        uniDrawDistance = glGetUniformLocation(glProgram, "drawDistance")
        uniTextures = glGetUniformLocation(glProgram, "textures")
        uniTextureOffsets = glGetUniformLocation(glProgram, "textureOffsets")
        uniBlockMain = glGetUniformBlockIndex(glProgram, "uniforms")
        uniMouseCoordsId = glGetUniformLocation(glProgram, "mouseCoords")
        uniAlphaMode = glGetUniformLocation(glProgram, "alphaMode")
        uniHashSeed = glGetUniformLocation(glProgram, "hashSeed")
    }

    private fun initUniformBuffer() {
        uniformBufferId = glGenBuffers()
        glBindBuffer(GL_UNIFORM_BUFFER, uniformBufferId)
        uniformBuffer.clear()
        uniformBuffer.put(IntArray(9))
        uniformBuffer.flip()
        glBufferData(GL_UNIFORM_BUFFER, uniformBuffer, GL_DYNAMIC_DRAW)
        glBindBuffer(GL_UNIFORM_BUFFER, 0)
    }

    private fun initAAFbo(width: Int, height: Int, aaSamples: Int) {
        // Discard old FBO
        shutdownAAFbo()

        // Create and bind the FBO
        fboSceneHandle = glGenFramebuffers()
        glBindFramebuffer(GL_FRAMEBUFFER, fboSceneHandle)

        // Create color render buffer
        rboSceneHandle = glGenRenderbuffers()
        glBindRenderbuffer(GL_RENDERBUFFER, rboSceneHandle)
        glRenderbufferStorageMultisample(GL_RENDERBUFFER, aaSamples, GL_RGBA, width, height)
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, rboSceneHandle)

        // Create depth buffer
        rboSceneDepthBuffer = glGenRenderbuffers()
        glBindRenderbuffer(GL_RENDERBUFFER, rboSceneDepthBuffer)
        glRenderbufferStorageMultisample(GL_RENDERBUFFER, aaSamples, GL_DEPTH_COMPONENT16, width, height)
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rboSceneDepthBuffer)

        // Reset
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
        glBindRenderbuffer(GL_RENDERBUFFER, 0)
    }

    private fun makeProjectionMatrix(width: Float, height: Float, near: Float): FloatArray =
        floatArrayOf(
            2 / width, 0f, 0f, 0f,
            0f, 2 / height, 0f, 0f,
            0f, 0f, -1f, -1f,
            0f, 0f, -2 * near, 0f,
        )

    private fun calculateViewProjectionMatrix(): FloatArray {
        val projectionMatrix = makeProjectionMatrix(
            canvasWidth.toFloat(),
            canvasHeight.toFloat(),
            50.0f
        )
        return Matrix4f()
            .scale(camera.scale.toFloat(), camera.scale.toFloat(), 1.0f)
            .mul(Matrix4f(GpuFloatBuffer.allocateDirect(projectionMatrix.size).put(projectionMatrix).flip()))
            .rotateX((-Math.PI + camera.pitch * Constants.UNIT).toFloat())
            .rotateY((camera.yaw * Constants.UNIT).toFloat())
            .translate(-camera.cameraX.toFloat(), -camera.cameraZ.toFloat(), -camera.cameraY.toFloat())
            .get(projectionMatrix)
    }

    private fun shutdownProgram() {
        glDeleteProgram(glProgram)
        glProgram = -1
    }

    private fun shutdownAAFbo() {
        if (fboSceneHandle != -1) {
            glDeleteFramebuffers(fboSceneHandle)
            fboSceneHandle = -1
        }
        if (rboSceneHandle != -1) {
            glDeleteRenderbuffers(rboSceneHandle)
            rboSceneHandle = -1
        }
        if (rboSceneDepthBuffer != -1) {
            glDeleteRenderbuffers(rboSceneDepthBuffer)
            rboSceneDepthBuffer = -1
        }
    }
}
