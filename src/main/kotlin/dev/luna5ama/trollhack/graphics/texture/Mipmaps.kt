package dev.luna5ama.trollhack.graphics.texture

import dev.fastmc.common.ParallelUtils
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

object Mipmaps {
    private suspend fun half(input: RawImage): RawImage {
        val inputChannels = input.channels
        val rowStride = input.width * inputChannels
        val w = input.width shr 1
        val h = input.height shr 1
        val inputData = input.data
        val pixels = w * h
        val outputData = ByteArray(pixels * inputChannels)

        if (inputChannels == 1) {
            coroutineScope {
                ParallelUtils.splitListIndex(outputData.size) { start, end ->
                    launch {
                        for (i in start until end) {
                            val x = (i % w) * 2
                            val y = (i / w) * 2

                            val y1 = y * rowStride
                            val y2 = y * rowStride + rowStride

                            val sum = (inputData[y1 + x].toInt() and 0xFF) +
                                (inputData[y1 + x + 1].toInt() and 0xFF) +
                                (inputData[y2 + x].toInt() and 0xFF) +
                                (inputData[y2 + x + 1].toInt() and 0xFF)

                            outputData[i] = (sum shr 2).toByte()
                        }
                    }
                }
            }
        } else {
            coroutineScope {
                ParallelUtils.splitListIndex(outputData.size) { start, end ->
                    launch {
                        for (i in start until end) {
                            val x = (i % w) * 2
                            val y = (i / w) * 2

                            val y1 = y * rowStride
                            val y2 = y * rowStride + rowStride

                            repeat(inputChannels) {
                                val sum = (inputData[y1 + x + it].toInt() and 0xFF) +
                                    (inputData[y1 + x + 1 + it].toInt() and 0xFF) +
                                    (inputData[y2 + x + it].toInt() and 0xFF) +
                                    (inputData[y2 + x + 1 + it].toInt() and 0xFF)

                                outputData[i + it] = (sum shr 2).toByte()
                            }
                        }
                    }
                }
            }
        }

        return RawImage(outputData, w, h, inputChannels)
    }

    fun getTotalSize(size: Int, levels: Int): Int {
        var total = size
        for (i in 1..levels) {
            total += size shr (i * 2)
        }
        return total
    }

    fun generate(image: RawImage, levels: Int): Flow<RawImage> {
        return flow {
            var prev = image
            emit(prev)
            repeat(levels) {
                prev = half(prev)
                emit(prev)
            }
        }
    }
}