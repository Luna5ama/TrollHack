package cum.xiaro.trollhack.manager.managers

import cum.xiaro.trollhack.util.extension.synchronized
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import cum.xiaro.trollhack.TrollHackMod
import cum.xiaro.trollhack.manager.Manager
import cum.xiaro.trollhack.util.ConfigUtils
import cum.xiaro.trollhack.util.PlayerProfile
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object FriendManager : Manager() {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val file = File("${TrollHackMod.DIRECTORY}/friends.json")

    private var friendFile = FriendFile()
    val friends = HashMap<String, PlayerProfile>().synchronized()

    val empty get() = friends.isEmpty()
    var enabled = friendFile.enabled
        set(value) {
            field = value
            friendFile.enabled = value
        }

    fun isFriend(name: String) = friendFile.enabled && friends.contains(name.lowercase())

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
            friendFile = gson.fromJson(FileReader(file), object : TypeToken<FriendFile>() {}.type)
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
            if (friends != other.friends) return false

            return true
        }

        override fun hashCode(): Int {
            var result = enabled.hashCode()
            result = 31 * result + friends.hashCode()
            return result
        }
    }
}