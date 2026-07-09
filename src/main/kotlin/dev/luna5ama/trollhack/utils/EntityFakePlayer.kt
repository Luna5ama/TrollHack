@file:OptIn(ExperimentalStdlibApi::class)

package dev.luna5ama.trollhack.utils


import com.mojang.authlib.GameProfile
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.RemotePlayer
import java.util.*

class EntityFakePlayer(world: ClientLevel, name: String, uuid: UUID? = null) : RemotePlayer(world, GameProfile(adaptUUID(uuid), name)) {
    companion object {
        fun adaptUUID(uuid: UUID?): UUID {
            var ret = uuid
            if (ClientSettings.customUUID)
                ret = runCatching { UUID.fromString(ClientSettings.uuid) }
                    .getOrElse {

                        it.printStackTrace()
                        UUID.randomUUID()
                    }
            return ret ?: UUID.randomUUID()
        }
    }
}