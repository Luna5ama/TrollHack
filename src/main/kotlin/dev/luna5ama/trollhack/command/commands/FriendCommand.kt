package dev.luna5ama.trollhack.command.commands

import dev.luna5ama.trollhack.command.ClientCommand
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.util.text.NoSpamMessage

object FriendCommand : ClientCommand(
    name = "friend",
    alias = arrayOf("f"),
    description = "Add someone as your friend!"
) {

    private var confirmTime = 0L

    init {
        literal("add", "new", "+") {
            player("player") { playerArg ->
                execute("Add a friend") {
                    val name = playerArg.value.name
                    if (FriendManager.isFriend(name)) {
                        NoSpamMessage.sendMessage("That player is already your friend.")
                    } else {
                        if (FriendManager.addFriend(name)) {
                            NoSpamMessage.sendMessage("§7${name}§r has been friended.")
                        } else {
                            NoSpamMessage.sendMessage("Failed to find UUID of $name")
                        }
                    }
                }
            }
        }

        literal("del", "remove", "-") {
            player("player") { playerArg ->
                execute("Remove a friend") {
                    val name = playerArg.value.name
                    if (FriendManager.removeFriend(name)) NoSpamMessage.sendMessage("§7${name}§r has been unfriended.")
                    else NoSpamMessage.sendMessage("That player isn't your friend.")
                }
            }
        }

        literal("toggle") {
            execute("Disable or enable all friends") {
                FriendManager.enabled = !FriendManager.enabled
                if (FriendManager.enabled) {
                    NoSpamMessage.sendMessage("Friends have been §aenabled")
                } else {
                    NoSpamMessage.sendMessage("Friends have been §cdisabled")
                }
            }
        }

        literal("clear") {
            execute("Clear friends list") {
                if (System.currentTimeMillis() - confirmTime > 15000L) {
                    confirmTime = System.currentTimeMillis()
                    NoSpamMessage.sendMessage("This will delete ALL your friends, run §7${prefix}friend clear§f again to confirm")
                } else {
                    confirmTime = 0L
                    FriendManager.clearFriend()
                    NoSpamMessage.sendMessage("Friends have been §ccleared")
                }
            }
        }

        literal("is") {
            player("player") { playerArg ->
                execute("Check if player is a friend") {
                    isFriend(playerArg.value.name)
                }
            }
        }

        literal("list") {
            execute("List your friends") {
                listFriends()
            }
        }

        execute("List your friends") {
            listFriends()
        }
    }

    private fun isFriend(name: String) {
        val string = if (FriendManager.isFriend(name)) "Yes, $name is your friend."
        else "No, $name isn't a friend of yours."
        NoSpamMessage.sendMessage(string)
    }

    private fun listFriends() {
        if (FriendManager.empty) {
            NoSpamMessage.sendMessage("You currently don't have any friends added. run §7${prefix}friend add <name>§r to add one.")
        } else {
            val f = FriendManager.friends.values.joinToString(
                prefix = "\n    ",
                separator = "\n    "
            ) { it.name } // nicely format the chat output
            NoSpamMessage.sendMessage("Your friends: $f")
        }
    }
}