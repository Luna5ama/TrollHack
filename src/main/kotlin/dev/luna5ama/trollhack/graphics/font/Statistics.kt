package dev.luna5ama.trollhack.graphics.font

class Statistics(val chunkSize: Int, val atlasSize: Int) {
    var stringsCached: Int = 0; private set
    val chunksLoaded get() = loadedChunks0.size
    val badChunks get() = corruptedChunks0.size

    private val loadedChunks0 = mutableListOf<Int>()
    private val corruptedChunks0 = mutableListOf<Int>()

    fun chunkInitialized(chunk: Int) {
        loadedChunks0.add(chunk)
        loadedChunks0.sort()
    }

    fun chunkCorrupted(chunk: Int) {
        corruptedChunks0.add(chunk)
        corruptedChunks0.sort()
    }

    fun stringCached() {
        stringsCached++
    }
}