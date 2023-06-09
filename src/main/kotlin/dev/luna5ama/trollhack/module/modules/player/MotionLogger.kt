package dev.luna5ama.trollhack.module.modules.player

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.MovementUtils.realSpeed
import dev.luna5ama.trollhack.util.threads.DefaultScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.time.LocalTime
import java.time.format.DateTimeFormatter

internal object MotionLogger : Module(
    name = "Motion Logger",
    description = "Logs player motion to a file",
    category = Category.PLAYER
) {
    private val fileTimeFormatter = DateTimeFormatter.ofPattern("HH-mm-ss_SSS")
    private val timer = TickTimer(TimeUnit.SECONDS)

    private const val directory = "${TrollHackMod.DIRECTORY}/motionLogs"

    private var tick = 0
    private var filename = ""
    private var lines = ArrayList<String>()

    init {
        onEnable {
            filename = "${fileTimeFormatter.format(LocalTime.now())}.csv"

            synchronized(this) {
                lines.add("Tick,Pos X,Pos Y,Pos Z,Motion X,Motion Y,Motion Z,Speed\n")
                tick = 0
            }
        }

        onDisable {
            write()
        }

        safeParallelListener<TickEvent.Post> {
            val motionX = player.posX - player.lastTickPosX
            val motionY = player.posY - player.lastTickPosY
            val motionZ = player.posZ - player.lastTickPosZ
            val speed = player.realSpeed

            synchronized(MotionLogger) {
                lines.add("${tick++},${player.posX},${player.posY},${player.posZ},$motionX,$motionY,$motionZ,$speed\n")
            }

            if (lines.size >= 500 || timer.tickAndReset(15L)) {
                write()
            }
        }

        safeListener<ConnectionEvent.Disconnect> {
            disable()
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
                TrollHackMod.logger.warn("$chatName Failed saving motion log!", e)
            }
        }
    }
}