package dev.luna5ama.trollhack.util

import java.util.*

class PlayerProfile(
    val uuid: UUID,
    val name: String
) {
    override fun equals(other: Any?): Boolean {
        return this === other
            || other is PlayerProfile
            && other.uuid == uuid
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }

    val isInvalid
        get() = this.uuid == INVALID.uuid

    companion object {
        @JvmField
        val INVALID = PlayerProfile(UUID(0L, 0L), "Invalid")
    }
}

