/* Derived from RuneLite source code, which is licensed as follows:
 *
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
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
package controllers.worldRenderer.helpers

import org.lwjgl.BufferUtils
import org.slf4j.LoggerFactory
import java.nio.BufferOverflowException
import java.nio.IntBuffer

class GpuIntBuffer {
    private val logger = LoggerFactory.getLogger(GpuIntBuffer::class.java)

    var buffer = allocateDirect(65536)
        private set

    fun flip() {
        buffer.flip()
    }

    fun clear() {
        buffer.clear()
    }

    fun ensureCapacity(size: Int) {
        var capacity = buffer.capacity()
        val position = buffer.position()
        if (capacity - position < size) {
            do {
                capacity *= 2
            } while (capacity - position < size)
            val newB = allocateDirect(capacity)
            buffer.flip()
            try {
                newB.put(buffer)
            } catch (e: BufferOverflowException) {
                logger.error("Could not append to GPU buffer", e)
            }

            buffer = newB
        }
    }

    companion object {
        fun allocateDirect(size: Int): IntBuffer =
            BufferUtils.createIntBuffer(size)
    }
}
