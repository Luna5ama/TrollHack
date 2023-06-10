package dev.luna5ama.trollhack.manager.managers

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeConcurrentListener
import dev.luna5ama.trollhack.manager.Manager
import dev.luna5ama.trollhack.module.AbstractModule
import dev.luna5ama.trollhack.module.modules.client.ChatSetting
import dev.luna5ama.trollhack.util.accessor.packetMessage
import dev.luna5ama.trollhack.util.extension.synchronized
import net.minecraft.network.play.client.CPacketChatMessage
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

object MessageManager : Manager() {
    private val messageQueue = TreeSet<QueuedMessage>().synchronized()
    private val packetSet = HashSet<CPacketChatMessage>().synchronized()
    private val timer = TickTimer()
    var lastPlayerMessage = ""

    private val activeModifiers = TreeSet<MessageModifier>().synchronized()

    init {
        listener<PacketEvent.Send>(0, true) {
            if (it.packet !is CPacketChatMessage || packetSet.remove(it.packet)) return@listener
            it.cancel()

            if (it.packet.message != lastPlayerMessage) addMessageToQueue(it.packet, it)
            else addMessageToQueue(it.packet, mc.player ?: it, Int.MAX_VALUE - 1)
        }

        safeConcurrentListener<TickEvent.Post>(true) {
            if (messageQueue.isEmpty()) {
                // Reset the current id so we don't reach the max 32 bit integer limit (although that is not likely to happen)
                QueuedMessage.idCounter.set(Int.MIN_VALUE)
            } else {
                if (timer.tick(ChatSetting.delay)) {
                    messageQueue.pollFirst()?.let {
                        synchronized(activeModifiers) {
                            for (modifier in activeModifiers) {
                                modifier.apply(it)
                            }
                        }
                        if (it.packet.message.isNotBlank()) {
                            connection.sendPacket(it.packet)
                            timer.reset()
                        }
                    }
                }

                // Removes the low priority messages if it exceed the limit
                while (messageQueue.size > ChatSetting.maxMessageQueueSize) {
                    messageQueue.pollLast()
                }
            }
        }
    }

    fun sendMessageDirect(message: String) {
        val packet = CPacketChatMessage(message)
        packetSet.add(packet)
        mc.connection?.sendPacket(packet)
    }

    fun addMessageToQueue(message: String, source: Any, priority: Int = 0) {
        addMessageToQueue(CPacketChatMessage(message), source, priority)
    }

    fun addMessageToQueue(packet: CPacketChatMessage, source: Any, priority: Int = 0) {
        val message = QueuedMessage(priority, source, packet)
        messageQueue.add(message)
        packetSet.add(packet)
    }

    class QueuedMessage(
        private val priority: Int,
        val source: Any,
        val packet: CPacketChatMessage,
    ) : Comparable<QueuedMessage> {
        private val id = idCounter.getAndIncrement()

        override fun compareTo(other: QueuedMessage): Int {
            val result = -priority.compareTo(other.priority)
            return if (result != 0) result
            else id.compareTo(other.id)
        }

        companion object {
            val idCounter = AtomicInteger(Int.MIN_VALUE)
        }
    }

    fun AbstractModule.newMessageModifier(
        filter: (QueuedMessage) -> Boolean = { true },
        modifier: (QueuedMessage) -> String
    ) =
        MessageModifier(modulePriority, filter, modifier)

    class MessageModifier(
        private val priority: Int,
        private val filter: (QueuedMessage) -> Boolean = { true },
        private val modifier: (QueuedMessage) -> String
    ) : Comparable<MessageModifier> {
        private val id = idCounter.getAndIncrement()

        /**
         * Adds this modifier to the active modifier set [activeModifiers]
         */
        fun enable() {
            activeModifiers.add(this)
        }

        /**
         * Adds this modifier to the active modifier set [activeModifiers]
         */
        fun disable() {
            activeModifiers.remove(this)
        }

        /**
         * Apple this modifier on [queuedMessage]
         *
         * @param queuedMessage Message to be applied on
         *
         * @return true if [queuedMessage] have been modified
         */
        fun apply(queuedMessage: QueuedMessage) {
            if (filter.invoke(queuedMessage)) {
                queuedMessage.packet.packetMessage = modifier.invoke(queuedMessage)
            }
        }

        override fun compareTo(other: MessageModifier): Int {
            val result = -priority.compareTo(other.priority)
            return if (result != 0) result
            else id.compareTo(other.id)
        }

        private companion object {
            val idCounter = AtomicInteger(Int.MIN_VALUE)
        }
    }
}