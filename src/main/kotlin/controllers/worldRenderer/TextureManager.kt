package controllers.worldRenderer
/*
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
import AppConstants
import cache.definitions.TextureDefinition
import cache.loaders.SpriteLoader
import cache.loaders.TextureLoader
import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2ES3
import com.jogamp.opengl.GL4
import controllers.worldRenderer.helpers.GLUtil
import java.awt.image.BufferedImage
import java.awt.image.IndexColorModel
import java.io.File
import java.nio.ByteBuffer
import javax.imageio.ImageIO

class TextureManager constructor(
    private val spriteLoader: SpriteLoader,
    private val textureLoader: TextureLoader
) {
    fun initTextureArray(gl: GL4): Int {
        if (!allTexturesLoaded()) {
            return -1
        }
        val textures: Array<TextureDefinition?> = textureLoader.getAll()
        val textureArrayId: Int = GLUtil.glGenTexture(gl)
        gl.glBindTexture(GL2ES3.GL_TEXTURE_2D_ARRAY, textureArrayId)
        gl.glTexStorage3D(
            GL2ES3.GL_TEXTURE_2D_ARRAY,
            1,
            GL.GL_RGBA8,
            TEXTURE_SIZE,
            TEXTURE_SIZE,
            textures.size
        )
        gl.glTexParameteri(GL2ES3.GL_TEXTURE_2D_ARRAY, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST)
        gl.glTexParameteri(GL2ES3.GL_TEXTURE_2D_ARRAY, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST)
        gl.glTexParameteri(GL2ES3.GL_TEXTURE_2D_ARRAY, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE)

        // Set brightness to 1.0d to upload unmodified textures to GPU
// 		double save = textureProvider.getBrightness();
// 		textureProvider.setBrightness(1.0d);
        updateTextures(gl, textureArrayId)

// 		textureProvider.setBrightness(save);
        gl.glActiveTexture(GL.GL_TEXTURE1)
        gl.glBindTexture(GL2ES3.GL_TEXTURE_2D_ARRAY, textureArrayId)
        gl.glActiveTexture(GL.GL_TEXTURE0)
        return textureArrayId
    }

    fun freeTextureArray(gl: GL4, textureArrayId: Int) {
        GLUtil.glDeleteTexture(gl, textureArrayId)
    }

    /**
     * Check if all textures have been loaded and cached yet.
     *
     * @param textureProvider
     * @return
     */
    private fun allTexturesLoaded(): Boolean {
        val textures: Array<TextureDefinition?> = textureLoader.getAll()
        if (textures.isEmpty()) {
            return false
        }
        for (textureId in textures.indices) {
            val texture: TextureDefinition = textures[textureId] ?: continue
            val loaded = texture.loadPixels(0.8, 128, spriteLoader)
            if (!loaded) {
                return false
            }
        }
        return true
    }

    private fun updateTextures(gl: GL4, textureArrayId: Int) {
        val textures: Array<TextureDefinition?> = textureLoader.getAll()
        gl.glBindTexture(GL2ES3.GL_TEXTURE_2D_ARRAY, textureArrayId)
        var cnt = 0
        for (textureId in textures.indices) {
            val texture: TextureDefinition = textures[textureId] ?: continue
            val srcPixels: IntArray = textureLoader.get(textureId)?.pixels ?: continue // this can't happen
            ++cnt
            if (srcPixels.size != TEXTURE_SIZE * TEXTURE_SIZE) {
                // The texture storage is 128x128 bytes, and will only work correctly with the
                // 128x128 textures from high detail mode
                continue
            }
            val pixels = convertPixels(
                srcPixels,
                TEXTURE_SIZE,
                TEXTURE_SIZE,
                TEXTURE_SIZE,
                TEXTURE_SIZE
            )
            val pixelBuffer = ByteBuffer.wrap(pixels)
            gl.glTexSubImage3D(
                GL2ES3.GL_TEXTURE_2D_ARRAY,
                0,
                0,
                0,
                textureId,
                TEXTURE_SIZE,
                TEXTURE_SIZE,
                1,
                GL.GL_RGBA,
                GL.GL_UNSIGNED_BYTE,
                pixelBuffer
            )

            File(AppConstants.TEXTURES_DIRECTORY).mkdirs()

            val image = BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, IndexColorModel.BITMASK)
            for (y in 0 until 128) {
                for (x in 0 until 128) {
                    var p = srcPixels[x + y * 128]
                    val r = (p and 0xff000000L.toInt()).ushr(24)
                    val g = (p and 0xff0000).ushr(16)
                    val b = (p and 0xff00).ushr(8)
                    val a = p and 0xff

                    var alpha = 0
                    if (g + b + a > 0) {
                        alpha = 0xff
                    }

                    p = (alpha shl 24) or (g shl 16) or (b shl 8) or a
                    image.setRGB(x, y, p)
                }
            }

            ImageIO.write(
                image,
                "png",
                File("${AppConstants.TEXTURES_DIRECTORY}/$textureId.png")
            )
        }
    }

    /**
     * Animate the given texture
     *
     * @param texture
     * @param diff    Number of elapsed client ticks since last animation
     */
//    fun animate(texture: TextureDefinition, diff: Int) {
//        val pixels: IntArray = texture.pixels ?: return
//        val animationSpeed: Int = texture.getAnimationSpeed()
//        val uvdiff =
//            if (pixels.size == 4096) PERC_64 else PERC_128
//        var u: Float = texture.getU()
//        var v: Float = texture.getV()
//        val offset = animationSpeed * diff
//        val d = offset.toFloat() * uvdiff
//        when (texture.getAnimationDirection()) {
//            1 -> {
//                v -= d
//                if (v < 0f) {
//                    v += 1f
//                }
//            }
//            3 -> {
//                v += d
//                if (v > 1f) {
//                    v -= 1f
//                }
//            }
//            2 -> {
//                u -= d
//                if (u < 0f) {
//                    u += 1f
//                }
//            }
//            4 -> {
//                u += d
//                if (u > 1f) {
//                    u -= 1f
//                }
//            }
//            else -> return
//        }
//        texture.setU(u)
//        texture.setV(v)
//    }

    companion object {
        private const val PERC_64 = 1f / 64f
        private const val PERC_128 = 1f / 128f
        private const val TEXTURE_SIZE = 128
        private fun convertPixels(
            srcPixels: IntArray,
            width: Int,
            height: Int,
            textureWidth: Int,
            textureHeight: Int
        ): ByteArray {
            val pixels = ByteArray(textureWidth * textureHeight * 4)
            var pixelIdx = 0
            var srcPixelIdx = 0
            val offset = (textureWidth - width) * 4
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val rgb = srcPixels[srcPixelIdx++]
                    if (rgb != 0) {
                        pixels[pixelIdx++] = (rgb shr 16).toByte()
                        pixels[pixelIdx++] = (rgb shr 8).toByte()
                        pixels[pixelIdx++] = rgb.toByte()
                        pixels[pixelIdx++] = (-1).toByte()
                    } else {
                        pixelIdx += 4
                    }
                }
                pixelIdx += offset
            }
            return pixels
        }
    }
}
