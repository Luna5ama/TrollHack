package cum.xiaro.trollhack.module.modules.combat

import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.managers.FriendManager
import cum.xiaro.trollhack.manager.managers.WaypointManager
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.EntityUtils.flooredPosition
import cum.xiaro.trollhack.util.EntityUtils.isFakeOrSelf
import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.TimeUnit
import cum.xiaro.trollhack.util.atTrue
import cum.xiaro.trollhack.util.text.MessageSendUtils
import cum.xiaro.trollhack.util.text.MessageSendUtils.sendServerMessage
import cum.xiaro.trollhack.util.text.format
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.SoundEvents
import net.minecraft.util.text.TextFormatting

internal object VisualRange : Module(
    name = "VisualRange",
    description = "Shows players who enter and leave range in chat",
    category = Category.COMBAT,
    alwaysListening = true
) {
    private const val NAME_FORMAT = "\$NAME"

    private val playSound by setting("Play Sound", false)
    private val leaving0 = setting("Count Leaving", false)
    private val leaving by leaving0
    private val friends by setting("Friends", true)
    private val uwuAura by setting("UwU Aura", false)
    private val logToFile by setting("Log To File", false)
    private val enterMessage by setting("Enter Message", "$NAME_FORMAT spotted!")
    private val leaveMessage by setting("Leave Message", "$NAME_FORMAT left!", leaving0.atTrue())

    private val playerSet = LinkedHashSet<EntityPlayer>()
    private val timer = TickTimer(TimeUnit.SECONDS)

    init {
        safeListener<TickEvent.Post> {
            if (!timer.tickAndReset(1L)) return@safeListener

            val loadedPlayerSet = LinkedHashSet(world.playerEntities)
            for (entityPlayer in loadedPlayerSet) {
                if (entityPlayer.isFakeOrSelf) continue // Self / Freecam / FakePlayer check
                if (!friends && FriendManager.isFriend(entityPlayer.name)) continue // Friend check

                if (playerSet.add(entityPlayer) && isEnabled) {
                    onEnter(entityPlayer)
                }
            }

            val toRemove = ArrayList<EntityPlayer>()
            for (player in playerSet) {
                if (!loadedPlayerSet.contains(player)) {
                    toRemove.add(player)
                    if (isEnabled) onLeave(player)
                }
            }
            playerSet.removeAll(toRemove)
        }
    }

    private fun onEnter(player: EntityPlayer) {
        val message = enterMessage.replaceName(player)

        sendNotification(message)
        if (logToFile) WaypointManager.add(player.flooredPosition, message)
        if (uwuAura) sendServerMessage("/w ${player.name} hi uwu")
    }

    private fun onLeave(player: EntityPlayer) {
        if (!leaving) return
        val message = leaveMessage.replaceName(player)

        sendNotification(message)
        if (logToFile) WaypointManager.add(player.flooredPosition, message)
        if (uwuAura) sendServerMessage("/w ${player.name} bye uwu")
    }

    private fun String.replaceName(player: EntityPlayer) = replace(NAME_FORMAT, getColor(player) format player.name)

    private fun getColor(player: EntityPlayer) =
        if (FriendManager.isFriend(player.name)) TextFormatting.GREEN
        else TextFormatting.RED

    private fun sendNotification(message: String) {
        if (playSound) mc.soundHandler.playSound(PositionedSoundRecord.getRecord(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f))
        MessageSendUtils.sendNoSpamChatMessage(message)
    }
}