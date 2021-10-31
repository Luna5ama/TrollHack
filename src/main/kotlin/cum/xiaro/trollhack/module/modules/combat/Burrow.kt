package cum.xiaro.trollhack.module.modules.combat

import cum.xiaro.trollhack.util.extension.sq
import cum.xiaro.trollhack.util.interfaces.DisplayEnum
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.events.player.PlayerMoveEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.managers.CombatManager
import cum.xiaro.trollhack.manager.managers.EntityManager
import cum.xiaro.trollhack.manager.managers.HotbarManager.spoofHotbar
import cum.xiaro.trollhack.manager.managers.PlayerPacketManager
import cum.xiaro.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import cum.xiaro.trollhack.manager.managers.TimerManager.modifyTimer
import cum.xiaro.trollhack.manager.managers.TimerManager.resetTimer
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.module.modules.movement.AutoCenter
import cum.xiaro.trollhack.util.EntityUtils.isFakeOrSelf
import cum.xiaro.trollhack.util.EntityUtils.spoofSneak
import cum.xiaro.trollhack.util.atValue
import cum.xiaro.trollhack.util.inventory.slot.HotbarSlot
import cum.xiaro.trollhack.util.inventory.slot.firstBlock
import cum.xiaro.trollhack.util.inventory.slot.hotbarSlots
import cum.xiaro.trollhack.util.math.vector.Vec2f
import cum.xiaro.trollhack.util.notAtValue
import cum.xiaro.trollhack.util.text.MessageSendUtils
import cum.xiaro.trollhack.util.threads.runSafe
import cum.xiaro.trollhack.util.world.getGroundPos
import cum.xiaro.trollhack.util.world.isReplaceable
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.SPacketExplosion
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.abs

internal object Burrow : Module(
    name = "Burrow",
    description = "Clips yourself into a block",
    category = Category.COMBAT,
    modulePriority = 250
) {
    private val mode0 = setting("Mode", Mode.INSTANT)
    private val mode by mode0
    private val timer by setting("Timer", 4.0f, 1.0f..8.0f, 0.5f, mode0.atValue(Mode.JUMP))
    private val breakCrystal by setting("Break Crystal", true)
    private val autoCenter by setting("Auto Center", true)
    private val rubberY by setting("Rubber Y", 20.0f, -20.0f..20.0f, 0.5f, mode0.notAtValue(Mode.INSTANT))
    private val timeoutTicks by setting("Timeout Ticks", 10, 0..100, 5)

    private enum class Mode(override val displayName: CharSequence) : DisplayEnum {
        INSTANT("Instant"),
        JUMP("Jump")
    }

    var override: BlockPos? = null
    private var postBlockPos: BlockPos? = null
    private var timeout: Long = 0L

    private var blockPos: BlockPos? = null
    private var position: Vec3d? = null
    private var rotation: Vec2f? = null
    private var cancelMotion = false
    private var enabledTicks = 0

    private var velocityTime = 0L

    init {
        onDisable {
            resetTimer()

            override = null
            blockPos = null
            position = null
            rotation = null
            enabledTicks = 0

            velocityTime = 0L
        }

        onEnable {
            runSafe {
                runTick()
            } ?: disable()
        }

        safeListener<PlayerMoveEvent.Pre>(-3000, true) {
            if (isEnabled) {
                runTick()
            } else if (cancelMotion) {
                it.x = 0.0
                it.z = 0.0
                player.motionX = 0.0
                player.motionZ = 0.0
                cancelMotion = false
            }
        }

        safeListener<PacketEvent.Receive>(true) {
            when (it.packet) {
                is SPacketPlayerPosLook -> {
                    if (autoCenter && System.currentTimeMillis() < timeout) {
                        val pos = postBlockPos

                        if (pos != null && it.packet.y.toInt() == pos.y) {
                            AutoCenter.centerPlayer(pos)
                        }

                        postBlockPos = null
                        timeout = 0L
                    }
                }
                is SPacketExplosion -> {
                    if (it.packet.y > 2.0 && velocityTime <= System.currentTimeMillis()) {
                        velocityTime = System.currentTimeMillis() + 3000L
                    }
                }
            }
        }
    }

    private fun SafeClientEvent.runTick() {
        if (!tryRunTick() && enabledTicks++ >= timeoutTicks) {
            disable()
        }
    }

    private fun SafeClientEvent.tryRunTick(): Boolean {
        if (!player.onGround || (player.posY - player.prevPosY).sq > 0.01) {
            return false
        }

        val blockPos = override ?: world.getGroundPos(player).up()

        if (!canPlace(blockPos)) {
            return false
        }

        postBlockPos = null
        timeout = 0L

        val position = Vec3d(player.posX, player.posY, player.posZ)
        val rotation = PlayerPacketManager.rotation

        Burrow.blockPos = blockPos
        Burrow.position = position
        Burrow.rotation = rotation

        when (mode) {
            Mode.INSTANT -> instantMode(blockPos, position, rotation)
            Mode.JUMP -> jumpMode(blockPos, position, rotation)
        }

        player.motionX = 0.0
        player.motionZ = 0.0

        cancelMotion = true
        enabledTicks = 0
        return true
    }

    private fun SafeClientEvent.canPlace(pos: BlockPos): Boolean {
        return abs(player.posX - (pos.x + 0.5)) < 0.79
            && abs(player.posZ - (pos.z + 0.5)) < 0.79
            && world.getBlockState(pos).isReplaceable
            && !world.getBlockState(pos.down()).isReplaceable
            && pos.up(2).let { world.getBlockState(it).getCollisionBoundingBox(world, it) == null }
            && AxisAlignedBB(pos).let { box ->
            EntityManager.entity.none { entity ->
                entity.isEntityAlive
                    && (!breakCrystal || entity !is EntityEnderCrystal)
                    && (entity !is EntityPlayer || !entity.isFakeOrSelf)
                    && entity.collisionBoundingBox?.intersects(box) ?: false
            }
        }
    }

    private fun SafeClientEvent.instantMode(blockPos: BlockPos, position: Vec3d, rotation: Vec2f) {
        getSlot()?.let {
            if (breakCrystal) breakCrystal(blockPos)
            cancelPacket()

            connection.sendPacket(CPacketPlayer.PositionRotation(position.x, position.y + 0.41999808688698, position.z, rotation.x, 90.0f, false))
            connection.sendPacket(CPacketPlayer.Position(position.x, position.y + 0.7500019, position.z, false))
            connection.sendPacket(CPacketPlayer.Position(position.x, position.y + 0.9999962, position.z, false))
            connection.sendPacket(CPacketPlayer.Position(position.x, position.y + 1.17000380178814, position.z, false))
            connection.sendPacket(CPacketPlayer.Position(position.x, position.y + 1.17001330178815, position.z, false))

            placeBlock(it, blockPos)

            connection.sendPacket(CPacketPlayer.Position(position.x, position.y + 1.2426308013947485, position.z, false))
            if (velocityTime > System.currentTimeMillis()) {
                connection.sendPacket(CPacketPlayer.Position(position.x, position.y + 3.3400880035762786, position.z, false))
                connection.sendPacket(CPacketPlayer.Position(position.x, position.y - 1.0, position.z, false))
            } else {
                connection.sendPacket(CPacketPlayer.Position(position.x, position.y + 2.3400880035762786, position.z, false))
            }
        }

        disable()
    }

    private fun SafeClientEvent.jumpMode(blockPos: BlockPos, position: Vec3d, rotation: Vec2f) {
        val slot = getSlot() ?: run {
            disable()
            return
        }

        if (player.onGround) {
            player.jump()
        } else if (player.posY - position.y > 1.0) {
            if (breakCrystal) breakCrystal(blockPos)
            cancelPacket()

            placeBlock(slot, blockPos)

            connection.sendPacket(CPacketPlayer.PositionRotation(position.x, position.y + rubberY, position.z, rotation.x, rotation.y, false))
            connection.sendPacket(CPacketPlayer.PositionRotation(position.x, position.y, position.z, rotation.x, rotation.y, false))

            disable()
            return
        }

        modifyTimer(50.0f / timer)
        sendPlayerPacket {
            rotate(Vec2f(PlayerPacketManager.rotation.x, 90.0f))
        }
    }

    private fun SafeClientEvent.getSlot(): HotbarSlot? {
        val slot = player.hotbarSlots.firstBlock(Blocks.OBSIDIAN)

        return if (slot == null) {
            MessageSendUtils.sendNoSpamChatMessage("$chatName No obsidian in hotbar!")
            null
        } else {
            slot
        }
    }

    private fun cancelPacket() {
        sendPlayerPacket {
            cancelAll()
        }
    }

    private fun SafeClientEvent.breakCrystal(blockPos: BlockPos) {
        val box = AxisAlignedBB(blockPos)
        for ((crystal, _) in CombatManager.crystalList) {
            if (crystal.entityBoundingBox.intersects(box)) {
                connection.sendPacket(CPacketUseEntity(crystal))
                connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
                break
            }
        }
    }

    private fun SafeClientEvent.placeBlock(slot: HotbarSlot, blockPos: BlockPos) {
        val target = blockPos.down()

        val packet = CPacketPlayerTryUseItemOnBlock(target, EnumFacing.UP, EnumHand.MAIN_HAND, 0.5f, 1.0f, 0.5f)

        player.spoofSneak {
            spoofHotbar(slot) {
                connection.sendPacket(packet)
            }
        }

        connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))

        timeout = System.currentTimeMillis() + 100L
        postBlockPos = blockPos
    }
}