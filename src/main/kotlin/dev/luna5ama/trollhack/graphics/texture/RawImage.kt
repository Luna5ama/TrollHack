package dev.luna5ama.trollhack.graphics.texture

data class RawImage(val data: ByteArray, val width: Int, val height: Int, val channels: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RawImage) return false

        if (!data.contentEquals(other.data)) return false
        if (width != other.width) return false
        if (height != other.height) return false
        return channels == other.channels
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + channels
        return result
    }
}