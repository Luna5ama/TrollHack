package dev.luna5ama.trollhack.manager.managers

import com.google.gson.JsonParser
import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.manager.Manager
import dev.luna5ama.trollhack.util.ConfigUtils
import dev.luna5ama.trollhack.util.PlayerProfile
import dev.luna5ama.trollhack.util.Wrapper
import dev.luna5ama.trollhack.util.extension.synchronized
import java.io.File
import java.io.FileNotFoundException
import java.net.URL
import java.util.*

object UUIDManager : Manager() {
    private val file = File("${TrollHackMod.DIRECTORY}/uuid_cache.txt")

    private val parser = JsonParser()
    private val nameProfileMap = LinkedHashMap<String, PlayerProfile>().synchronized()
    private val uuidNameMap = LinkedHashMap<UUID, PlayerProfile>().synchronized()

    fun getByString(stringIn: String): PlayerProfile? {
        return fixUUID(stringIn)?.let {
            getByUUID(it)
        } ?: getByName(stringIn)
    }

    fun getByUUID(uuid: UUID, forceOnline: Boolean = false): PlayerProfile? {
        val result = uuidNameMap.getOrPut(uuid) {
            requestProfile(uuid, forceOnline)?.also { profile ->
                // If UUID already present in nameUuidMap but not in uuidNameMap (user changed name)
                nameProfileMap[profile.name]?.let { uuidNameMap.remove(it.uuid) }
                nameProfileMap[profile.name] = profile
            } ?: return null
        }

        trimMaps()

        return result.takeUnless { it.isInvalid }
    }

    fun getByName(name: String, forceOnline: Boolean = false): PlayerProfile? {
        val result = nameProfileMap.getOrPut(name.lowercase()) {
            requestProfile(name, forceOnline)?.also { profile ->
                // If UUID already present in uuidNameMap but not in nameUuidMap (user changed name)
                uuidNameMap[profile.uuid]?.let { nameProfileMap.remove(it.name) }
                uuidNameMap[profile.uuid] = profile
            } ?: return null
        }

        trimMaps()

        return result.takeUnless { it.isInvalid }
    }

    private fun trimMaps() {
        while (nameProfileMap.size > 1000) {
            nameProfileMap.remove(nameProfileMap.keys.first())?.also {
                uuidNameMap.remove(it.uuid)
            }
        }
    }

    private fun requestProfile(name: String, forceOnline: Boolean): PlayerProfile? {
        if (!forceOnline) {
            Wrapper.minecraft.connection?.playerInfoMap?.find {
                it.gameProfile.name.equals(name, ignoreCase = true)
            }?.let {
                return PlayerProfile(it.gameProfile.id, it.gameProfile.name)
            }
        }

        val response = try {
            URL("https://api.mojang.com/users/profiles/minecraft/$name").readText()
        } catch (e: FileNotFoundException) {
            return PlayerProfile.INVALID
        } catch (e: Exception) {
            TrollHackMod.logger.error("Failed requesting profile", e)
            return null
        }

        return if (response.isBlank()) {
            TrollHackMod.logger.error("Response is null or blank, internet might be down")
            null
        } else {
            try {
                val jsonElement = parser.parse(response).asJsonObject
                val id = jsonElement["id"].asString
                val realName = jsonElement["name"].asString
                PlayerProfile(fixUUID(id)!!, realName) // let it throw a NPE if failed to parse the string to UUID
            } catch (e: Exception) {
                TrollHackMod.logger.error("Failed parsing profile", e)
                null
            }
        }
    }

    private fun requestProfile(uuid: UUID, forceOnline: Boolean): PlayerProfile? {
        if (forceOnline) {
            Wrapper.minecraft.connection?.getPlayerInfo(uuid)?.let {
                return PlayerProfile(it.gameProfile.id, it.gameProfile.name)
            }
        }

        val response = try {
            URL("https://api.mojang.com/user/profiles/${removeDashes(uuid.toString())}/names").readText()
        } catch (e: FileNotFoundException) {
            return PlayerProfile.INVALID
        } catch (e: Exception) {
            TrollHackMod.logger.error("Failed requesting profile", e)
            return null
        }

        return if (response.isBlank()) {
            TrollHackMod.logger.error("Response is null or blank, internet might be down")
            null
        } else {
            try {
                val jsonElement = parser.parse(response).asJsonArray
                val name = jsonElement.asJsonArray.last().asJsonObject["name"].asString
                PlayerProfile(uuid, name)
            } catch (e: Exception) {
                TrollHackMod.logger.error("Failed parsing profile", e)
                null
            }
        }
    }

    fun load(): Boolean {
        ConfigUtils.fixEmptyJson(file)

        return try {
            val profileList = ArrayList<PlayerProfile>()

            file.forEachLine {
                runCatching {
                    val split = it.split(':')
                    profileList.add(PlayerProfile(UUID.fromString(split[1]), split[0]))
                }
            }

            uuidNameMap.clear()
            nameProfileMap.clear()
            uuidNameMap.putAll(profileList.associateBy { it.uuid })
            nameProfileMap.putAll(profileList.associateBy { it.name })

            TrollHackMod.logger.info("UUID cache loaded")
            true
        } catch (e: Exception) {
            TrollHackMod.logger.warn("Failed loading UUID cache", e)
            false
        }
    }

    fun save(): Boolean {
        return try {
            file.bufferedWriter().use { writer ->
                uuidNameMap.values.asSequence().filter {
                    !it.isInvalid
                }.forEach {
                    writer.append(it.name)
                    writer.append(':')
                    writer.append(it.uuid.toString())
                    writer.newLine()
                }
            }

            TrollHackMod.logger.info("UUID cache saved")
            true
        } catch (e: Exception) {
            TrollHackMod.logger.warn("Failed saving UUID cache", e)
            false
        }
    }

    private val uuidRegex = "[a-z0-9].{7}-[a-z0-9].{3}-[a-z0-9].{3}-[a-z0-9].{3}-[a-z0-9].{11}".toRegex()

    private fun fixUUID(string: String): UUID? {
        if (isUUID(string)) return UUID.fromString(string)
        if (string.length < 32) return null
        val fixed = insertDashes(string)
        return if (isUUID(fixed)) UUID.fromString(fixed)
        else null
    }

    private fun isUUID(string: String) = uuidRegex.matches(string)

    private fun removeDashes(string: String) = string.replace("-", "")

    private fun insertDashes(string: String) = StringBuilder(string)
        .insert(8, '-')
        .insert(13, '-')
        .insert(18, '-')
        .insert(23, '-')
        .toString()
}
