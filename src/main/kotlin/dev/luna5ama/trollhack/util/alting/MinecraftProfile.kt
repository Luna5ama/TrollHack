package dev.luna5ama.trollhack.util.alting

import java.util.*

/**
 * A simple Minecraft profile. Contains all the necessities to login.
 */
class MinecraftProfile(
    /**
     * @return The username of this profile. E.g. PlanetTeamSpeak
     */
    val name: String,
    /**
     * @return The UUID of this profile. E.g. 1aa35f31-0881-4959-bd14-21e8a72ba0c1
     */
    val uuid: UUID,
    /**
     * @return The authentication token to login. E.g. eyJhbGciOiJIUzI1NiJ9.eyJ4dWlkIjoiMjUzNTQyNjUzMTQ4O...
     */
    val token: String
)