package cache.utils

import kotlin.math.abs
import kotlin.math.pow

class ColorPalette(brightness: Double) {
    val colorPalette = IntArray(65536) { i ->
        HSLtoRGB(i.toShort(), brightness)
    }

    companion object {
        const val BRIGHTNESS_MAX = .6
        const val BRIGHTNESS_HIGH = .7
        const val BRIGHTNESS_LOW = .8
        const val BRIGHTNESS_MIN = .9

        private const val HUE_OFFSET = .5 / 64.0
        private const val SATURATION_OFFSET = .5 / 8.0

        fun adjustForBrightness(rgb: Int, brightness: Double): Int {
            var r = (rgb shr 16).toDouble() / 256.0
            var g = (rgb shr 8 and 255).toDouble() / 256.0
            var b = (rgb and 255).toDouble() / 256.0
            r = r.pow(brightness)
            g = g.pow(brightness)
            b = b.pow(brightness)
            return (
                (r * 256.0).toInt() shl 16
                    or ((g * 256.0).toInt() shl 8)
                    or (b * 256.0).toInt()
                )
        }

        fun HSLtoRGB(hsl: Short, brightness: Double): Int {
            val hue: Double = unpackHue(hsl).toDouble() / 64.0 + HUE_OFFSET
            val saturation: Double =
                unpackSaturation(hsl).toDouble() / 8.0 + SATURATION_OFFSET
            val luminance = unpackLuminance(hsl).toDouble() / 128.0

            // This is just a standard hsl to rgb transform
            // the only difference is the offsets above and the brightness transform below
            val chroma = (1.0 - abs(2.0 * luminance - 1.0)) * saturation
            val x = chroma * (1 - abs(hue * 6.0 % 2.0 - 1.0))
            val lightness = luminance - chroma / 2
            var r = lightness
            var g = lightness
            var b = lightness
            when ((hue * 6.0).toInt()) {
                0 -> {
                    r += chroma
                    g += x
                }
                1 -> {
                    g += chroma
                    r += x
                }
                2 -> {
                    g += chroma
                    b += x
                }
                3 -> {
                    b += chroma
                    g += x
                }
                4 -> {
                    b += chroma
                    r += x
                }
                else -> {
                    r += chroma
                    b += x
                }
            }

            var rgb = (
                (r * 256.0).toInt() shl 16
                    or ((g * 256.0).toInt() shl 8)
                    or (b * 256.0).toInt()
                )

            rgb = adjustForBrightness(rgb, brightness)

            if (rgb == 0) {
                rgb = 1
            }
            return rgb
        }

        fun unpackHue(hsl: Short): Int = hsl.toInt() shr 10 and 63
        fun unpackSaturation(hsl: Short): Int = hsl.toInt() shr 7 and 7
        fun unpackLuminance(hsl: Short): Int = hsl.toInt() and 127
    }
}
