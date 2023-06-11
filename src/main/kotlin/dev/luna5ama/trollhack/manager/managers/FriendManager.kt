package dev.luna5ama.trollhack.manager.managers

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.manager.Manager
import dev.luna5ama.trollhack.util.ConfigUtils
import dev.luna5ama.trollhack.util.PlayerProfile
import dev.luna5ama.trollhack.util.extension.synchronized
import java.io.File
import java.io.FileWriter

object FriendManager : Manager() {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val file = File("${TrollHackMod.DIRECTORY}/friends.json")

    private var friendFile = FriendFile()
    val friends = HashMap<String, PlayerProfile>().synchronized()

    val empty get() = friends.isEmpty()
    var enabled
        get() = friendFile.enabled
        set(value) {
            friendFile.enabled = value
        }

    fun isFriend(name: String) = enabled && friends.contains(name.lowercase())

    fun addFriend(name: String) = UUIDManager.getByName(name)?.let {
        friendFile.friends.add(it)
        friends[it.name.lowercase()] = it
        true
    } ?: false

    fun removeFriend(name: String) = friendFile.friends.remove(friends.remove(name.lowercase()))

    fun clearFriend() {
        friends.clear()
        friendFile.friends.clear()
    }

    fun loadFriends(): Boolean {
        ConfigUtils.fixEmptyJson(file)

        return try {
            friendFile = gson.fromJson(file.readText(), object : TypeToken<FriendFile>() {}.type)
            friends.clear()
            friends.putAll(friendFile.friends.associateBy { it.name.lowercase() })
            TrollHackMod.logger.info("Friend loaded")
            true
        } catch (e: Exception) {
            TrollHackMod.logger.warn("Failed loading friends", e)
            false
        }
    }

    fun saveFriends(): Boolean {
        return try {
            FileWriter(file, false).buffered().use {
                gson.toJson(friendFile, it)
            }
            TrollHackMod.logger.info("Friends saved")
            true
        } catch (e: Exception) {
            TrollHackMod.logger.warn("Failed saving friends", e)
            false
        }
    }

    data class FriendFile(
        var enabled: Boolean = true,
        val friends: MutableSet<PlayerProfile> = LinkedHashSet<PlayerProfile>().synchronized()
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is FriendFile) return false

            if (enabled != other.enabled) return false
            return friends == other.friends
        }

        override fun hashCode(): Int {
            var result = enabled.hashCode()
            result = 31 * result + friends.hashCode()
            return result
        }
    }
}