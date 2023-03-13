package me.luna.trollhack.module.modules.combat

import me.luna.trollhack.event.SafeClientEvent
import me.luna.trollhack.event.events.TickEvent
import me.luna.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import me.luna.trollhack.event.safeListener
import me.luna.trollhack.event.safeParallelListener
import me.luna.trollhack.manager.managers.HoleManager
import me.luna.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import me.luna.trollhack.module.Category
import me.luna.trollhack.module.Module
import me.luna.trollhack.util.TickTimer
import me.luna.trollhack.util.math.vector.Vec2f
import me.luna.trollhack.util.threads.runSafe
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.math.AxisAlignedBB
import kotlin.math.abs
import kotlin.math.round
import kotlin.random.Random
import kotlin.random.nextInt

internal object CornerClip : Module(
    name = "CornerClip",
    category = Category.COMBAT,
    description = "Partially clips into corner on 5b",
    modulePriority = 9999
) {
    private val yDown by setting("Y Down", 0.02, 0.0..1.0, 0.001)
    private val autoEnableInHole by setting("Auto Enable In Hole", false)
    private val enableSeconds by setting("Enable Seconds", 2.0f, 0.1f..5.0f, 0.1f, ::autoEnableInHole)
    private val retryTimeoutSeconds by setting("Retry Timeout Seconds", 0.2f, 0.1f..5.0f, 0.1f)
    private val timeoutSeconds by setting("Timeout Seconds", 1.0f, 0.1f..5.0f, 0.1f)

    private var clippedTicks = 0
    private val enableTimer = TickTimer()
    private val timeoutTimer = TickTimer()
    private val retryTimer = TickTimer()
    private var alternate = false

    init {
        onToggle {
            enableTimer.reset()
        }

        onEnable {
            runSafe {
                clippedTicks = 0
                alternate = false
                timeoutTimer.reset()
                retryTimer.reset(-6969420L)
            } ?: disable()
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (isClipped()) {
                clippedTicks++
            } else {
                clippedTicks = 0
            }

            if (clippedTicks > 5
                || abs(player.motionY) >= 0.4
                || timeoutTimer.tick((timeoutSeconds * 1000.0f).toLong())) {
                disable()
                return@safeListener
            }

            if (alternate) {
                sendPlayerPacket {
                    rotate(Vec2f(
                        player.rotationYaw + Random.nextInt(-180..180),
                        Random.nextInt(-90..90).toFloat()
                    ))
                }
            } else if (clippedTicks <= 1 && retryTimer.tickAndReset((retryTimeoutSeconds * 1000.0f).toLong())) {
                val posX = player.posX
                var posY = round(player.posY)
                val posZ = player.posZ
                val onGround = player.onGround

                connection.sendPacket(CPacketPlayer.Position(
                    posX,
                    posY,
                    posZ,
                    onGround
                ))

                val halfY = yDown / 4.0
                posY -= halfY

                player.setPosition(posX, posY, posZ)
                connection.sendPacket(CPacketPlayer.Position(
                    posX,
                    posY,
                    posZ,
                    onGround
                ))

                posY -= halfY * 3.0

                player.setPosition(posX, posY, posZ)
                connection.sendPacket(CPacketPlayer.Position(
                    posX,
                    posY,
                    posZ,
                    onGround
                ))

                sendPlayerPacket {
                    move(player.positionVector)
                    onGround(onGround)
                }
            }

            alternate = !alternate
        }

        safeParallelListener<TickEvent.Post>(true) {
            if (isEnabled) return@safeParallelListener

            if (!autoEnableInHole || isClipped() || !HoleManager.getHoleInfo(player).isHole) {
                enableTimer.reset()
                return@safeParallelListener
            }

            if (enableTimer.tickAndReset((enableSeconds * 1000.0f).toLong())) {
                enable()
            }
        }
    }

    fun SafeClientEvent.isClipped(): Boolean {
        if (!isInt(player.posX)) return false
        if (!isInt(player.posY)) return false
        if (!isInt(player.posZ)) return false

        val qWidth = player.width / 4.0
        val bb = AxisAlignedBB(
            player.posX - qWidth,
            player.posY + 0.1,
            player.posZ - qWidth,
            player.posX + qWidth,
            player.posY + 1.0,
            player.posZ + qWidth
        )
        return world.getCollisionBoxes(null, bb).size >= 2
    }

    private fun isInt(v: Double): Boolean {
        return abs(v - round(v)) < 0.001
    }
}