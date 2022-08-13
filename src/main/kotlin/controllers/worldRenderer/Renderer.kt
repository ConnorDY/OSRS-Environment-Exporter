package controllers.worldRenderer

import cache.LocationType
import cache.utils.ColorPalette
import com.jogamp.newt.NewtFactory
import com.jogamp.newt.awt.NewtCanvasAWT
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2ES2
import com.jogamp.opengl.GL2ES3
import com.jogamp.opengl.GL4
import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLCapabilities
import com.jogamp.opengl.GLEventListener
import com.jogamp.opengl.GLProfile
import com.jogamp.opengl.math.Matrix4
import com.jogamp.opengl.util.Animator
import com.jogamp.opengl.util.GLBuffers
import controllers.worldRenderer.helpers.AntiAliasingMode
import controllers.worldRenderer.helpers.GLUtil
import controllers.worldRenderer.helpers.GLUtil.glDeleteFrameBuffer
import controllers.worldRenderer.helpers.GLUtil.glDeleteRenderbuffers
import controllers.worldRenderer.helpers.GLUtil.glGenBuffers
import controllers.worldRenderer.helpers.GLUtil.glGetInteger
import controllers.worldRenderer.helpers.GpuIntBuffer
import controllers.worldRenderer.shaders.Shader
import controllers.worldRenderer.shaders.ShaderException
import models.DebugModel
import models.DebugOptionsModel
import models.scene.REGION_HEIGHT
import models.scene.REGION_SIZE
import models.scene.Scene
import models.scene.SceneTile
import org.slf4j.LoggerFactory
import java.awt.Dimension
import java.awt.event.ActionListener
import java.nio.IntBuffer
import kotlin.math.min

class Renderer constructor(
    private val camera: Camera,
    private val scene: Scene,
    private val sceneUploader: SceneUploader,
    private val inputHandler: InputHandler,
    private val textureManager: TextureManager,
    private val debugModel: DebugModel,
    private val debugOptionsModel: DebugOptionsModel,
) : GLEventListener {
    private val logger = LoggerFactory.getLogger(Renderer::class.java)

    private lateinit var gl: GL4
    private var glProgram = 0

    private var fboSceneHandle = 0
    private var rboSceneHandle = 0
    private var rboSceneDepthBuffer = 0

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

    // FIXME: setting these here locks this in as the minimum
    // figure out how to make these small and resize programmatically after load
    var canvasWidth = 100
    var canvasHeight = (canvasWidth / 1.3).toInt()
    private var lastStretchedCanvasWidth = 0
    private var lastStretchedCanvasHeight = 0
    private var lastAntiAliasingMode: AntiAliasingMode? = null
    private var lastFrameTime: Long = 0
    private var deltaTimeTarget = 0

    // -Dnewt.verbose=true
    // -Dnewt.debug=true
    lateinit var window: GLWindow
    lateinit var animator: Animator
    lateinit var glCanvas: NewtCanvasAWT
    fun initCanvas(): NewtCanvasAWT {
        // center camera in viewport
        camera.centerX = canvasWidth / 2
        camera.centerY = canvasHeight / 2

        fboSceneHandle = -1
        rboSceneHandle = -1
        rboSceneDepthBuffer = -1

        val jfxNewtDisplay = NewtFactory.createDisplay(null, false)
        val screen = NewtFactory.createScreen(jfxNewtDisplay, 0)
        val glProfile = GLProfile.getMaxProgrammableCore(true)
        val glCaps = GLCapabilities(glProfile)
        glCaps.alphaBits = 8

        window = GLWindow.create(screen, glCaps)
        window.addGLEventListener(this)
        window.addKeyListener(inputHandler)
        window.addMouseListener(inputHandler)
        inputHandler.renderer = this

        glCanvas = NewtCanvasAWT(window)
        glCanvas.size = Dimension(canvasWidth, canvasHeight)

        animator = Animator(window)
        animator.setUpdateFPSFrames(3, null)
        animator.start()

        lastStretchedCanvasHeight = -1
        lastStretchedCanvasWidth = -1
        lastAntiAliasingMode = null
        textureArrayId = -1

        scene.sceneChangeListeners.add(
            ActionListener {
                isSceneUploadRequired = true

                if (debugOptionsModel.resetCameraOnSceneChange.get()) {
                    camera.cameraX = Constants.LOCAL_HALF_TILE_SIZE * scene.cols * REGION_SIZE
                    camera.cameraY = Constants.LOCAL_HALF_TILE_SIZE * scene.rows * REGION_SIZE
                    camera.cameraZ = -2500
                }
            }
        )

        return glCanvas
    }

    fun stop() {
        animator.stop()
    }

    override fun init(drawable: GLAutoDrawable) {
        gl = drawable.gl.gL4
        gl.glEnable(GL.GL_DEPTH_TEST)
        gl.glDepthFunc(GL.GL_LEQUAL)
        gl.glDepthRangef(0f, 1f)

        priorityRenderer = try {
            GLSLPriorityRenderer(gl)
        } catch (e: ShaderException) {
            logger.warn("Tried to spawn a GLSLPriorityRenderer but got exception", e)
            CPUNonPriorityRenderer(gl)
        }

        try {
            initProgram()
        } catch (e: ShaderException) {
            e.printStackTrace()
        }
        initUniformBuffer()

        // disable vsync
//            gl.swapInterval = 0
    }

    override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {
        gl = drawable.gl.gL4
        canvasWidth = width
        canvasHeight = height
        camera.centerX = canvasWidth / 2
        camera.centerY = canvasHeight / 2
        gl.glViewport(x, y, width, height)
    }

    var isSceneUploadRequired = true
    private val clientStart = System.currentTimeMillis()
    private var lastUpdate = System.nanoTime()

    override fun display(drawable: GLAutoDrawable?) {
        debugModel.fps.set("%.0f".format(animator.lastFPS))

        if (isSceneUploadRequired) {
            uploadScene()
        }

        val thisUpdate = System.nanoTime()
        val deltaTime = (thisUpdate - lastUpdate).toDouble() / 1_000_000
        lastUpdate = thisUpdate

        inputHandler.tick(deltaTime)

        // Setup anti-aliasing
        val antiAliasingMode: AntiAliasingMode = AntiAliasingMode.MSAA_16
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

        val clientCycle = ((System.currentTimeMillis() - clientStart) / 20).toInt() // 50 fps

        priorityRenderer.produceVertices(camera, clientCycle)

        if (textureArrayId == -1) {
            // lazy init textures as they may not be loaded at plugin start.
            // this will return -1 and retry if not all textures are loaded yet, too.
            textureArrayId = textureManager.initTextureArray(gl)
        }
//        val textures: Array<TextureDefinition> = textureProvider.getTextureDefinitions()
        gl.glUseProgram(glProgram)
        // Brightness happens to also be stored in the texture provider, so we use that
        gl.glUniform1f(uniBrightness, ColorPalette.BRIGHTNESS_HIGH.toFloat()) // (float) textureProvider.getBrightness());
        gl.glUniform1i(uniDrawDistance, Constants.MAX_DISTANCE * Constants.LOCAL_TILE_SIZE)
        gl.glUniform1f(uniSmoothBanding, 1f)
        gl.glUniformMatrix4fv(uniViewProjectionMatrix, 1, false, calculateViewProjectionMatrix().matrix, 0)

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
        gl.glUniform2i(uniMouseCoordsId, inputHandler.mouseX, canvasHeight - inputHandler.mouseY)

        // UBO
        gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, uniformBufferId)
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
        gl.glBufferSubData(
            GL2ES3.GL_UNIFORM_BUFFER,
            0,
            uniformBuffer.limit() * Integer.BYTES.toLong(),
            uniformBuffer
        )
        gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, 0)

        // Bind uniforms
        gl.glBindBufferBase(GL2ES3.GL_UNIFORM_BUFFER, 0, uniformBufferId)
        gl.glUniformBlockBinding(glProgram, uniBlockMain, 0)
        gl.glUniform1i(uniTextures, 1) // texture sampler array is bound to texture1
        gl.glUniform2fv(uniTextureOffsets, textureOffsets.size, textureOffsets, 0)

        priorityRenderer.draw()
        gl.glUseProgram(0)

        if (aaEnabled) {
            gl.glBindFramebuffer(GL.GL_READ_FRAMEBUFFER, fboSceneHandle)
            gl.glBindFramebuffer(GL.GL_DRAW_FRAMEBUFFER, 0)
            gl.glBlitFramebuffer(
                0, 0, lastStretchedCanvasWidth, lastStretchedCanvasHeight,
                0, 0, lastStretchedCanvasWidth, lastStretchedCanvasHeight,
                GL.GL_COLOR_BUFFER_BIT, GL.GL_NEAREST
            )
        }

        gl.glBindFramebuffer(GL.GL_READ_FRAMEBUFFER, 0)
        gl.glBindFramebuffer(GL.GL_DRAW_FRAMEBUFFER, 0)
        gl.glBlitFramebuffer(
            0, 0, canvasWidth, canvasHeight,
            0, 0, canvasWidth, canvasHeight,
            GL.GL_COLOR_BUFFER_BIT, GL.GL_NEAREST
        )
        gl.glBindFramebuffer(GL.GL_READ_FRAMEBUFFER, 0)

        if (deltaTimeTarget != 0) {
            val endFrameTime = System.nanoTime()
            val sleepTime = deltaTimeTarget + (lastFrameTime - endFrameTime)
            lastFrameTime = if (sleepTime in 0..SECOND_IN_NANOS) {
                Thread.sleep(
                    sleepTime / MILLISECOND_IN_NANOS,
                    (sleepTime % MILLISECOND_IN_NANOS).toInt()
                )
                endFrameTime + sleepTime
            } else {
                endFrameTime
            }
        }
    }

    /** Sets the FPS target for this renderer.
     *  It may vary above and below the actual value.
     *  @param target The FPS target, or 0 for unlimited.
     */
    fun setFpsTarget(target: Int) {
        deltaTimeTarget =
            if (target > 0) SECOND_IN_NANOS / target
            else 0
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
        SceneExporter(textureManager).exportSceneToFile(scene, this)
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

    override fun dispose(drawable: GLAutoDrawable?) {
        shutdownProgram()
        shutdownAAFbo()
        priorityRenderer.destroy()
    }

    @Throws(ShaderException::class)
    private fun initProgram() {
        val template = Shader.createTemplate(-1, -1)
        template.addInclude(Shader::class.java)

        try {
            glProgram = Shader.PROGRAM.value.compile(gl, template)
        } catch (e: ShaderException) {
            // This will likely destroy the renderer, but the rest of the program should
            // still be usable.
            e.printStackTrace()
        }

        initUniforms()
    }

    private fun initUniforms() {
        uniViewProjectionMatrix = gl.glGetUniformLocation(glProgram, "viewProjectionMatrix")
        uniBrightness = gl.glGetUniformLocation(glProgram, "brightness")
        uniSmoothBanding = gl.glGetUniformLocation(glProgram, "smoothBanding")
        uniDrawDistance = gl.glGetUniformLocation(glProgram, "drawDistance")
        uniTextures = gl.glGetUniformLocation(glProgram, "textures")
        uniTextureOffsets = gl.glGetUniformLocation(glProgram, "textureOffsets")
        uniBlockMain = gl.glGetUniformBlockIndex(glProgram, "uniforms")
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

    private fun initAAFbo(width: Int, height: Int, aaSamples: Int) {
        // Create and bind the FBO
        fboSceneHandle = GLUtil.glGenFrameBuffer(gl)
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fboSceneHandle)

        // Create color render buffer
        rboSceneHandle = GLUtil.glGenRenderbuffer(gl)
        gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, rboSceneHandle)
        gl.glRenderbufferStorageMultisample(GL.GL_RENDERBUFFER, aaSamples, GL.GL_RGBA, width, height)
        gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, GL.GL_RENDERBUFFER, rboSceneHandle)

        // Create depth buffer
        rboSceneDepthBuffer = GLUtil.glGenRenderbuffer(gl)
        gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, rboSceneDepthBuffer)
        gl.glRenderbufferStorageMultisample(GL.GL_RENDERBUFFER, aaSamples, GL.GL_DEPTH_COMPONENT16, width, height)
        gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_RENDERBUFFER, rboSceneDepthBuffer)

        // Reset
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0)
        gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, 0)
    }

    private fun makeProjectionMatrix(width: Float, height: Float, near: Float): FloatArray =
        floatArrayOf(
            2 / width, 0f, 0f, 0f,
            0f, 2 / height, 0f, 0f,
            0f, 0f, -1f, -1f,
            0f, 0f, -2 * near, 0f,
        )

    private fun calculateViewProjectionMatrix(): Matrix4 {
        val viewProjectionMatrix = Matrix4()
        viewProjectionMatrix.scale(camera.scale.toFloat(), camera.scale.toFloat(), 1.0f)
        viewProjectionMatrix.multMatrix(makeProjectionMatrix(canvasWidth.toFloat(), canvasHeight.toFloat(), 50.0f))
        viewProjectionMatrix.rotate((Math.PI - camera.pitch * Constants.UNIT).toFloat(), -1.0f, 0.0f, 0.0f)
        viewProjectionMatrix.rotate((camera.yaw * Constants.UNIT).toFloat(), 0.0f, 1.0f, 0.0f)
        viewProjectionMatrix.translate(-camera.cameraX.toFloat(), -camera.cameraZ.toFloat(), -camera.cameraY.toFloat())
        return viewProjectionMatrix
    }

    private fun shutdownProgram() {
        gl.glDeleteProgram(glProgram)
        glProgram = -1
    }

    private fun shutdownAAFbo() {
        if (fboSceneHandle != -1) {
            glDeleteFrameBuffer(gl, fboSceneHandle)
            fboSceneHandle = -1
        }
        if (rboSceneHandle != -1) {
            glDeleteRenderbuffers(gl, rboSceneHandle)
            rboSceneHandle = -1
        }
        if (rboSceneDepthBuffer != -1) {
            glDeleteRenderbuffers(gl, rboSceneDepthBuffer)
            rboSceneDepthBuffer = -1
        }
    }

    companion object {
        const val SECOND_IN_NANOS = 1_000_000_000
        const val MILLISECOND_IN_NANOS = 1_000_000
    }
}
