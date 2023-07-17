package dev.luna5ama.trollhack.graphics.texture

import dev.fastmc.common.ParallelUtils
import dev.fastmc.common.ceilToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder

object BC4Compression {
    private fun IntArray.encode(output: ByteBuffer) {
        var min = 255
        var max = 0

        for (i in 0 until 16) {
            val value = this[i]
            if (value < min) {
                min = value
            }
            if (value > max) {
                max = value
            }
        }

        if (min == max) {
            output.putLong(((min shl 8) or max).toLong())
            return
        }

        var table = 0L

        val min24 = min * 0x10101
        val max24 = max * 0x10101
        val step = (max24 - min24) / 7
        val halfStep = step shr 1
        val f = -min24 + halfStep

        for (i in 0 until 16) {
            val value = this[i] * 0x10101
            var index = (value + f) / step
            index = (7 - index) or (((index + 1) shr 3) * -1)
            index %= 7
            index += 1
            table = table or (index.toLong() shl (i * 3))
        }

        output.putLong((table shl 16) or (min.toLong() shl 8) or max.toLong())
    }

    private fun RawImage.extractBlock(output: IntArray, x: Int, y: Int) {
        val r0 = y * width
        val r1 = (y + 1) * width
        val r2 = (y + 2) * width
        val r3 = (y + 3) * width

        @Suppress("UnnecessaryVariable")
        val c0 = x
        val c1 = x + 1
        val c2 = x + 2
        val c3 = x + 3

        output[0] = data[r0 + c0].toInt() and 0xFF
        output[1] = data[r0 + c1].toInt() and 0xFF
        output[2] = data[r0 + c2].toInt() and 0xFF
        output[3] = data[r0 + c3].toInt() and 0xFF

        output[4] = data[r1 + c0].toInt() and 0xFF
        output[5] = data[r1 + c1].toInt() and 0xFF
        output[6] = data[r1 + c2].toInt() and 0xFF
        output[7] = data[r1 + c3].toInt() and 0xFF

        output[8] = data[r2 + c0].toInt() and 0xFF
        output[9] = data[r2 + c1].toInt() and 0xFF
        output[10] = data[r2 + c2].toInt() and 0xFF
        output[11] = data[r2 + c3].toInt() and 0xFF

        output[12] = data[r3 + c0].toInt() and 0xFF
        output[13] = data[r3 + c1].toInt() and 0xFF
        output[14] = data[r3 + c2].toInt() and 0xFF
        output[15] = data[r3 + c3].toInt() and 0xFF
    }

    fun getEncodedSize(rawSize: Int): Int {
        return rawSize / 2
    }

    suspend fun encode(input: RawImage, output: ByteBuffer) {
        val xBlocks = (input.width / 4.0).ceilToInt()
        val yBlocks = (input.height / 4.0).ceilToInt()
        val totalBlocks = xBlocks * yBlocks

        coroutineScope {
            var offset = output.position()
            ParallelUtils.splitListIndex(totalBlocks) { start, end ->
                val size = (end - start) * 8
                output.clear().position(offset).limit(offset + size)
                val view = output.slice()
                view.order(ByteOrder.nativeOrder())
                offset += size
                launch {
                    val temp = IntArray(16)
                    for (i in start until end) {
                        val x = i % xBlocks
                        val y = i / xBlocks
                        input.extractBlock(temp, x * 4, y * 4)
                        temp.encode(view)
                    }
                }
            }
        }
    }
}