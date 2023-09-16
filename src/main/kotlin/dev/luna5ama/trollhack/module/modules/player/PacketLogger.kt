package dev.luna5ama.trollhack.module.modules.player

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.event.ICancellable
import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.setting.settings.impl.primitive.BooleanSetting
import dev.luna5ama.trollhack.util.accessor.*
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import dev.luna5ama.trollhack.util.text.MessageSendUtils
import dev.luna5ama.trollhack.util.threads.DefaultScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.network.Packet
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.*
import java.io.File
import java.io.FileWriter
import java.time.LocalTime
import java.time.format.DateTimeFormatter

internal object PacketLogger : Module(
    name = "Packet Logger",
    description = "Logs sent packets to a file",
    category = Category.PLAYER
) {
    private val page by setting("Page", Page.GENERAL)
    private val showClientTicks by setting(
        "Show Client Ticks",
        false,
        { page == Page.GENERAL },
        description = "Show timestamps of client ticks."
    )

    private enum class Page(override val displayName: CharSequence) : DisplayEnum {
        GENERAL("General"),
        CLIENT("Client"),
        SERVER("Server");

        override fun toString(): String {
            return displayString
        }
    }

    private enum class LogStage(
        override val displayName: CharSequence,
        val predicate: (PacketEvent.Stage) -> Boolean
    ) : DisplayEnum {
        PRE("Pre", { it == PacketEvent.Stage.PRE }),
        POST("Post", { it == PacketEvent.Stage.POST }),
        BOTH("Both", { true })
    }

    private class PacketLogBuilder<T : Packet<*>>(
        event: PacketEvent,
        val packet: T
    ) {
        private val stringBuilder = StringBuilder()

        init {
            stringBuilder.apply {
                append(event.side)
                append(',')

                append(event.stage)
                append(',')

                append(packet.javaClass.simpleName)
                append(',')

                append(formattedTime(System.nanoTime() - start))
                append(',')

                append(formattedTime(System.nanoTime() - last))
                append(',')
            }
        }

        operator fun String.unaryPlus() {
            stringBuilder.append(this)
        }

        infix fun String.to(value: Any?) {
            if (value != null) {
                add(this, value.toString())
            }
        }

        infix fun String.to(value: String?) {
            if (value != null) {
                add(this, value)
            }
        }

        inline infix fun String.to(block: StringBuilder.() -> Unit) {
            add(this, buildString(block))
        }

        fun add(key: String, value: String) {
            stringBuilder.apply {
                append(key)
                append(": ")
                append(value)
                append(' ')
            }
        }

        fun build(logInChat: Boolean) {
            val string = stringBuilder.run {
                append('\n')
                toString()
            }

            synchronized(PacketLogger) {
                lines.add(string)
                last = System.nanoTime()
            }

            if (logInChat) {
                MessageSendUtils.sendChatMessage(string)
            }
        }
    }

    private class SideSetting private constructor(
        side: Page,
        handleFunc: List<Pair<Class<out Packet<*>>, PacketLogBuilder<*>.() -> Unit>>
    ) {
        val sideEnabled by setting("$side Enabled", false, { page == side })
        val sideStage by setting("$side Stage", LogStage.PRE, { page == side && sideEnabled })
        val logInChat by setting("$side Log In Chat", false, { page == side && sideEnabled })
        val logAll by setting("$side Log All", false, { page == side && sideEnabled })
        val logCancelled by setting("$side Log Cancelled", false, { page == side && sideEnabled && !logAll })

        private val unknownHandler =
            Handler(setting("$side Log Unknown", false, { page == side && sideEnabled && !logAll })) { +"Unknown" }

        private val handlers = mutableMapOf<Class<out Packet<*>>, Handler>().apply {
            handleFunc.associateTo(this) { (clazz, func) ->
                clazz to Handler(setting(clazz.simpleName, false, { page == side && sideEnabled && !logAll }), func)
            }
        }

        private fun getHandler(event: PacketEvent): Handler {
            return handlers.getOrPut(event.packet.javaClass) {
                handlers.entries.find { (clazz, _) ->
                    clazz.isInstance(event.packet)
                }?.value ?: unknownHandler
            }
        }

        fun handle(event: PacketEvent) {
            if (!sideEnabled) return
            if (!sideStage.predicate(event.stage)) return
            if (!logCancelled && event is ICancellable && event.cancelled) return

            getHandler(event).handle(event)
        }

        private inner class Handler(
            val setting: BooleanSetting,
            val handleFunc: PacketLogBuilder<out Packet<*>>.() -> Unit
        ) {
            fun handle(event: PacketEvent) {
                if (!logAll && !setting.value) return
                PacketLogBuilder(event, event.packet).apply(handleFunc).build(logInChat)
            }
        }

        class Builder(private val side: Page) {
            private val handlers = HashMap<Class<out Packet<*>>, PacketLogBuilder<*>.() -> Unit>()

            inline fun <reified T : Packet<*>> handle(noinline block: PacketLogBuilder<T>.() -> Unit) {
                @Suppress("UNCHECKED_CAST")
                handlers[T::class.java] = block as PacketLogBuilder<*>.() -> Unit
            }

            fun build(): SideSetting {
                return SideSetting(side, handlers.toList().sortedBy { it.first.simpleName })
            }
        }
    }

    private val clientSide = SideSetting.Builder(Page.CLIENT).apply { registerClient() }.build()
    private val serverSide = SideSetting.Builder(Page.SERVER).apply { registerServer() }.build()

    private val fileTimeFormatter = DateTimeFormatter.ofPattern("HH-mm-ss_SSS")

    private var start = 0L
    private var last = 0L
    private var lastTick = 0L
    private val timer = TickTimer(TimeUnit.SECONDS)

    private const val directory = "${TrollHackMod.DIRECTORY}/packetLogs"
    private var filename = ""
    private var lines = ArrayList<String>()

    init {
        onEnable {
            start = System.nanoTime()
            last = start
            lastTick = start

            filename = "${fileTimeFormatter.format(LocalTime.now())}.csv"

            synchronized(PacketLogger) {
                lines.add("From,Stage,Packet Name,Time Since Start (ms),Time Since Last (ms),Data\n")
            }
        }

        onDisable {
            write()
        }

        safeParallelListener<TickEvent.Pre> {
            if (showClientTicks) {
                synchronized(PacketLogger) {
                    val current = System.nanoTime()
                    lines.add("Tick Pulse,,${formattedTime(current - start)},${formattedTime(current - lastTick)}\n")
                    lastTick = current
                }
            }

            /* Don't let lines get too big, write periodically to the file */
            if (lines.size >= 500 || timer.tickAndReset(15L)) {
                write()
            }
        }

        safeListener<ConnectionEvent.Disconnect> {
            disable()
        }

        listener<PacketEvent.Send>(Int.MIN_VALUE) {
            clientSide.handle(it)
        }

        listener<PacketEvent.PostSend>(Int.MIN_VALUE) {
            clientSide.handle(it)
        }

        listener<PacketEvent.Receive>(Int.MIN_VALUE) {
            serverSide.handle(it)
        }

        listener<PacketEvent.PostReceive>(Int.MIN_VALUE) {
            serverSide.handle(it)
        }
    }

    private fun write() {
        val lines = synchronized(this) {
            val cache = lines
            lines = ArrayList()
            cache
        }

        DefaultScope.launch(Dispatchers.IO) {
            try {
                with(File(directory)) {
                    if (!exists()) mkdir()
                }

                FileWriter("$directory/${filename}", true).buffered().use {
                    for (line in lines) it.write(line)
                }
            } catch (e: Exception) {
                TrollHackMod.logger.warn("$chatName Failed saving packet log!", e)
            }
        }
    }

    private fun formattedTime(nano: Long): String {
        return String.format("%.2f", nano / 1_000_000.0)
    }

    private fun SideSetting.Builder.registerClient() {
        handle<CPacketAnimation> {
            "hand" to packet.hand
        }
        handle<CPacketChatMessage> {
            "message" to packet.message
        }
        handle<CPacketClickWindow> {
            "windowId" to packet.windowId
            "slotID" to packet.slotId
            "mouseButton" to packet.usedButton
            "clickType" to packet.clickType
            "transactionID" to packet.actionNumber
            "clickedItem" to packet.clickedItem
        }
        handle<CPacketConfirmTeleport> {
            "teleportID" to packet.teleportId
        }
        handle<CPacketEntityAction> {
            "action" to packet.action.name
            "auxData" to packet.auxData
        }
        handle<CPacketHeldItemChange> {
            "slotID" to packet.slotId
        }
        handle<CPacketKeepAlive> {
            "ket" to packet.key
        }
        handle<CPacketPlayer> {
            if (packet.moving) {
                "x" to packet.x
                "y" to packet.y
                "z" to packet.z
            }
            if (packet.rotating) {
                "yaw" to packet.yaw
                "pitch" to packet.pitch
            }
            "onGround" to packet.isOnGround
        }
        handle<CPacketPlayerDigging> {
            "x" to packet.position.x
            "y" to packet.position.y
            "z" to packet.position.z
            "facing" to packet.facing
            "action" to packet.action
        }
        handle<CPacketPlayerTryUseItem> {
            "hand" to packet.hand
        }
        handle<CPacketPlayerTryUseItemOnBlock> {
            "x" to packet.pos.x
            "y" to packet.pos.y
            "z" to packet.pos.z
            "side" to packet.direction
            "hitVecX" to packet.facingX
            "hitVecY" to packet.facingY
            "hitVecZ" to packet.facingZ
        }
        @Suppress("UNNECESSARY_SAFE_CALL")
        handle<CPacketUseEntity> {
            "action" to packet.action
            "action" to packet.hand
            "hitVecX" to packet.hitVec?.x
            "hitVecX" to packet.hitVec?.y
            "hitVecX" to packet.hitVec?.z
        }
        handle<CPacketConfirmTransaction> {
            "windowID" to packet.windowId
            "uid" to packet.uid
        }
    }

    private fun SideSetting.Builder.registerServer() {
        handle<SPacketBlockChange> {
            "x" to packet.blockPosition.x
            "y" to packet.blockPosition.y
            "z" to packet.blockPosition.z
            "block" to packet.blockState.block.toString()
        }
        handle<SPacketChat> {
            "unformattedText" to packet.chatComponent.unformattedText
            "type" to packet.type
            "itSystem" to packet.isSystem
        }
        handle<SPacketChunkData> {
            "chunkX" to packet.chunkX
            "chunkZ" to packet.chunkZ
            "extractedSize" to packet.extractedSize
        }
        handle<SPacketConfirmTransaction> {
            "window" to packet.windowId
            "id" to packet.actionNumber
            "accepted" to packet.wasAccepted()
        }
        handle<SPacketDestroyEntities> {
            "entityIDs" to {
                for (entry in packet.entityIDs) {
                    append("> ")
                    append(entry)
                    append(' ')
                }
            }
        }
        handle<SPacketEntityMetadata> {
            "dataEntries" to {
                val dataManagerEntries = packet.dataManagerEntries
                @Suppress("SENSELESS_COMPARISON")
                if (dataManagerEntries == null) {
                    append("null")
                } else {
                    for (entry in dataManagerEntries) {
                        append("> isDirty: ")
                        append(entry.isDirty)

                        append(" key: ")
                        append(entry.key)

                        append(" value: ")
                        append(entry.value)

                        append(' ')
                    }
                }
            }
        }
        handle<SPacketEntityProperties> {
            "entityID" to packet.entityId
        }
        handle<SPacketEntityStatus> {
            "entityID" to packet.entityID
            "opCode" to packet.opCode
        }
        handle<SPacketEntityTeleport> {
            "x" to packet.x
            "y" to packet.y
            "z" to packet.z
            "yaw" to packet.yaw
            "pitch" to packet.pitch
            "entityID" to packet.entityId
        }
        handle<SPacketKeepAlive> {
            "id" to packet.id
        }
        handle<SPacketMultiBlockChange> {
            "changedBlocks" to {
                for (changedBlock in packet.changedBlocks) {
                    append("> x: ")
                    append(changedBlock.pos.x)

                    append("y: ")
                    append(changedBlock.pos.y)

                    append("z: ")
                    append(changedBlock.pos.z)

                    append(' ')
                }
            }
        }
        handle<SPacketPlayerPosLook> {
            "x" to packet.x
            "y" to packet.y
            "z" to packet.z
            "yaw" to packet.yaw
            "pitch" to packet.pitch
            "teleportID" to packet.teleportId
            "flags" to {
                for (entry in packet.flags) {
                    append("> ")
                    append(entry.name)
                    append(' ')
                }
            }
        }
        handle<SPacketSoundEffect> {
            "sound" to packet.sound.soundName
            "category" to packet.category
            "posX" to packet.x
            "posY" to packet.y
            "posZ" to packet.z
            "volume" to packet.volume
            "pitch" to packet.pitch
        }
        handle<SPacketSpawnObject> {
            "entityID" to packet.entityID
            "data" to packet.data
        }
        handle<SPacketTeams> {
            "action" to packet.action
            "type" to packet.displayName
            "itSystem" to packet.color
        }
        handle<SPacketTimeUpdate> {
            "totalWorldTime" to packet.totalWorldTime
            "worldTime" to packet.worldTime
        }
        handle<SPacketUnloadChunk> {
            "x" to packet.x
            "z" to packet.z
        }
        handle<SPacketUpdateHealth> {
            "foodLevel" to packet.foodLevel
            "health" to packet.health
        }
        handle<SPacketUpdateTileEntity> {
            "x" to packet.pos.x
            "y" to packet.pos.y
            "z" to packet.pos.z
        }
        handle<SPacketHeldItemChange> {
            "hotbar" to packet.heldItemHotbarIndex
        }
        handle<SPacketSetSlot> {
            "windowID" to packet.windowId
            "slot" to packet.slot
            "item" to packet.stack
        }
    }
}