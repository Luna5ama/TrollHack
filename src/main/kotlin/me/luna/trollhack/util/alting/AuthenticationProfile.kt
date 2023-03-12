package me.luna.trollhack.util.alting

import java.util.*

class AuthenticationProfile(
    val username: String,
    val id: UUID,
    val accessToken: String?,
    val type: Type?,
    val properties: String?
) {
    enum class Type {
        LEGACY, MOJANG, MSA
    }
}