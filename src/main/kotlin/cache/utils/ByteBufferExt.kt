package cache.utils

import java.nio.ByteBuffer

fun ByteBuffer.readUnsignedSmartShortExtended(): Int {
    var acc = 0
    do {
        val curr = readUnsignedShortSmart()
        acc += curr
    } while (curr == 0x7FFF)
    return acc
}

fun ByteBuffer.readUnsignedShortSmart(): Int {
    val peek = peek()
    return if (peek < 128) get().toInt() else readUnsignedShort() - 0x8000
}

fun ByteBuffer.readShortSmart(): Int {
    val peek: Int = peek()
    return if (peek < 128) get().toInt() - 64 else this.readUnsignedShort() - 0xc000
}

fun ByteBuffer.readUnsignedShort(): Int {
    return short.toInt() and 0xFFFF
}

fun ByteBuffer.readUnsignedByte(): Int {
    return get().toInt() and 0xFF
}

fun ByteBuffer.peek(): Int {
    mark()
    val peek: Int = get().toInt() and 0xFF
    reset()
    return peek
}

fun ByteBuffer.read24BitInt(): Int {
    return (this.readUnsignedByte() shl 16) + (this.readUnsignedByte() shl 8) + this.readUnsignedByte()
}

fun ByteBuffer.readString(): String? {
    val sb = StringBuilder()
    while (true) {
        var ch: Int = this.readUnsignedByte()
        if (ch == 0) {
            break
        }
        if (ch in 128..159) {
            var var7: Char = CHARACTERS[ch - 128]
            if (0 == var7.toInt()) {
                var7 = '?'
            }
            ch = var7.toInt()
        }
        sb.append(ch.toChar())
    }
    return sb.toString()
}

fun ByteBuffer.readByteArray(length: Int): ByteArray {
    val array = ByteArray(length)
    get(array)
    return array
}

private val CHARACTERS = charArrayOf(
    '\u20ac', '\u0000', '\u201a', '\u0192', '\u201e', '\u2026',
    '\u2020', '\u2021', '\u02c6', '\u2030', '\u0160', '\u2039',
    '\u0152', '\u0000', '\u017d', '\u0000', '\u0000', '\u2018',
    '\u2019', '\u201c', '\u201d', '\u2022', '\u2013', '\u2014',
    '\u02dc', '\u2122', '\u0161', '\u203a', '\u0153', '\u0000',
    '\u017e', '\u0178'
)
