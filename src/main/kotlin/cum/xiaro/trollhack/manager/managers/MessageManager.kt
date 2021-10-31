package cum.xiaro.trollhack.manager.managers

import cum.xiaro.trollhack.util.extension.synchronized
import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.events.RunGameLoopEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeConcurrentListener
import cum.xiaro.trollhack.manager.Manager
import cum.xiaro.trollhack.module.AbstractModule
import cum.xiaro.trollhack.module.modules.client.ChatSetting
import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.accessor.packetMessage
import net.minecraft.network.play.client.CPacketChatMessage
import java.util.*

object MessageManager : Manager() {
    private val messageQueue = TreeSet<QueuedMessage>(Comparator.reverseOrder())
    private val packetSet = HashSet<CPacketChatMessage>()
    private val timer = TickTimer()
    var lastPlayerMessage = ""
    private var currentId = 0

    private val activeModifiers = TreeSet<MessageModifier>(Comparator.reverseOrder()).synchronized()
    private var modifierId = 0

    init {
        listener<PacketEvent.Send>(0, true) {
            if (it.packet !is CPacketChatMessage || packetSet.remove(it.packet)) return@listener
            it.cancel()

            if (it.packet.message != lastPlayerMessage) addMessageToQueue(it.packet, it)
            else addMessageToQueue(it.packet, mc.player ?: it, Int.MAX_VALUE - 1)
        }

        safeConcurrentListener<RunGameLoopEvent.Tick>(true) {
            synchronized(MessageManager) {
                if (messageQueue.isEmpty()) {
                    // Reset the current id so we don't reach the max 32 bit integer limit (although that is not likely to happen)
                    currentId = 0
                } else {
                    if (timer.tickAndReset(ChatSetting.delay)) {
                        messageQueue.pollFirst()?.let {
                            synchronized(activeModifiers) {
                                for (modifier in activeModifiers) {
                                    modifier.apply(it)
                                }
                            }
                            if (it.packet.message.isNotBlank()) connection.sendPacket(it.packet)
                        }
                    }

                    // Removes the low priority messages if it exceed the limit
                    while (messageQueue.size > ChatSetting.maxMessageQueueSize) {
                        messageQueue.pollLast()
                    }
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
        val message = QueuedMessage(currentId++, priority, source, packet)
        messageQueue.add(message)
        packetSet.add(packet)
    }

    class QueuedMessage(
        private val id: Int,
        private val priority: Int,
        val source: Any,
        val packet: CPacketChatMessage,
    ) : Comparable<QueuedMessage> {
        override fun compareTo(other: QueuedMessage): Int {
            val result = priority.compareTo(other.priority)
            return if (result != 0) result
            else id.compareTo(other.id)
        }
    }

    fun AbstractModule.newMessageModifier(filter: (QueuedMessage) -> Boolean = { true }, modifier: (QueuedMessage) -> String) =
        MessageModifier(modifierId++, modulePriority, filter, modifier)

    class MessageModifier(
        private val id: Int,
        private val priority: Int,
        private val filter: (QueuedMessage) -> Boolean = { true },
        private val modifier: (QueuedMessage) -> String
    ) : Comparable<MessageModifier> {

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
            val result = priority.compareTo(other.priority)
            return if (result != 0) result
            else id.compareTo(other.id)
        }
    }
}