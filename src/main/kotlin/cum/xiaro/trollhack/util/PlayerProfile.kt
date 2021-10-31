package cum.xiaro.trollhack.util

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
}

