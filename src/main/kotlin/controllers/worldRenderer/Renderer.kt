package controllers.worldRenderer

import cache.LocationType
import cache.utils.ColorPalette
import com.jogamp.newt.NewtFactory
import com.jogamp.newt.awt.NewtCanvasAWT
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2ES2
import com.jogamp.opengl.GL2ES3
import com.jogamp.opengl.GL2GL3
import com.jogamp.opengl.GL4
import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLCapabilities
import com.jogamp.opengl.GLEventListener
import com.jogamp.opengl.GLProfile
import com.jogamp.opengl.util.Animator
import com.jogamp.opengl.util.GLBuffers
import controllers.worldRenderer.helpers.AntiAliasingMode
import controllers.worldRenderer.helpers.GLUtil
import controllers.worldRenderer.helpers.GLUtil.glDeleteBuffer
import controllers.worldRenderer.helpers.GLUtil.glDeleteBuffers
import controllers.worldRenderer.helpers.GLUtil.glDeleteFrameBuffer
import controllers.worldRenderer.helpers.GLUtil.glDeleteRenderbuffers
import controllers.worldRenderer.helpers.GLUtil.glDeleteTexture
import controllers.worldRenderer.helpers.GLUtil.glDeleteVertexArrays
import controllers.worldRenderer.helpers.GLUtil.glGenBuffers
import controllers.worldRenderer.helpers.GLUtil.glGenVertexArrays
import controllers.worldRenderer.helpers.GLUtil.glGetInteger
import controllers.worldRenderer.helpers.GpuIntBuffer
import controllers.worldRenderer.helpers.ModelBuffers
import controllers.worldRenderer.shaders.Shader
import controllers.worldRenderer.shaders.ShaderException
import models.DebugModel
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
    private val debugModel: DebugModel
) : GLEventListener {
    private val logger = LoggerFactory.getLogger(Renderer::class.java)

    private val MAX_TEMP_VERTICES: Int = 65535

    private lateinit var gl: GL4
    private var glProgram = 0

    private var fboMainRenderer = 0
    private var rboDepthMain = 0
    private var texColorMain = 0
    private var texPickerMain = 0
    private val pboIds = IntArray(3)

    private var vaoHandle = 0

    private var fboSceneHandle = 0
    private var colorTexSceneHandle = 0
    private var rboSceneHandle = 0
    private var depthTexSceneHandle = 0

    private var tmpOutBufferId = 0 // target vertex buffer for compute shaders
    private var tmpOutUvBufferId = 0 // target uv buffer for compute shaders
    private var colorPickerBufferId = 0 // buffer for unique picker id
//    private var animFrameBufferId = 0 // which frame the model should display on

    private var hoverId = 0

    private var textureArrayId = 0
    private var uniformBufferId = 0
    private val uniformBuffer: IntBuffer = GpuIntBuffer.allocateDirect(5 + 3 + 1)
    private val textureOffsets = FloatArray(128)

    private lateinit var modelBuffers: ModelBuffers
    private lateinit var priorityRenderer: PriorityRenderer

    var zLevelsSelected = Array(REGION_HEIGHT) { true }

    // Uniforms
    private var uniDrawDistance = 0
    private var uniProjectionMatrix = 0
    private var uniBrightness = 0
    private var uniTextures = 0
    private var uniTextureOffsets = 0
    private var uniBlockMain = 0
    private var uniSmoothBanding = 0
    private var uniHoverId = 0
    private var uniMouseCoordsId = 0

    // FIXME: setting these here locks this in as the minimum
    // figure out how to make these small and resize programmatically after load
    var canvasWidth = 100
    var canvasHeight = (canvasWidth / 1.3).toInt()
    private var lastViewportWidth = 0
    private var lastViewportHeight = 0
    private var lastCanvasWidth = 0
    private var lastCanvasHeight = 0
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
        tmpOutUvBufferId = -1
        tmpOutBufferId = -1
        colorPickerBufferId = -1

        colorTexSceneHandle = -1
        depthTexSceneHandle = -1
        fboSceneHandle = -1
        rboSceneHandle = -1

        modelBuffers = ModelBuffers()

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

        lastCanvasHeight = -1
        lastCanvasWidth = lastCanvasHeight
        lastViewportHeight = lastCanvasWidth
        lastViewportWidth = lastViewportHeight
        lastStretchedCanvasHeight = -1
        lastStretchedCanvasWidth = lastStretchedCanvasHeight
        lastAntiAliasingMode = null
        textureArrayId = -1

        scene.sceneChangeListeners.add(
            ActionListener {
                isSceneUploadRequired = true
                camera.cameraX = Constants.LOCAL_HALF_TILE_SIZE * scene.cols * REGION_SIZE
                camera.cameraY = Constants.LOCAL_HALF_TILE_SIZE * scene.rows * REGION_SIZE
                camera.cameraZ = -2500
            }
        )

        return glCanvas
    }

    fun stop() {
        animator.stop()
    }

    override fun init(drawable: GLAutoDrawable) {
        try {
            gl = drawable.gl.gL4
            gl.glEnable(GL.GL_DEPTH_TEST)
            gl.glDepthFunc(GL.GL_LEQUAL)
            gl.glDepthRangef(0f, 1f)

            priorityRenderer = GLSLPriorityRenderer(gl)
            initProgram()
            initUniformBuffer()
            initBuffers()
            initPickerBuffer()
            initVao()

            // disable vsync
//            gl.swapInterval = 0
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

    var isSceneUploadRequired = true
    private val clientStart = System.currentTimeMillis()
    private var lastUpdate = System.nanoTime()

    override fun display(drawable: GLAutoDrawable?) {
        debugModel.fps.set("%.0f".format(animator.lastFPS))

        if (isSceneUploadRequired) {
            uploadScene()
        }

        sceneUploader.resetOffsets() // to reuse uploadModel function

        val thisUpdate = System.nanoTime()
        val deltaTime = (thisUpdate - lastUpdate).toDouble() / 1_000_000
        lastUpdate = thisUpdate

        inputHandler.tick(deltaTime)

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

        modelBuffers.flip()
        modelBuffers.flipVertUv()
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

        priorityRenderer.produceVertices(modelBuffers, uniformBufferId, tmpOutBufferId, tmpOutUvBufferId, colorPickerBufferId)

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
        gl.glUniform1i(uniHoverId, hoverId)
        gl.glUniform2i(uniMouseCoordsId, inputHandler.mouseX, canvasHeight - inputHandler.mouseY)

        // Bind uniforms
        gl.glBindBufferBase(GL2ES3.GL_UNIFORM_BUFFER, 0, uniformBufferId)
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
        modelBuffers.clear()
        modelBuffers.clearBufferOffset()

        zLevelsSelected.forEachIndexed { z, visible ->
            if (visible) {
                for (x in 0 until scene.cols * REGION_SIZE) {
                    for (y in 0 until scene.rows * REGION_SIZE) {
                        scene.getTile(z, x, y)?.let { drawTile(it) }
                    }
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
//        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, animFrameBufferId)
//        gl.glBufferData(
//            GL.GL_ARRAY_BUFFER,
//            ((modelBuffers.targetBufferOffset + MAX_TEMP_VERTICES) * GLBuffers.SIZEOF_INT * 4).toLong(),
//            null,
//            GL.GL_DYNAMIC_DRAW
//        )
    }

    private fun drawTile(tile: SceneTile) {
        val x: Int = tile.x
        val y: Int = tile.y
        val tilePaint = tile.tilePaint
        if (tilePaint != null) {
            priorityRenderer.addRenderable(tilePaint, modelBuffers, x, y, 0, LocationType.TILE_PAINT.id)
        }

        val tileModel = tile.tileModel
        if (tileModel != null) {
            priorityRenderer.addRenderable(tileModel, modelBuffers, x, y, 0, LocationType.TILE_MODEL.id)
        }

        val floorDecorationEntity = tile.floorDecoration?.entity
        if (floorDecorationEntity != null) {
            priorityRenderer.addRenderable(floorDecorationEntity.model, modelBuffers, x, y, floorDecorationEntity.height, LocationType.FLOOR_DECORATION.id)
        }

        val wall = tile.wall
        if (wall != null) {
            priorityRenderer.addRenderable(wall.entity.model, modelBuffers, x, y, wall.entity.height, wall.type.id)
            if (wall.entity2 != null) {
                priorityRenderer.addRenderable(wall.entity2.model, modelBuffers, x, y, wall.entity2.height, wall.type.id)
            }
        }

        val wallDecorationEntity = tile.wallDecoration?.entity
        if (wallDecorationEntity != null) {
            priorityRenderer.addRenderable(wallDecorationEntity.model, modelBuffers, x, y, wallDecorationEntity.height, LocationType.INTERACTABLE_WALL_DECORATION.id)
        }

        for (gameObject in tile.gameObjects) {
            priorityRenderer.addRenderable(gameObject.entity.model, modelBuffers, x, y, gameObject.entity.height, LocationType.INTERACTABLE.id)
        }
    }

    fun exportScene() {
        SceneExporter(textureManager).exportSceneToFile(scene, this)
    }

    private fun uploadScene() {
        modelBuffers.clearVertUv()
        try {
            sceneUploader.upload(scene, modelBuffers.vertexBuffer, modelBuffers.uvBuffer)
        } catch (e: Exception) {
            e.printStackTrace()
            logger.warn("out of space vertexBuffer size {}", modelBuffers.vertexBuffer.buffer.limit())
        }
        modelBuffers.flipVertUv()

        logger.debug("vertexBuffer size {}", modelBuffers.vertexBuffer.buffer.limit())
        priorityRenderer.startAdding(modelBuffers)
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
        uniProjectionMatrix = gl.glGetUniformLocation(glProgram, "projectionMatrix")
        uniBrightness = gl.glGetUniformLocation(glProgram, "brightness")
        uniSmoothBanding = gl.glGetUniformLocation(glProgram, "smoothBanding")
        uniDrawDistance = gl.glGetUniformLocation(glProgram, "drawDistance")
        uniTextures = gl.glGetUniformLocation(glProgram, "textures")
        uniTextureOffsets = gl.glGetUniformLocation(glProgram, "textureOffsets")
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
            logger.warn("bad picker fbo")
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
            logger.warn("bad aaPicker fbo")
        }

        // Reset
        gl.glBindTexture(GL2ES2.GL_TEXTURE_2D_MULTISAMPLE, 0)
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0)
        gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, 0)
    }

    private fun initBuffers() {
        tmpOutBufferId = glGenBuffers(gl)
        tmpOutUvBufferId = glGenBuffers(gl)
        colorPickerBufferId = glGenBuffers(gl)
//        animFrameBufferId = glGenBuffers(gl)
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
            2 / (right - left), 0f, 0f, 0f,
            0f, 2 / (top - bottom), 0f, 0f,
            0f, 0f, -2 / (far - near), 0f,
            tx, ty, tz, 1f
        )
        gl.glUniformMatrix4fv(uniProjectionMatrix, 1, false, matrix, 0)
        gl.glUseProgram(0)
    }

    private fun shutdownBuffers() {
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
//        if (animFrameBufferId != -1) {
//            glDeleteBuffer(gl, animFrameBufferId)
//            animFrameBufferId = -1
//        }
    }

    private fun shutdownProgram() {
        gl.glDeleteProgram(glProgram)
        glProgram = -1
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

    companion object {
        const val SECOND_IN_NANOS = 1_000_000_000
        const val MILLISECOND_IN_NANOS = 1_000_000
    }
}
