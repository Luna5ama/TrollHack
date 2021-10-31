package cum.xiaro.trollhack.manager.managers

import cum.xiaro.trollhack.util.extension.synchronized
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import cum.xiaro.trollhack.TrollHackMod
import cum.xiaro.trollhack.manager.Manager
import cum.xiaro.trollhack.util.ConfigUtils
import cum.xiaro.trollhack.util.ConnectionUtils
import cum.xiaro.trollhack.util.PlayerProfile
import cum.xiaro.trollhack.util.Wrapper
import java.io.File
import java.util.*

object UUIDManager : Manager() {

    private val file = File("${TrollHackMod.DIRECTORY}/uuid_cache.txt")

    private val parser = JsonParser()
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val type = object : TypeToken<Map<String, UUID>>() {}.type
    private val nameProfileMap = LinkedHashMap<String, PlayerProfile>().synchronized()
    private val uuidNameMap = LinkedHashMap<UUID, PlayerProfile>().synchronized()

    fun getOrRequest(nameOrUUID: String): PlayerProfile? {
        return Wrapper.minecraft.connection?.playerInfoMap?.let { playerInfoMap ->
            val infoMap = ArrayList(playerInfoMap)
            val isUUID = isUUID(nameOrUUID)
            val withOutDashes = removeDashes(nameOrUUID)

            infoMap.find {
                isUUID && removeDashes(it.gameProfile.id.toString()).equals(withOutDashes, ignoreCase = true)
                    || !isUUID && it.gameProfile.name.equals(nameOrUUID, ignoreCase = true)
            }?.gameProfile?.let {
                PlayerProfile(it.id, it.name)
            }
        } ?: requestProfile(nameOrUUID)
    }

    fun getByString(stringIn: String?) = stringIn?.let { string ->
        fixUUID(string)?.let { getByUUID(it) } ?: getByName(string)
    }

    fun getByUUID(uuid: UUID?) = uuid?.let {
        uuidNameMap.getOrPut(uuid) {
            getOrRequest(uuid.toString())?.also { profile ->
                // If UUID already present in nameUuidMap but not in uuidNameMap (user changed name)
                nameProfileMap[profile.name]?.let { uuidNameMap.remove(it.uuid) }
                nameProfileMap[profile.name] = profile
            } ?: return null
        }.also {
            trimMaps()
        }
    }

    fun getByName(name: String?) = name?.let {
        nameProfileMap.getOrPut(name.lowercase()) {
            getOrRequest(name)?.also { profile ->
                // If UUID already present in uuidNameMap but not in nameUuidMap (user changed name)
                uuidNameMap[profile.uuid]?.let { nameProfileMap.remove(it.name) }
                uuidNameMap[profile.uuid] = profile
            } ?: return null
        }.also {
            trimMaps()
        }
    }

    private fun trimMaps() {
        while (nameProfileMap.size > 1000) {
            nameProfileMap.remove(nameProfileMap.keys.first())?.also {
                uuidNameMap.remove(it.uuid)
            }
        }
    }

    private fun requestProfile(nameOrUUID: String): PlayerProfile? {
        val isUUID = isUUID(nameOrUUID)
        val response = if (isUUID) requestProfileFromUUID(nameOrUUID) else requestProfileFromName(nameOrUUID)

        return if (response.isNullOrBlank()) {
            TrollHackMod.logger.error("Response is null or blank, internet might be down")
            null
        } else {
            try {
                @Suppress("DEPRECATION") val jsonElement = parser.parse(response)
                if (isUUID) {
                    val name = jsonElement.asJsonArray.last().asJsonObject["name"].asString
                    PlayerProfile(UUID.fromString(nameOrUUID), name)
                } else {
                    val id = jsonElement.asJsonObject["id"].asString
                    val name = jsonElement.asJsonObject["name"].asString
                    PlayerProfile(fixUUID(id)!!, name) // let it throw a NPE if failed to parse the string to UUID
                }
            } catch (e: Exception) {
                TrollHackMod.logger.error("Failed parsing profile", e)
                null
            }
        }
    }

    private fun requestProfileFromUUID(uuid: String): String? {
        return request("https://api.mojang.com/user/profiles/${removeDashes(uuid)}/names")
    }

    private fun requestProfileFromName(name: String): String? {
        return request("https://api.mojang.com/users/profiles/minecraft/$name")
    }

    private fun request(url: String): String? {
        return ConnectionUtils.requestRawJsonFrom(url) {
            TrollHackMod.logger.error("Failed requesting from Mojang API", it)
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
                uuidNameMap.values.forEach {
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
